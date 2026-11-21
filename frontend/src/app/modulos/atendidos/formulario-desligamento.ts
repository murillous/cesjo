import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import {
  FormControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';

import { mensagemDeErro } from '../../acesso/problema';
import { dataIso, hoje } from '../lista-espera/datas';
import { AtendidosApi } from './atendidos-api';
import { MOTIVOS_DESLIGAMENTO, MotivoDesligamento } from './rotulos';

/**
 * Registro do desligamento (`/atendidos/:id/desligar`, RF008, ADR 0110): data, motivo e observação,
 * que fica cifrada. Obrigatória quando o motivo é "Outros".
 */
@Component({
  selector: 'app-formulario-desligamento',
  imports: [
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatRadioModule,
    ReactiveFormsModule,
    RouterLink,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './formulario-desligamento.html',
  styleUrl: './formulario-desligamento.scss',
})
export class FormularioDesligamento {
  /** Parâmetro da rota: id da inserção. */
  readonly id = input.required<string>();

  private readonly api = inject(AtendidosApi);
  private readonly avisos = inject(MatSnackBar);
  private readonly router = inject(Router);

  protected readonly motivos = MOTIVOS_DESLIGAMENTO;
  protected readonly hoje = hoje();
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    data: new FormControl<Date | null>(hoje(), Validators.required),
    motivo: ['' as MotivoDesligamento | '', Validators.required],
    observacao: ['', Validators.maxLength(2000)],
  });

  protected faltaObservacao(): boolean {
    const { motivo, observacao } = this.formulario.getRawValue();
    return motivo === 'OUTROS' && !observacao.trim();
  }

  protected salvar(): void {
    if (this.formulario.invalid || this.faltaObservacao() || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const dados = this.formulario.getRawValue();
    const observacao = dados.observacao.trim();
    this.enviando.set(true);
    this.erro.set(null);
    this.api
      .desligar(this.id(), {
        data: dataIso(dados.data!),
        motivo: dados.motivo as MotivoDesligamento,
        observacao: observacao || null,
      })
      .subscribe({
        next: () => {
          this.enviando.set(false);
          this.avisos.open('Desligamento registrado. A vaga foi liberada.', 'Fechar', {
            duration: 5000,
          });
          void this.router.navigate(['/atendidos', this.id()]);
        },
        error: (erro: unknown) => {
          this.enviando.set(false);
          this.erro.set(mensagemDeErro(erro, 'Não foi possível registrar o desligamento.'));
        },
      });
  }
}
