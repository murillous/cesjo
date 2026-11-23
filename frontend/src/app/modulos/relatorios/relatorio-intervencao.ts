import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';

import { mensagemDeErro } from '../../acesso/problema';
import { BotaoExportar } from '../../compartilhado/exportacao/botao-exportar';
import { LinhaRelatorio } from './estatisticas-api';
import { dataIso, mesAnterior } from './periodo';
import {
  RelatorioIntervencao as Relatorio,
  RelatorioIntervencaoApi,
  URL_EXPORTACAO_INTERVENCAO,
} from './relatorio-intervencao-api';

/**
 * Relatório de intervenção (CREAS) (RF016, issue #60, ADR 0205 e ADR 0207): casos de violação de
 * direitos do período, com os atendimentos e a situação de cada usuário, e um resumo. Vem com o mês
 * anterior preenchido, mas só gera quando a pessoa pede, porque cada geração é registrada na auditoria
 * como leitura de dado sensível.
 */
@Component({
  selector: 'app-relatorio-intervencao',
  imports: [
    BotaoExportar,
    DatePipe,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    ReactiveFormsModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './relatorio-intervencao.html',
  styleUrl: './relatorio-intervencao.scss',
})
export class RelatorioIntervencao {
  private readonly api = inject(RelatorioIntervencaoApi);
  private readonly hoje = new Date();

  protected readonly urlExportacao = URL_EXPORTACAO_INTERVENCAO;
  protected readonly maximo = new Date(
    this.hoje.getFullYear(),
    this.hoje.getMonth(),
    this.hoje.getDate(),
  );

  protected readonly periodo = new FormGroup({
    inicio: new FormControl<Date | null>(inicioDoMesAnterior(this.hoje), Validators.required),
    fim: new FormControl<Date | null>(fimDoMesAnterior(this.hoje), Validators.required),
  });

  protected readonly relatorio = signal<Relatorio | null>(null);
  /** Período do relatório na tela, mandado na exportação. */
  protected readonly gerado = signal<{ inicio: string; fim: string } | null>(null);
  protected readonly erro = signal<string | null>(null);
  protected readonly carregando = signal(false);

  /** Linhas do resumo agrupadas, na ordem em que vêm do servidor. */
  protected readonly grupos = computed(() => {
    const grupos: { nome: string; linhas: LinhaRelatorio[] }[] = [];
    for (const linha of this.relatorio()?.resumo ?? []) {
      const ultimo = grupos.at(-1);
      if (ultimo?.nome === linha.grupo) {
        ultimo.linhas.push(linha);
      } else {
        grupos.push({ nome: linha.grupo, linhas: [linha] });
      }
    }
    return grupos;
  });

  protected gerar(): void {
    const { inicio, fim } = this.periodo.getRawValue();
    if (this.periodo.invalid || !inicio || !fim) {
      this.periodo.markAllAsTouched();
      return;
    }
    const escolhido = { inicio: dataIso(inicio), fim: dataIso(fim) };
    this.carregando.set(true);
    this.erro.set(null);
    this.api.gerar(escolhido.inicio, escolhido.fim).subscribe({
      next: (relatorio) => {
        this.relatorio.set(relatorio);
        this.gerado.set(escolhido);
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

function inicioDoMesAnterior(hoje: Date): Date {
  const { ano, mes } = mesAnterior(hoje);
  return new Date(ano, mes - 1, 1);
}

function fimDoMesAnterior(hoje: Date): Date {
  const { ano, mes } = mesAnterior(hoje);
  return new Date(ano, mes, 0);
}
