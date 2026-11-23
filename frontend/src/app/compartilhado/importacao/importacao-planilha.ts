import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  inject,
  input,
  OnInit,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Subscription } from 'rxjs';

import { mensagemDeErro } from '../../acesso/problema';
import { formatarTamanho } from '../arquivos/regras-arquivo';
import {
  ColunaModelo,
  ImportacaoApi,
  Previa,
  ResultadoImportacao,
  TAMANHO_MAXIMO_PLANILHA,
} from './importacao-api';

type Estado = 'escolha' | 'lendo' | 'previa' | 'gravando' | 'concluida';

let proximoId = 0;

/**
 * Fluxo de importação de planilha Excel (ADR 0219), reutilizado pelas telas de importação: baixar o
 * modelo, enviar a planilha, conferir a pré-visualização com os erros por linha e confirmar. Só as
 * linhas válidas são gravadas.
 *
 * ```html
 * <app-importacao-planilha base="/api/importacao/lista-espera" item="candidato" itens="candidatos">
 *   <a matButton routerLink="/lista-espera">Ver a fila</a>
 * </app-importacao-planilha>
 * ```
 *
 * O conteúdo projetado aparece depois da importação concluída. Não checa permissão: a rota decide.
 */
@Component({
  selector: 'app-importacao-planilha',
  imports: [MatButtonModule, MatCheckboxModule, MatIconModule, MatProgressBarModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './importacao-planilha.html',
  styleUrl: './importacao-planilha.scss',
})
export class ImportacaoPlanilha implements OnInit {
  /** URL base da importação no backend, como `/api/importacao/lista-espera`. */
  readonly base = input.required<string>();

  /** Como chamar cada registro importado nas mensagens, no singular: "candidato". */
  readonly item = input('registro');

  /** O mesmo, no plural: "candidatos", "usuários atendidos". */
  readonly itens = input('registros');

  /** Emitido depois da gravação. */
  readonly importado = output<ResultadoImportacao>();

  private readonly api = inject(ImportacaoApi);
  private readonly seletor = viewChild.required<ElementRef<HTMLInputElement>>('seletor');
  private pedido?: Subscription;

  protected readonly ids = {
    dica: `importacao-${proximoId}-dica`,
    resumo: `importacao-${proximoId++}-resumo`,
  };
  protected readonly dica = `Arquivo .xlsx de até ${formatarTamanho(TAMANHO_MAXIMO_PLANILHA)}, com até 1000 linhas.`;

  protected readonly colunas = signal<ColunaModelo[]>([]);
  protected readonly estado = signal<Estado>('escolha');
  protected readonly arquivo = signal<File | null>(null);
  protected readonly previa = signal<Previa | null>(null);
  protected readonly resultado = signal<ResultadoImportacao | null>(null);
  protected readonly erro = signal('');
  protected readonly erroModelo = signal('');
  protected readonly baixandoModelo = signal(false);
  protected readonly soComErro = signal(false);

  protected readonly linhas = computed(() => {
    const previa = this.previa();
    if (!previa) {
      return [];
    }
    return this.soComErro() ? previa.linhas.filter((l) => l.erros.length > 0) : previa.linhas;
  });

  constructor() {
    inject(DestroyRef).onDestroy(() => this.pedido?.unsubscribe());
  }

  ngOnInit(): void {
    this.api.colunas(this.base()).subscribe({
      next: (colunas) => this.colunas.set(colunas),
      error: () => this.colunas.set([]),
    });
  }

  protected baixarModelo(): void {
    this.erroModelo.set('');
    this.baixandoModelo.set(true);
    this.api.baixarModelo(this.base()).subscribe({
      next: () => this.baixandoModelo.set(false),
      error: (erro: unknown) => {
        this.baixandoModelo.set(false);
        this.erroModelo.set(
          mensagemDeErro(erro, 'Não foi possível baixar o modelo. Tente de novo.'),
        );
      },
    });
  }

  protected escolher(): void {
    this.seletor().nativeElement.click();
  }

  protected aoEscolher(evento: Event): void {
    const campo = evento.target as HTMLInputElement;
    const arquivo = campo.files?.item(0);
    // Limpa o input para que escolher o mesmo arquivo de novo, já corrigido, dispare `change`.
    campo.value = '';
    if (arquivo) {
      this.ler(arquivo);
    }
  }

  /** Confere o arquivo no cliente e pede a pré-visualização. Público para os testes. */
  ler(arquivo: File): void {
    this.pedido?.unsubscribe();
    this.arquivo.set(arquivo);
    this.previa.set(null);
    this.resultado.set(null);
    this.soComErro.set(false);
    this.erro.set('');
    if (!arquivo.name.toLowerCase().endsWith('.xlsx')) {
      this.falhar('Escolha uma planilha Excel com extensão .xlsx.');
      return;
    }
    if (arquivo.size > TAMANHO_MAXIMO_PLANILHA) {
      this.falhar(
        `A planilha passa do limite de ${formatarTamanho(TAMANHO_MAXIMO_PLANILHA)}. Divida em arquivos menores.`,
      );
      return;
    }
    this.estado.set('lendo');
    this.pedido = this.api.previa(this.base(), arquivo).subscribe({
      next: (previa) => {
        this.previa.set(previa);
        this.estado.set('previa');
      },
      error: (erro: unknown) => this.falhar(mensagem(erro, 'Não foi possível ler a planilha.')),
    });
  }

  protected confirmar(): void {
    const arquivo = this.arquivo();
    if (!arquivo) {
      return;
    }
    this.erro.set('');
    this.estado.set('gravando');
    this.pedido = this.api.importar(this.base(), arquivo).subscribe({
      next: (resultado) => {
        this.resultado.set(resultado);
        this.estado.set('concluida');
        this.importado.emit(resultado);
      },
      error: (erro: unknown) => {
        this.estado.set('previa');
        this.erro.set(mensagem(erro, 'Não foi possível importar a planilha. Tente de novo.'));
      },
    });
  }

  protected recomecar(): void {
    this.pedido?.unsubscribe();
    this.arquivo.set(null);
    this.previa.set(null);
    this.resultado.set(null);
    this.erro.set('');
    this.estado.set('escolha');
  }

  protected alternarSoComErro(marcado: boolean): void {
    this.soComErro.set(marcado);
  }

  private falhar(texto: string): void {
    this.erro.set(texto);
    this.estado.set('escolha');
  }
}

function mensagem(erro: unknown, padrao: string): string {
  if (erro instanceof HttpErrorResponse) {
    if (erro.status === 413) {
      return `A planilha passa do limite de ${formatarTamanho(TAMANHO_MAXIMO_PLANILHA)}.`;
    }
    if (erro.status === 403) {
      return 'Sua conta não tem permissão para importar planilhas.';
    }
  }
  return mensagemDeErro(erro, padrao);
}
