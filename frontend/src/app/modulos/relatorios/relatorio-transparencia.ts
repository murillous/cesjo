import { CurrencyPipe, DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';

import { mensagemDeErro } from '../../acesso/problema';
import { BotaoExportar } from '../../compartilhado/exportacao/botao-exportar';
import { dataIso } from './periodo';
import {
  anoAnterior,
  LinhaTransparencia,
  RelatorioTransparencia as Relatorio,
  TransparenciaApi,
  URL_EXPORTACAO_TRANSPARENCIA,
} from './transparencia-api';

/**
 * Relatório de transparência (RF016, issue #61, ADR 0210 e ADR 0212): a pessoa escolhe o período,
 * vê os números agregados por área e baixa em PDF ou Excel para publicar ou mandar aos financiadores.
 * Abre no ano anterior, que já fechou.
 */
@Component({
  selector: 'app-relatorio-transparencia',
  imports: [
    BotaoExportar,
    CurrencyPipe,
    DatePipe,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    ReactiveFormsModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './relatorio-transparencia.html',
  styleUrl: './relatorio-transparencia.scss',
})
export class RelatorioTransparencia implements OnInit {
  private readonly api = inject(TransparenciaApi);

  protected readonly hoje = new Date();
  protected readonly urlExportacao = URL_EXPORTACAO_TRANSPARENCIA;

  protected readonly periodo = new FormGroup({
    inicio: new FormControl<Date | null>(anoAnterior(this.hoje).inicio, Validators.required),
    fim: new FormControl<Date | null>(anoAnterior(this.hoje).fim, Validators.required),
  });

  protected readonly relatorio = signal<Relatorio | null>(null);
  /** Período do relatório na tela, mandado na exportação. */
  protected readonly periodoGerado = signal<{ inicio: string; fim: string } | null>(null);
  protected readonly erro = signal<string | null>(null);
  protected readonly carregando = signal(false);

  /** Linhas agrupadas por área, na ordem em que vêm do servidor. */
  protected readonly grupos = computed(() => {
    const grupos: { nome: string; linhas: LinhaTransparencia[] }[] = [];
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
        this.periodoGerado.set(escolhido);
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
