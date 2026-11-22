import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';

import { mensagemDeErro } from '../../acesso/problema';
import { BotaoExportar } from '../../compartilhado/exportacao/botao-exportar';
import {
  EstatisticasApi,
  LinhaRelatorio,
  RelatorioEstatistico,
  URL_EXPORTACAO_MENSAL,
} from './estatisticas-api';
import { mesAnterior, MESES, mesIso } from './periodo';

/** Primeiro ano oferecido na escolha do mês. */
const PRIMEIRO_ANO = 2020;

/**
 * Relatório estatístico mensal (RF016, issue #58, ADR 0200): a pessoa escolhe o mês, vê os
 * quantitativos agrupados por área e baixa em PDF ou Excel. Abre no mês anterior, que já fechou.
 */
@Component({
  selector: 'app-relatorio-mensal',
  imports: [
    BotaoExportar,
    DatePipe,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
    ReactiveFormsModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './relatorio-mensal.html',
  styleUrl: './relatorio-mensal.scss',
})
export class RelatorioMensal implements OnInit {
  private readonly api = inject(EstatisticasApi);
  private readonly hoje = new Date();

  protected readonly meses = MESES;
  protected readonly anos = Array.from(
    { length: this.hoje.getFullYear() - PRIMEIRO_ANO + 1 },
    (_, i) => this.hoje.getFullYear() - i,
  );
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

  /** Linhas agrupadas por área, na ordem em que vêm do servidor. */
  protected readonly grupos = computed(() => {
    const grupos: { nome: string; linhas: LinhaRelatorio[] }[] = [];
    for (const linha of this.relatorio()?.linhas ?? []) {
      const ultimo = grupos.at(-1);
      if (ultimo?.nome === linha.grupo) {
        ultimo.linhas.push(linha);
      } else {
        grupos.push({ nome: linha.grupo, linhas: [linha] });
      }
    }
    return grupos;
  });

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
