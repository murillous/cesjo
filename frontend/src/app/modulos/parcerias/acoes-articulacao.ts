import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
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

import { mensagemDeErro } from '../../acesso/problema';
import { dataIso, hoje } from '../lista-espera/datas';
import { Articulacao, ParceriasApi } from './parcerias-api';

/**
 * Ações de articulação e fortalecimento da parceria, da mais recente à mais antiga, com o registro de
 * uma ação nova (RF013, ADR 0174). Fica na ficha do parceiro.
 */
@Component({
  selector: 'app-acoes-articulacao',
  imports: [
    DatePipe,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './acoes-articulacao.html',
  styleUrl: './acoes-articulacao.scss',
})
export class AcoesArticulacao {
  readonly parceiroId = input.required<string>();
  readonly podeGerenciar = input(false);

  private readonly api = inject(ParceriasApi);
  private readonly avisos = inject(MatSnackBar);

  protected readonly hoje = hoje();
  protected readonly acoes = signal<Articulacao[] | null>(null);
  protected readonly registrando = signal(false);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    data: new FormControl<Date | null>(hoje(), Validators.required),
    descricao: ['', [Validators.required, Validators.maxLength(2000)]],
  });

  constructor() {
    effect(() => this.carregar(this.parceiroId()));
  }

  protected abrirRegistro(): void {
    this.formulario.reset({ data: hoje(), descricao: '' });
    this.erro.set(null);
    this.registrando.set(true);
  }

  protected cancelar(): void {
    this.registrando.set(false);
    this.erro.set(null);
  }

  protected salvar(): void {
    const { data, descricao } = this.formulario.getRawValue();
    if (this.formulario.invalid || !descricao.trim() || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const id = this.parceiroId();
    this.enviando.set(true);
    this.erro.set(null);
    this.api.registrarArticulacao(id, dataIso(data!), descricao.trim()).subscribe({
      next: () => {
        this.enviando.set(false);
        this.registrando.set(false);
        this.avisos.open('Ação registrada.', 'Fechar', { duration: 4000 });
        this.carregar(id);
      },
      error: (erro: unknown) => {
        this.enviando.set(false);
        this.erro.set(mensagemDeErro(erro, 'Não foi possível registrar a ação.'));
      },
    });
  }

  private carregar(id: string): void {
    this.api.articulacoes(id).subscribe({
      next: (acoes) => this.acoes.set(acoes),
      error: (erro: unknown) =>
        this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar as ações.')),
    });
  }
}
