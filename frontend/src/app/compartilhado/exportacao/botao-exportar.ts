import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subscription } from 'rxjs';

import { mensagemDeErro } from '../../acesso/problema';
import { ArquivoBaixado } from '../arquivos/arquivos-api';
import { ExportacaoApi, FormatoExportacao, ParametrosExportacao } from './exportacao-api';

let proximoId = 0;

/**
 * Botão "Exportar" das listagens e relatórios: abre a escolha entre PDF e planilha Excel, baixa o
 * arquivo do endpoint do módulo com os filtros da tela e entrega ao navegador (ADR 0044).
 *
 * ```html
 * <app-botao-exportar url="/api/lista-espera/exportacao" [parametros]="filtros()" />
 * ```
 *
 * Não checa permissão: quem usa decide se ele aparece, com `Sessao.temPermissao`.
 */
@Component({
  selector: 'app-botao-exportar',
  imports: [MatButtonModule, MatIconModule, MatMenuModule, MatProgressSpinnerModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './botao-exportar.html',
  styleUrl: './botao-exportar.scss',
})
export class BotaoExportar {
  /** Endpoint `GET` do módulo que gera a exportação. */
  readonly url = input.required<string>();

  /** Filtros aplicados na tela, enviados junto com o formato. */
  readonly parametros = input<ParametrosExportacao>({});

  /** Texto do botão. */
  readonly rotulo = input('Exportar');

  /** Emitido quando o arquivo é entregue ao navegador. */
  readonly exportado = output<ArquivoBaixado>();

  private readonly api = inject(ExportacaoApi);
  private download?: Subscription;

  protected readonly idErro = `botao-exportar-${proximoId++}-erro`;
  protected readonly gerando = signal<FormatoExportacao | null>(null);
  protected readonly erro = signal('');

  constructor() {
    inject(DestroyRef).onDestroy(() => this.download?.unsubscribe());
  }

  protected exportar(formato: FormatoExportacao): void {
    this.download?.unsubscribe();
    this.erro.set('');
    this.gerando.set(formato);
    this.download = this.api.baixarESalvar(this.url(), formato, this.parametros()).subscribe({
      next: (arquivo) => {
        this.gerando.set(null);
        this.exportado.emit(arquivo);
      },
      error: (erro: unknown) => {
        this.gerando.set(null);
        this.erro.set(mensagem(erro));
      },
    });
  }
}

function mensagem(erro: unknown): string {
  if (erro instanceof HttpErrorResponse && erro.status === 403) {
    return 'Sua conta não tem permissão para exportar estes dados.';
  }
  return mensagemDeErro(erro, 'Não foi possível gerar o arquivo. Tente de novo.');
}
