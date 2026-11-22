import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import {
  FormControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';

import { mensagemDeErro } from '../../acesso/problema';
import { dataIso, hoje } from '../lista-espera/datas';
import { ColaboradoresApi, Vinculo } from './colaboradores-api';

/**
 * Histórico de vínculos do colaborador, com o registro da contratação e do desligamento (RF014,
 * ADR 0170). Fica na ficha do colaborador; avisa a ficha quando a situação pode ter mudado.
 */
@Component({
  selector: 'app-historico-vinculos',
  imports: [
    DatePipe,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './historico-vinculos.html',
  styleUrl: './historico-vinculos.scss',
})
export class HistoricoVinculos {
  readonly colaboradorId = input.required<string>();
  readonly podeGerenciar = input(false);
  /** Emitido depois de registrar contratação ou desligamento, que mudam a situação. */
  readonly mudou = output();

  private readonly api = inject(ColaboradoresApi);
  private readonly avisos = inject(MatSnackBar);

  protected readonly hoje = hoje();
  protected readonly vinculos = signal<Vinculo[] | null>(null);
  protected readonly aberto = computed(() => this.vinculos()?.find((v) => !v.dataDesligamento));
  protected readonly registrando = signal(false);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    data: new FormControl<Date | null>(hoje(), Validators.required),
    motivo: ['', Validators.maxLength(2000)],
  });

  constructor() {
    effect(() => this.carregar(this.colaboradorId()));
  }

  protected abrirRegistro(): void {
    this.formulario.reset({ data: hoje(), motivo: '' });
    const motivo = this.formulario.controls.motivo;
    motivo.setValidators(
      this.aberto()
        ? [Validators.required, Validators.maxLength(2000)]
        : Validators.maxLength(2000),
    );
    motivo.updateValueAndValidity();
    this.erro.set(null);
    this.registrando.set(true);
  }

  protected cancelar(): void {
    this.registrando.set(false);
    this.erro.set(null);
  }

  protected salvar(): void {
    const { data, motivo } = this.formulario.getRawValue();
    if (this.formulario.invalid || (this.aberto() && !motivo.trim()) || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const id = this.colaboradorId();
    const desligando = !!this.aberto();
    const operacao: Observable<Vinculo> = desligando
      ? this.api.desligar(id, dataIso(data!), motivo.trim())
      : this.api.contratar(id, dataIso(data!));
    this.enviando.set(true);
    this.erro.set(null);
    operacao.subscribe({
      next: () => {
        this.enviando.set(false);
        this.registrando.set(false);
        this.avisos.open(
          desligando ? 'Desligamento registrado.' : 'Contratação registrada.',
          'Fechar',
          { duration: 4000 },
        );
        this.carregar(id);
        this.mudou.emit();
      },
      error: (erro: unknown) => {
        this.enviando.set(false);
        this.erro.set(
          mensagemDeErro(
            erro,
            desligando
              ? 'Não foi possível registrar o desligamento.'
              : 'Não foi possível registrar a contratação.',
          ),
        );
      },
    });
  }

  private carregar(id: string): void {
    this.api.vinculos(id).subscribe({
      next: (vinculos) => this.vinculos.set(vinculos),
      error: (erro: unknown) =>
        this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar os vínculos.')),
    });
  }
}
