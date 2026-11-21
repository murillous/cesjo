import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';

import { mensagemDeErro } from '../problema';
import { Sessao } from '../sessao';
import { SessaoApi } from '../sessao-api';

/** Mesmo valor de `sgi.acesso.senha-tamanho-minimo` no backend (ADR 0012). */
export const TAMANHO_MINIMO_SENHA = 12;

function confirmacaoIgual(grupo: AbstractControl): ValidationErrors | null {
  const nova = grupo.get('novaSenha')?.value;
  const confirmacao = grupo.get('confirmacao')?.value;
  return confirmacao && nova !== confirmacao ? { confirmacaoDiferente: true } : null;
}

/**
 * Troca da própria senha. É obrigatória no primeiro acesso e depois de uma redefinição: enquanto
 * `trocaSenhaObrigatoria`, o app só mostra esta tela (issue #15).
 */
@Component({
  selector: 'app-pagina-trocar-senha',
  imports: [MatButtonModule, MatFormFieldModule, MatInputModule, ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './pagina-trocar-senha.html',
  styleUrl: './pagina-trocar-senha.scss',
})
export class PaginaTrocarSenha {
  private readonly api = inject(SessaoApi);
  private readonly sessao = inject(Sessao);
  private readonly router = inject(Router);

  protected readonly tamanhoMinimo = TAMANHO_MINIMO_SENHA;
  protected readonly obrigatoria = computed(
    () => this.sessao.conta()?.trocaSenhaObrigatoria ?? false,
  );
  protected readonly formulario = inject(NonNullableFormBuilder).group(
    {
      senhaAtual: ['', Validators.required],
      novaSenha: ['', [Validators.required, Validators.minLength(TAMANHO_MINIMO_SENHA)]],
      confirmacao: ['', Validators.required],
    },
    { validators: confirmacaoIgual },
  );
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected trocar(): void {
    if (this.formulario.invalid || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const { senhaAtual, novaSenha } = this.formulario.getRawValue();
    this.enviando.set(true);
    this.erro.set(null);
    this.api.trocarSenha(senhaAtual, novaSenha).subscribe({
      next: (conta) => {
        this.sessao.definir(conta);
        void this.router.navigateByUrl('/');
      },
      error: (erro: unknown) => {
        this.enviando.set(false);
        this.erro.set(mensagemDeErro(erro, 'Não foi possível trocar a senha. Tente de novo.'));
      },
    });
  }

  protected sair(): void {
    this.api.sair().subscribe({
      complete: () => this.aoSair(),
      error: () => this.aoSair(),
    });
  }

  private aoSair(): void {
    this.sessao.definir(null);
    void this.router.navigateByUrl('/entrar');
  }
}
