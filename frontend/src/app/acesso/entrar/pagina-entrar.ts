import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router } from '@angular/router';

import { mensagemDeErro } from '../problema';
import { Sessao } from '../sessao';
import { SessaoApi } from '../sessao-api';
import { ROTA_TROCAR_SENHA } from '../sessao.guard';

/** Tela de entrada no sistema (issue #13). */
@Component({
  selector: 'app-pagina-entrar',
  imports: [
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    ReactiveFormsModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './pagina-entrar.html',
  styleUrl: './pagina-entrar.scss',
})
export class PaginaEntrar {
  /** Endereço para voltar depois de entrar, vindo de `?retorno=`. */
  readonly retorno = input<string>();

  private readonly api = inject(SessaoApi);
  private readonly sessao = inject(Sessao);
  private readonly router = inject(Router);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    login: ['', Validators.required],
    senha: ['', Validators.required],
  });
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly senhaVisivel = signal(false);

  protected entrar(): void {
    if (this.formulario.invalid || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const { login, senha } = this.formulario.getRawValue();
    this.enviando.set(true);
    this.erro.set(null);
    this.api.entrar(login, senha).subscribe({
      next: (conta) => {
        this.sessao.definir(conta);
        void this.router.navigateByUrl(
          conta.trocaSenhaObrigatoria ? ROTA_TROCAR_SENHA : this.destinoSeguro(),
        );
      },
      error: (erro: unknown) => {
        this.enviando.set(false);
        this.formulario.controls.senha.reset();
        this.erro.set(mensagemDeErro(erro, 'Não foi possível entrar. Tente de novo.'));
      },
    });
  }

  /** Só aceita caminhos internos, para o `?retorno=` não levar a outro site. */
  private destinoSeguro(): string {
    const retorno = this.retorno();
    return retorno?.startsWith('/') && !retorno.startsWith('//') ? retorno : '/';
  }
}
