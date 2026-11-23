import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { Observable } from 'rxjs';

import { mensagemDeErro } from '../../acesso/problema';
import { ParametrosExportacao } from '../../compartilhado/exportacao/exportacao-api';
import {
  EstatisticasApi,
  RelatorioEstatistico,
  URL_EXPORTACAO_ANUAL,
  URL_EXPORTACAO_QUADRIMESTRAL,
} from './estatisticas-api';
import { anosOferecidos, quadrimestreAnterior, quadrimestreDoMes } from './periodo';
import { QuadroRelatorio } from './quadro-relatorio';

type Recorte = 'quadrimestral' | 'anual';

/** Os quadrimestres do ano civil. PENDENTE-CESJO(P9): divisão provisória do ano (ADR 0203). */
const QUADRIMESTRES: readonly { numero: number; rotulo: string }[] = [
  { numero: 1, rotulo: '1º quadrimestre (janeiro a abril)' },
  { numero: 2, rotulo: '2º quadrimestre (maio a agosto)' },
  { numero: 3, rotulo: '3º quadrimestre (setembro a dezembro)' },
];

/** O que está na tela, para a exportação baixar o mesmo relatório. */
interface Gerado {
  url: string;
  parametros: ParametrosExportacao;
}

/**
 * Relatórios estatísticos quadrimestral e anual (RF016, issue #59, ADR 0204): o mesmo conteúdo do
 * mensal, com a escolha do recorte. Abre no quadrimestre anterior, que já fechou.
 */
@Component({
  selector: 'app-relatorio-periodo',
  imports: [
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
    QuadroRelatorio,
    ReactiveFormsModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './relatorio-periodo.html',
  styleUrl: './escolha-relatorio.scss',
})
export class RelatorioPeriodo implements OnInit {
  private readonly api = inject(EstatisticasApi);
  private readonly hoje = new Date();

  protected readonly quadrimestres = QUADRIMESTRES;
  protected readonly anos = anosOferecidos(this.hoje);

  protected readonly escolha = new FormGroup({
    recorte: new FormControl<Recorte>('quadrimestral', { nonNullable: true }),
    quadrimestre: new FormControl(quadrimestreAnterior(this.hoje).quadrimestre, {
      nonNullable: true,
    }),
    ano: new FormControl(quadrimestreAnterior(this.hoje).ano, { nonNullable: true }),
  });

  protected readonly recorte = signal<Recorte>('quadrimestral');
  protected readonly relatorio = signal<RelatorioEstatistico | null>(null);
  protected readonly gerado = signal<Gerado | null>(null);
  protected readonly erro = signal<string | null>(null);
  protected readonly carregando = signal(false);

  constructor() {
    this.escolha.controls.recorte.valueChanges.subscribe((recorte) => this.recorte.set(recorte));
  }

  ngOnInit(): void {
    this.gerar();
  }

  /** Quadrimestre futuro não entra: o servidor recusaria. */
  protected quadrimestreFuturo(numero: number): boolean {
    return (
      this.escolha.controls.ano.value === this.hoje.getFullYear() &&
      numero > quadrimestreDoMes(this.hoje.getMonth() + 1)
    );
  }

  protected gerar(): void {
    const { recorte, quadrimestre, ano } = this.escolha.getRawValue();
    const pedido: Observable<RelatorioEstatistico> =
      recorte === 'anual' ? this.api.anual(ano) : this.api.quadrimestral(ano, quadrimestre);
    const gerado: Gerado =
      recorte === 'anual'
        ? { url: URL_EXPORTACAO_ANUAL, parametros: { ano } }
        : { url: URL_EXPORTACAO_QUADRIMESTRAL, parametros: { ano, quadrimestre } };
    this.carregando.set(true);
    this.erro.set(null);
    pedido.subscribe({
      next: (relatorio) => {
        this.relatorio.set(relatorio);
        this.gerado.set(gerado);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.relatorio.set(null);
        this.erro.set(mensagemDeErro(erro, 'Não foi possível gerar o relatório.'));
        this.carregando.set(false);
      },
    });
  }
}
