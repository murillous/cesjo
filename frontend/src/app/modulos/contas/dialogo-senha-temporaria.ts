import { Clipboard } from '@angular/cdk/clipboard';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

export interface DadosSenhaTemporaria {
  nome: string;
  login: string;
  senha: string;
}

/** Mostra a senha temporária uma única vez, depois de criar a conta ou redefinir a senha. */
@Component({
  selector: 'app-dialogo-senha-temporaria',
  imports: [MatButtonModule, MatDialogModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>Senha temporária de {{ dados.nome }}</h2>
    <mat-dialog-content>
      <p>
        Entregue o login e esta senha à pessoa. Ela vai criar uma senha própria no primeiro acesso.
        A senha não aparece de novo depois que você fechar esta janela.
      </p>
      <dl>
        <dt>Login</dt>
        <dd>{{ dados.login }}</dd>
        <dt>Senha temporária</dt>
        <dd class="senha">{{ dados.senha }}</dd>
      </dl>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button matButton type="button" (click)="copiar()">
        <mat-icon aria-hidden="true">content_copy</mat-icon>
        {{ copiada() ? 'Senha copiada' : 'Copiar senha' }}
      </button>
      <button matButton="filled" type="button" mat-dialog-close>Fechar</button>
    </mat-dialog-actions>
  `,
  styles: `
    dl {
      display: grid;
      gap: var(--sgi-space-0-5);
      margin: 0;
    }

    dt {
      font-size: var(--sgi-font-size-small);
      color: var(--sgi-color-text-secondary);
    }

    dd {
      margin: 0 0 var(--sgi-space-1);
    }

    .senha {
      padding: var(--sgi-space-1) var(--sgi-space-1-5);
      background: var(--sgi-color-background-secondary);
      border-radius: var(--sgi-radius-sm);
      font-size: var(--sgi-font-size-h3);
      letter-spacing: 0.08em;
      overflow-wrap: anywhere;
    }
  `,
})
export class DialogoSenhaTemporaria {
  protected readonly dados = inject<DadosSenhaTemporaria>(MAT_DIALOG_DATA);
  private readonly clipboard = inject(Clipboard);
  protected readonly copiada = signal(false);

  protected copiar(): void {
    this.copiada.set(this.clipboard.copy(this.dados.senha));
  }
}
