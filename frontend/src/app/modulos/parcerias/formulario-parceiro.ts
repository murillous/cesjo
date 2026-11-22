import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';

import { mensagemDeErro } from '../../acesso/problema';
import { Sessao } from '../../acesso/sessao';
import { dataDeIso, dataIso } from '../lista-espera/datas';
import { AcoesArticulacao } from './acoes-articulacao';
import { DadosParceiro, Parceiro, ParceriasApi } from './parcerias-api';
import { TIPOS_PARCEIRO, TipoParceiro } from './rotulos';

/** Fim da vigência antes do início. */
function vigenciaCoerente(grupo: AbstractControl): ValidationErrors | null {
  const inicio = grupo.get('vigenciaInicio')?.value as Date | null;
  const fim = grupo.get('vigenciaFim')?.value as Date | null;
  return inicio && fim && fim < inicio ? { vigencia: true } : null;
}

/** Texto sem espaços nas pontas, ou `null` quando vazio. */
function texto(valor: string): string | null {
  return valor.trim() || null;
}

/**
 * Cadastro (`/parcerias/novo`) e ficha (`/parcerias/:id`) do parceiro, com as ações de articulação
 * na ficha (RF013, ADR 0172 e ADR 0174). Sem `PARCERIAS_GERENCIAR`, só mostra os dados.
 */
@Component({
  selector: 'app-formulario-parceiro',
  imports: [
    AcoesArticulacao,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    ReactiveFormsModule,
    RouterLink,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './formulario-parceiro.html',
  styleUrl: './formulario-parceiro.scss',
})
export class FormularioParceiro {
  /** Parâmetro `:id` da rota; vazio no cadastro. */
  readonly id = input<string>();

  private readonly api = inject(ParceriasApi);
  private readonly avisos = inject(MatSnackBar);
  private readonly router = inject(Router);

  protected readonly tipos = TIPOS_PARCEIRO;
  protected readonly podeGerenciar = inject(Sessao).temPermissao('PARCERIAS_GERENCIAR');
  protected readonly parceiro = signal<Parceiro | null>(null);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly edicao = computed(() => !!this.id());

  protected readonly formulario = inject(NonNullableFormBuilder).group(
    {
      nome: ['', [Validators.required, Validators.maxLength(150)]],
      tipo: ['' as TipoParceiro | '', Validators.required],
      instrumento: ['', Validators.maxLength(200)],
      vigenciaInicio: new FormControl<Date | null>(null),
      vigenciaFim: new FormControl<Date | null>(null),
      contato: ['', Validators.maxLength(500)],
      observacoes: ['', Validators.maxLength(2000)],
      ativo: [true],
    },
    { validators: vigenciaCoerente },
  );

  constructor() {
    if (!this.podeGerenciar) {
      this.formulario.disable();
    }
    toObservable(this.id)
      .pipe(
        filter((id): id is string => !!id),
        switchMap((id) => this.api.obter(id)),
      )
      .subscribe({
        next: (parceiro) => this.mostrar(parceiro),
        error: (erro: unknown) =>
          this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar o parceiro.')),
      });
  }

  protected salvar(): void {
    if (this.formulario.invalid || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const valor = this.formulario.getRawValue();
    const dados: DadosParceiro = {
      nome: valor.nome.trim(),
      tipo: valor.tipo as TipoParceiro,
      instrumento: texto(valor.instrumento),
      vigenciaInicio: valor.vigenciaInicio ? dataIso(valor.vigenciaInicio) : null,
      vigenciaFim: valor.vigenciaFim ? dataIso(valor.vigenciaFim) : null,
      contato: texto(valor.contato),
      observacoes: texto(valor.observacoes),
    };
    const id = this.id();
    this.enviando.set(true);
    this.erro.set(null);
    (id ? this.api.editar(id, { ...dados, ativo: valor.ativo }) : this.api.criar(dados)).subscribe({
      next: (salvo) => {
        this.enviando.set(false);
        this.avisos.open(id ? 'Parceiro salvo.' : 'Parceiro cadastrado.', 'Fechar', {
          duration: 4000,
        });
        if (id) {
          this.mostrar(salvo);
        } else {
          void this.router.navigate(['/parcerias', salvo.id]);
        }
      },
      error: (erro: unknown) => {
        this.enviando.set(false);
        this.erro.set(mensagemDeErro(erro, 'Não foi possível salvar o parceiro.'));
      },
    });
  }

  private mostrar(parceiro: Parceiro): void {
    this.parceiro.set(parceiro);
    this.formulario.setValue({
      nome: parceiro.nome,
      tipo: parceiro.tipo,
      instrumento: parceiro.instrumento ?? '',
      vigenciaInicio: parceiro.vigenciaInicio ? dataDeIso(parceiro.vigenciaInicio) : null,
      vigenciaFim: parceiro.vigenciaFim ? dataDeIso(parceiro.vigenciaFim) : null,
      contato: parceiro.contato ?? '',
      observacoes: parceiro.observacoes ?? '',
      ativo: parceiro.ativo,
    });
  }
}
