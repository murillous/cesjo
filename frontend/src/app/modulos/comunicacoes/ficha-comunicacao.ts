import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';

import { mensagemDeErro } from '../../acesso/problema';
import { Sessao } from '../../acesso/sessao';
import { hoje } from '../lista-espera/datas';
import { dataIso, lerDataIso } from '../visitas/datas-visita';
import { Comunicacao, ComunicacoesApi, DadosComunicacao } from './comunicacoes-api';
import { TIPO_POR_VALOR, TipoComunicacao, TIPOS_COMUNICACAO } from './rotulos';

/**
 * Registro (`/comunicacoes/nova`), leitura e correção (`/comunicacoes/:id`) de um contato ou
 * comunicação do RF011 (ADR 0163). O tipo se escolhe por cartões com ícone, porque muda o rótulo do
 * campo "pessoa ou instituição".
 */
@Component({
  selector: 'app-ficha-comunicacao',
  imports: [
    DatePipe,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    ReactiveFormsModule,
    RouterLink,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './ficha-comunicacao.html',
  styleUrl: './ficha-comunicacao.scss',
})
export class FichaComunicacao {
  /** Parâmetro `:id` da rota de leitura. */
  readonly id = input<string>();

  private readonly api = inject(ComunicacoesApi);
  private readonly avisos = inject(MatSnackBar);
  private readonly router = inject(Router);

  protected readonly tipos = TIPOS_COMUNICACAO;
  protected readonly tipoPorValor = TIPO_POR_VALOR;
  protected readonly hoje = hoje();
  protected readonly data = lerDataIso;
  protected readonly podeRegistrar = inject(Sessao).temPermissao('COMUNICACOES_REGISTRAR');
  protected readonly comunicacao = signal<Comunicacao | null>(null);
  protected readonly editando = signal(false);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly nova = computed(() => !this.id());

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    tipo: ['' as TipoComunicacao | '', Validators.required],
    data: [hoje() as Date | null, Validators.required],
    contraparte: ['', [Validators.required, Validators.maxLength(200)]],
    assunto: ['', [Validators.required, Validators.maxLength(200)]],
    descricao: ['', Validators.maxLength(10000)],
  });

  private readonly tipoEscolhido = toSignal(this.formulario.controls.tipo.valueChanges, {
    initialValue: this.formulario.controls.tipo.value,
  });

  /** Rótulo do campo "pessoa ou instituição", conforme o tipo escolhido. */
  protected readonly rotuloContraparte = computed(() => {
    const tipo = this.tipoEscolhido();
    return tipo ? TIPO_POR_VALOR[tipo].contraparte : 'Pessoa ou instituição';
  });

  constructor() {
    toObservable(this.id)
      .pipe(
        filter((id): id is string => !!id),
        switchMap((id) => this.api.comunicacao(id)),
      )
      .subscribe({
        next: (comunicacao) => this.mostrar(comunicacao),
        error: (erro: unknown) =>
          this.erro.set(mensagemDeErro(erro, 'Não foi possível abrir o registro.')),
      });
  }

  protected corrigir(): void {
    this.editando.set(true);
  }

  protected desistir(): void {
    const comunicacao = this.comunicacao();
    if (comunicacao) {
      this.mostrar(comunicacao);
    }
  }

  protected salvar(): void {
    if (this.formulario.invalid || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const valor = this.formulario.getRawValue();
    const dados: DadosComunicacao = {
      tipo: valor.tipo as TipoComunicacao,
      data: dataIso(valor.data!),
      contraparte: valor.contraparte.trim(),
      assunto: valor.assunto.trim(),
      descricao: valor.descricao.trim() || null,
    };
    const id = this.id();
    const operacao = id ? this.api.corrigir(id, dados) : this.api.registrar(dados);
    this.enviando.set(true);
    this.erro.set(null);
    operacao.subscribe({
      next: (comunicacao) => {
        this.enviando.set(false);
        this.avisos.open(id ? 'Registro corrigido.' : 'Contato registrado.', 'Fechar', {
          duration: 4000,
        });
        if (id) {
          this.mostrar(comunicacao);
        } else {
          void this.router.navigate(['/comunicacoes', comunicacao.id]);
        }
      },
      error: (erro: unknown) => {
        this.enviando.set(false);
        this.erro.set(mensagemDeErro(erro, 'Não foi possível salvar o registro.'));
      },
    });
  }

  private mostrar(comunicacao: Comunicacao): void {
    this.comunicacao.set(comunicacao);
    this.editando.set(false);
    this.erro.set(null);
    this.formulario.setValue({
      tipo: comunicacao.tipo,
      data: lerDataIso(comunicacao.data),
      contraparte: comunicacao.contraparte,
      assunto: comunicacao.assunto,
      descricao: comunicacao.descricao ?? '',
    });
  }
}
