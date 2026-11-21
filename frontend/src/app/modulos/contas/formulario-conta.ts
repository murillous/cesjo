import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { filter, Observable, switchMap } from 'rxjs';

import { Perfil } from '../../acesso/permissao';
import { PERFIS, ROTULOS_PERFIL } from '../../acesso/perfis';
import { mensagemDeErro } from '../../acesso/problema';
import { Sessao } from '../../acesso/sessao';
import { Conta, ContasApi } from './contas-api';
import { DadosConfirmacao, DialogoConfirmacao } from './dialogo-confirmacao';
import { DadosSenhaTemporaria, DialogoSenhaTemporaria } from './dialogo-senha-temporaria';
import { situacaoConta } from './situacao-conta';

/** Mesmo formato de login aceito pelo backend (ADR 0013). */
const LOGIN_VALIDO = /^\s*[A-Za-z0-9._-]{3,60}\s*$/;

/**
 * Criação (`/contas/nova`) e edição (`/contas/:id`) de conta de acesso, com as ações de desativar,
 * reativar e redefinir senha na edição.
 */
@Component({
  selector: 'app-formulario-conta',
  imports: [
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    ReactiveFormsModule,
    RouterLink,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './formulario-conta.html',
  styleUrl: './formulario-conta.scss',
})
export class FormularioConta {
  /** Parâmetro `:id` da rota; vazio na criação. */
  readonly id = input<string>();

  private readonly api = inject(ContasApi);
  private readonly dialogo = inject(MatDialog);
  private readonly avisos = inject(MatSnackBar);
  private readonly router = inject(Router);
  private readonly sessao = inject(Sessao);

  protected readonly perfis = PERFIS;
  protected readonly rotulos = ROTULOS_PERFIL;
  protected readonly conta = signal<Conta | null>(null);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly edicao = computed(() => !!this.id());
  protected readonly propria = computed(() => this.conta()?.id === this.sessao.conta()?.id);
  protected readonly situacao = computed(() => {
    const conta = this.conta();
    return conta ? situacaoConta(conta) : '';
  });

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    login: ['', [Validators.required, Validators.pattern(LOGIN_VALIDO)]],
    nome: ['', [Validators.required, Validators.maxLength(150)]],
    perfil: ['' as Perfil | '', Validators.required],
  });

  constructor() {
    toObservable(this.id)
      .pipe(
        filter((id): id is string => !!id),
        switchMap((id) => this.api.obter(id)),
      )
      .subscribe({
        next: (conta) => this.mostrar(conta),
        error: (erro: unknown) =>
          this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar a conta.')),
      });
  }

  protected salvar(): void {
    if (this.formulario.invalid || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const { login, nome, perfil } = this.formulario.getRawValue();
    const id = this.id();
    if (id) {
      this.executar(this.api.editar(id, { nome, perfil: perfil as Perfil }), (conta) => {
        this.mostrar(conta);
        this.avisar('Conta salva.');
      });
    } else {
      this.executar(this.api.criar({ login, nome, perfil: perfil as Perfil }), (criada) => {
        this.mostrarSenha(criada.conta, criada.senhaTemporaria);
        void this.router.navigate(['/contas', criada.conta.id]);
      });
    }
  }

  protected desativar(conta: Conta): void {
    this.confirmar({
      titulo: 'Desativar conta',
      mensagem: `${conta.nome} não vai mais conseguir entrar no sistema. Você pode reativar a conta depois.`,
      acao: 'Desativar',
    }).subscribe(() =>
      this.executar(this.api.desativar(conta.id), (atualizada) => {
        this.mostrar(atualizada);
        this.avisar('Conta desativada.');
      }),
    );
  }

  protected reativar(conta: Conta): void {
    this.executar(this.api.reativar(conta.id), (atualizada) => {
      this.mostrar(atualizada);
      this.avisar('Conta reativada.');
    });
  }

  protected redefinirSenha(conta: Conta): void {
    this.confirmar({
      titulo: 'Redefinir senha',
      mensagem: `A senha atual de ${conta.nome} deixa de valer. O sistema gera uma senha temporária, que precisa ser trocada no próximo acesso.`,
      acao: 'Redefinir senha',
    }).subscribe(() =>
      this.executar(this.api.redefinirSenha(conta.id), ({ senhaTemporaria }) => {
        this.mostrarSenha(conta, senhaTemporaria);
        this.api.obter(conta.id).subscribe((atualizada) => this.mostrar(atualizada));
      }),
    );
  }

  private mostrar(conta: Conta): void {
    this.conta.set(conta);
    this.formulario.setValue({ login: conta.login, nome: conta.nome, perfil: conta.perfil });
    this.formulario.controls.login.disable();
  }

  private mostrarSenha(conta: Conta, senha: string): void {
    this.dialogo.open<DialogoSenhaTemporaria, DadosSenhaTemporaria>(DialogoSenhaTemporaria, {
      data: { nome: conta.nome, login: conta.login, senha },
      maxWidth: '480px',
    });
  }

  private confirmar(dados: DadosConfirmacao): Observable<boolean> {
    return this.dialogo
      .open<DialogoConfirmacao, DadosConfirmacao, boolean>(DialogoConfirmacao, {
        data: dados,
        maxWidth: '480px',
      })
      .afterClosed()
      .pipe(filter((confirmado): confirmado is true => confirmado === true));
  }

  private executar<T>(operacao: Observable<T>, sucesso: (resultado: T) => void): void {
    this.enviando.set(true);
    this.erro.set(null);
    operacao.subscribe({
      next: (resultado) => {
        this.enviando.set(false);
        sucesso(resultado);
      },
      error: (erro: unknown) => {
        this.enviando.set(false);
        this.erro.set(mensagemDeErro(erro, 'Não foi possível concluir a operação.'));
      },
    });
  }

  private avisar(mensagem: string): void {
    this.avisos.open(mensagem, 'Fechar', { duration: 4000 });
  }
}
