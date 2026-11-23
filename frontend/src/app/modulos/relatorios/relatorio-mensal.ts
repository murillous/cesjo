import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';

import { mensagemDeErro } from '../../acesso/problema';
import { EstatisticasApi, RelatorioEstatistico, URL_EXPORTACAO_MENSAL } from './estatisticas-api';
import { anosOferecidos, mesAnterior, MESES, mesIso } from './periodo';
import { QuadroRelatorio } from './quadro-relatorio';

/**
 * Relatório estatístico mensal (RF016, issue #58, ADR 0200): a pessoa escolhe o mês, vê os
 * quantitativos agrupados por área e baixa em PDF ou Excel. Abre no mês anterior, que já fechou.
 */
@Component({
  selector: 'app-relatorio-mensal',
  imports: [
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
    QuadroRelatorio,
    ReactiveFormsModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './relatorio-mensal.html',
  styleUrl: './escolha-relatorio.scss',
})
export class RelatorioMensal implements OnInit {
  private readonly api = inject(EstatisticasApi);
  private readonly hoje = new Date();

  protected readonly meses = MESES;
  protected readonly anos = anosOferecidos(this.hoje);
  protected readonly urlExportacao = URL_EXPORTACAO_MENSAL;

  protected readonly escolha = new FormGroup({
    mes: new FormControl(mesAnterior(this.hoje).mes, { nonNullable: true }),
    ano: new FormControl(mesAnterior(this.hoje).ano, { nonNullable: true }),
  });

  protected readonly relatorio = signal<RelatorioEstatistico | null>(null);
  /** Mês do relatório na tela, `AAAA-MM`, mandado na exportação. */
  protected readonly mesGerado = signal<string | null>(null);
  protected readonly erro = signal<string | null>(null);
  protected readonly carregando = signal(false);

  ngOnInit(): void {
    this.gerar();
  }

  /** Mês futuro não entra: o servidor recusaria. */
  protected mesFuturo(mes: number): boolean {
    return (
      this.escolha.controls.ano.value === this.hoje.getFullYear() && mes > this.hoje.getMonth() + 1
    );
  }

  protected gerar(): void {
    const { ano, mes } = this.escolha.getRawValue();
    const escolhido = mesIso(ano, mes);
    this.carregando.set(true);
    this.erro.set(null);
    this.api.mensal(escolhido).subscribe({
      next: (relatorio) => {
        this.relatorio.set(relatorio);
        this.mesGerado.set(escolhido);
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
