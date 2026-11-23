import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { BotaoExportar } from '../../compartilhado/exportacao/botao-exportar';
import { ParametrosExportacao } from '../../compartilhado/exportacao/exportacao-api';
import { LinhaRelatorio, RelatorioEstatistico } from './estatisticas-api';

/**
 * Prévia de um relatório estatístico (ADR 0201 e ADR 0204): título, período, botão de download e uma
 * tabela por grupo, na ordem que vem do servidor. Serve ao mensal, ao quadrimestral e ao anual; quem
 * usa diz para onde vai a exportação e com quais parâmetros, os mesmos da prévia na tela.
 */
@Component({
  selector: 'app-quadro-relatorio',
  imports: [BotaoExportar, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './quadro-relatorio.html',
  styleUrl: './quadro-relatorio.scss',
})
export class QuadroRelatorio {
  readonly relatorio = input.required<RelatorioEstatistico>();
  /** Endpoint da exportação, para o `app-botao-exportar`. */
  readonly url = input.required<string>();
  readonly parametros = input.required<ParametrosExportacao>();

  /** Linhas agrupadas por área, na ordem em que vêm do servidor. */
  protected readonly grupos = computed(() => {
    const grupos: { nome: string; linhas: LinhaRelatorio[] }[] = [];
    for (const linha of this.relatorio().linhas) {
      const ultimo = grupos.at(-1);
      if (ultimo?.nome === linha.grupo) {
        ultimo.linhas.push(linha);
      } else {
        grupos.push({ nome: linha.grupo, linhas: [linha] });
      }
    }
    return grupos;
  });
}
