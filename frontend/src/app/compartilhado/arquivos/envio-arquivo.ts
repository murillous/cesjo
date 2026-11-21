import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  forwardRef,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Subscription } from 'rxjs';

import { ArquivoEnviado, ArquivosApi } from './arquivos-api';
import {
  ACEITE_INPUT,
  formatarTamanho,
  TAMANHO_MAXIMO_BYTES,
  validarArquivo,
} from './regras-arquivo';

type Estado = 'vazio' | 'validando' | 'enviando' | 'enviado' | 'anexado' | 'erro';

interface ArquivoEscolhido {
  nome: string;
  tamanho: number;
  tipo: string;
}

let proximoId = 0;

/**
 * Campo de envio de um arquivo (PDF, JPG ou PNG, até 10 MB), reutilizado pelos formulários que
 * anexam documentos, como a lista de espera (#27). Ao escolher, confere o arquivo no cliente e envia
 * na hora para `POST /api/arquivos` (ADR 0029).
 *
 * Funciona como campo de formulário: o valor é o `id` do arquivo guardado, ou `null`.
 *
 * ```html
 * <app-envio-arquivo rotulo="Laudo médico" formControlName="laudoId" />
 * ```
 */
@Component({
  selector: 'app-envio-arquivo',
  imports: [MatButtonModule, MatIconModule, MatProgressBarModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './envio-arquivo.html',
  styleUrl: './envio-arquivo.scss',
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => EnvioArquivo), multi: true },
  ],
})
export class EnvioArquivo implements ControlValueAccessor {
  /** Nome do campo, exibido acima do botão e usado nos nomes acessíveis. */
  readonly rotulo = input('Documento');

  /** Emitido quando o arquivo termina de ser guardado no backend. */
  readonly enviado = output<ArquivoEnviado>();

  /** Emitido quando o arquivo anexado é removido do campo. */
  readonly removido = output();

  private readonly api = inject(ArquivosApi);
  private readonly seletor = viewChild.required<ElementRef<HTMLInputElement>>('seletor');
  private envio?: Subscription;

  protected readonly ids = {
    rotulo: `envio-arquivo-${proximoId}-rotulo`,
    dica: `envio-arquivo-${proximoId}-dica`,
    erro: `envio-arquivo-${proximoId++}-erro`,
  };
  protected readonly aceite = ACEITE_INPUT;
  protected readonly dica = `PDF, JPG ou PNG, até ${formatarTamanho(TAMANHO_MAXIMO_BYTES)}.`;

  protected readonly estado = signal<Estado>('vazio');
  protected readonly arquivo = signal<ArquivoEscolhido | null>(null);
  protected readonly progresso = signal(0);
  protected readonly erro = signal<string | null>(null);
  protected readonly desabilitado = signal(false);
  protected readonly arrastando = signal(false);

  protected readonly ocupado = computed(
    () => this.estado() === 'validando' || this.estado() === 'enviando',
  );
  protected readonly tamanho = computed(() => {
    const arquivo = this.arquivo();
    return arquivo ? formatarTamanho(arquivo.tamanho) : '';
  });
  protected readonly icone = computed(() =>
    this.arquivo()?.tipo === 'application/pdf' ? 'picture_as_pdf' : 'image',
  );

  private aoMudar: (id: string | null) => void = () => undefined;
  private aoTocar: () => void = () => undefined;

  constructor() {
    inject(DestroyRef).onDestroy(() => this.envio?.unsubscribe());
  }

  protected escolher(): void {
    if (!this.desabilitado() && !this.ocupado()) {
      this.seletor().nativeElement.click();
    }
  }

  protected aoEscolher(evento: Event): void {
    const campo = evento.target as HTMLInputElement;
    const arquivo = campo.files?.item(0);
    // Limpa o input para que escolher o mesmo arquivo de novo dispare `change`.
    campo.value = '';
    if (arquivo) {
      void this.processar(arquivo);
    }
  }

  protected aoArrastar(evento: DragEvent): void {
    if (this.desabilitado() || this.ocupado()) {
      return;
    }
    evento.preventDefault();
    this.arrastando.set(true);
  }

  protected aoSairDoArraste(): void {
    this.arrastando.set(false);
  }

  protected aoSoltar(evento: DragEvent): void {
    evento.preventDefault();
    this.arrastando.set(false);
    const arquivo = evento.dataTransfer?.files.item(0);
    if (arquivo && !this.desabilitado() && !this.ocupado()) {
      void this.processar(arquivo);
    }
  }

  protected remover(): void {
    this.envio?.unsubscribe();
    const tinhaValor = this.estado() === 'enviado' || this.estado() === 'anexado';
    this.limpar();
    if (tinhaValor) {
      this.aoMudar(null);
      this.removido.emit();
    }
    this.aoTocar();
  }

  protected tocar(): void {
    this.aoTocar();
  }

  /** Confere e envia o arquivo. Público para testes e para quem recebe o arquivo de outra fonte. */
  async processar(arquivo: File): Promise<void> {
    this.envio?.unsubscribe();
    const tinhaValor = this.estado() === 'enviado' || this.estado() === 'anexado';
    this.arquivo.set({ nome: arquivo.name, tamanho: arquivo.size, tipo: arquivo.type });
    this.erro.set(null);
    this.progresso.set(0);
    this.estado.set('validando');
    if (tinhaValor) {
      this.aoMudar(null);
    }

    const problema = await validarArquivo(arquivo);
    if (problema) {
      this.falhar(problema);
      return;
    }

    this.estado.set('enviando');
    this.envio = this.api.enviar(arquivo).subscribe({
      next: (evento) => {
        if (evento.tipo === 'progresso') {
          this.progresso.set(evento.percentual);
          return;
        }
        this.arquivo.set({
          nome: evento.arquivo.nome,
          tamanho: evento.arquivo.tamanho,
          tipo: evento.arquivo.tipoMime,
        });
        this.progresso.set(100);
        this.estado.set('enviado');
        this.aoMudar(evento.arquivo.id);
        this.enviado.emit(evento.arquivo);
      },
      error: (erro: unknown) => this.falhar(mensagemDeErro(erro)),
    });
  }

  writeValue(id: string | null): void {
    this.envio?.unsubscribe();
    this.limpar();
    if (id) {
      // Só o id é conhecido: mostra que há arquivo anexado, sem nome nem tamanho.
      this.arquivo.set({ nome: 'Arquivo anexado', tamanho: 0, tipo: '' });
      this.estado.set('anexado');
    }
  }

  registerOnChange(fn: (id: string | null) => void): void {
    this.aoMudar = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.aoTocar = fn;
  }

  setDisabledState(desabilitado: boolean): void {
    this.desabilitado.set(desabilitado);
  }

  private falhar(mensagem: string): void {
    this.erro.set(mensagem);
    this.estado.set('erro');
  }

  private limpar(): void {
    this.arquivo.set(null);
    this.erro.set(null);
    this.progresso.set(0);
    this.estado.set('vazio');
  }
}

function mensagemDeErro(erro: unknown): string {
  if (erro instanceof HttpErrorResponse) {
    if (erro.status === 413) {
      return `O arquivo ultrapassa o limite de ${formatarTamanho(TAMANHO_MAXIMO_BYTES)}.`;
    }
    if (erro.status === 403) {
      return 'Sua conta não tem permissão para enviar arquivos.';
    }
    const detalhe = (erro.error as { detail?: unknown } | null)?.detail;
    if (erro.status >= 400 && erro.status < 500 && typeof detalhe === 'string' && detalhe) {
      return detalhe;
    }
  }
  return 'Não foi possível enviar o arquivo. Tente de novo.';
}
