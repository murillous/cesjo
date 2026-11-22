import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { Contagem } from './estatisticas-api';

/**
 * Gráfico de barras horizontais de uma série só, em HTML e CSS, sem biblioteca (ADR 0197). Cada
 * barra leva o valor e a porcentagem na ponta, então a cor nunca é o único jeito de ler. A tabela
 * com os mesmos números fica logo abaixo, recolhida, para leitor de tela e para quem prefere.
 */
@Component({
  selector: 'app-grafico-barras',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './grafico-barras.html',
  styleUrl: './grafico-barras.scss',
})
export class GraficoBarras {
  readonly titulo = input.required<string>();
  readonly contagens = input.required<readonly Contagem[]>();
  /** Nome do que é contado, no plural, para a tabela. */
  readonly unidade = input('Usuários');

  protected readonly soma = computed(() =>
    this.contagens().reduce((total, contagem) => total + contagem.total, 0),
  );

  private readonly maior = computed(() =>
    Math.max(0, ...this.contagens().map((contagem) => contagem.total)),
  );

  /** Tamanho da barra em relação à maior, de 0 a 1, para a maior ocupar a faixa toda. */
  protected fracao(total: number): number {
    const maior = this.maior();
    return maior === 0 ? 0 : total / maior;
  }

  protected porcentagem(total: number): string {
    const soma = this.soma();
    if (soma === 0) {
      return '0%';
    }
    return `${Math.round((total / soma) * 100)}%`;
  }
}
