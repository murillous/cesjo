import { DatePipe, PercentPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';

import { mensagemDeErro } from '../../acesso/problema';
import { EstatisticasApi, PainelAtendimento as Painel } from './estatisticas-api';
import { GraficoBarras } from './grafico-barras';
import { dataIso, periodoPadrao } from './periodo';

/**
 * Dashboard de violações, atividades e desligamentos num período (RF017, issue #57, ADR 0198). Abre
 * com o ano corrente e busca de novo a cada período aplicado.
 */
@Component({
  selector: 'app-painel-atendimento',
  imports: [
    DatePipe,
    GraficoBarras,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    PercentPipe,
    ReactiveFormsModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './painel-atendimento.html',
  styleUrl: './painel-atendimento.scss',
})
export class PainelAtendimento implements OnInit {
  private readonly api = inject(EstatisticasApi);

  protected readonly periodo = new FormGroup({
    inicio: new FormControl<Date | null>(periodoPadrao().inicio, Validators.required),
    fim: new FormControl<Date | null>(periodoPadrao().fim, Validators.required),
  });

  protected readonly painel = signal<Painel | null>(null);
  protected readonly erro = signal<string | null>(null);
  protected readonly carregando = signal(false);

  ngOnInit(): void {
    this.aplicar();
  }

  protected aplicar(): void {
    const { inicio, fim } = this.periodo.getRawValue();
    if (this.periodo.invalid || !inicio || !fim) {
      this.periodo.markAllAsTouched();
      return;
    }
    this.carregando.set(true);
    this.erro.set(null);
    this.api.atendimento(dataIso(inicio), dataIso(fim)).subscribe({
      next: (painel) => {
        this.painel.set(painel);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar o dashboard.'));
        this.carregando.set(false);
      },
    });
  }
}
