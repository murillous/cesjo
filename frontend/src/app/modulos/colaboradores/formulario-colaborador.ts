import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { filter, Observable, switchMap } from 'rxjs';

import { mensagemDeErro } from '../../acesso/problema';
import { Sessao } from '../../acesso/sessao';
import { ContasApi } from '../contas/contas-api';
import { Colaborador, ColaboradoresApi, ContaLigada } from './colaboradores-api';
import { FuncaoColaborador, FUNCOES } from './rotulos';

/** Quantas contas a escolha da conta de acesso carrega; o sistema tem dezenas. */
const LIMITE_CONTAS = 200;

/**
 * Cadastro (`/colaboradores/novo`) e edição (`/colaboradores/:id`) de colaborador, com a situação e a
 * conta de acesso ligada (ADR 0045 e ADR 0046). Sem `COLABORADORES_GERENCIAR`, só mostra os dados.
 */
@Component({
  selector: 'app-formulario-colaborador',
  imports: [
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    ReactiveFormsModule,
    RouterLink,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './formulario-colaborador.html',
  styleUrl: './formulario-colaborador.scss',
})
export class FormularioColaborador {
  /** Parâmetro `:id` da rota; vazio no cadastro. */
  readonly id = input<string>();

  private readonly api = inject(ColaboradoresApi);
  private readonly contasApi = inject(ContasApi);
  private readonly avisos = inject(MatSnackBar);
  private readonly router = inject(Router);
  private readonly sessao = inject(Sessao);

  protected readonly funcoes = FUNCOES;
  protected readonly podeGerenciar = this.sessao.temPermissao('COLABORADORES_GERENCIAR');
  protected readonly colaborador = signal<Colaborador | null>(null);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly edicao = computed(() => !!this.id());
  private readonly contasCarregadas = signal<ContaLigada[]>([]);

  /** Contas que podem ser escolhidas, sempre com a conta já ligada, mesmo que fora da lista. */
  protected readonly contas = computed(() => {
    const lista = this.contasCarregadas();
    const ligada = this.colaborador()?.conta;
    return ligada && !lista.some((c) => c.id === ligada.id) ? [ligada, ...lista] : lista;
  });

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    nome: ['', [Validators.required, Validators.maxLength(150)]],
    funcao: ['' as FuncaoColaborador | '', Validators.required],
    contaId: [''],
    ativo: [true],
  });

  constructor() {
    if (!this.podeGerenciar) {
      this.formulario.disable();
    }
    // A escolha da conta lê a lista de contas, que só quem gerencia contas pode ver (ADR 0046).
    if (this.podeGerenciar && this.sessao.temPermissao('CONTAS_GERENCIAR')) {
      this.contasApi.listar(0, LIMITE_CONTAS).subscribe({
        next: (pagina) =>
          this.contasCarregadas.set(
            pagina.content.map(({ id, login, nome, ativa }) => ({ id, login, nome, ativa })),
          ),
        error: () => this.contasCarregadas.set([]),
      });
    }
    toObservable(this.id)
      .pipe(
        filter((id): id is string => !!id),
        switchMap((id) => this.api.obter(id)),
      )
      .subscribe({
        next: (colaborador) => this.mostrar(colaborador),
        error: (erro: unknown) =>
          this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar o colaborador.')),
      });
  }

  protected salvar(): void {
    if (this.formulario.invalid || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const { nome, funcao, contaId, ativo } = this.formulario.getRawValue();
    const dados = { nome, funcao: funcao as FuncaoColaborador, contaId: contaId || null };
    const id = this.id();
    this.executar(
      id ? this.api.editar(id, { ...dados, ativo }) : this.api.criar(dados),
      (salvo) => {
        this.avisos.open(id ? 'Colaborador salvo.' : 'Colaborador cadastrado.', 'Fechar', {
          duration: 4000,
        });
        if (id) {
          this.mostrar(salvo);
        } else {
          void this.router.navigate(['/colaboradores', salvo.id]);
        }
      },
    );
  }

  private mostrar(colaborador: Colaborador): void {
    this.colaborador.set(colaborador);
    this.formulario.setValue({
      nome: colaborador.nome,
      funcao: colaborador.funcao,
      contaId: colaborador.conta?.id ?? '',
      ativo: colaborador.ativo,
    });
  }

  private executar(operacao: Observable<Colaborador>, sucesso: (salvo: Colaborador) => void): void {
    this.enviando.set(true);
    this.erro.set(null);
    operacao.subscribe({
      next: (salvo) => {
        this.enviando.set(false);
        sucesso(salvo);
      },
      error: (erro: unknown) => {
        this.enviando.set(false);
        this.erro.set(mensagemDeErro(erro, 'Não foi possível salvar o colaborador.'));
      },
    });
  }
}
