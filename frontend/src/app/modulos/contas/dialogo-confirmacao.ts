import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

export interface DadosConfirmacao {
  titulo: string;
  mensagem: string;
  acao: string;
}

/** Pede confirmação antes de uma ação sobre a conta. Fecha com `true` quando confirmada. */
@Component({
  selector: 'app-dialogo-confirmacao',
  imports: [MatButtonModule, MatDialogModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>{{ dados.titulo }}</h2>
    <mat-dialog-content>
      <p>{{ dados.mensagem }}</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button matButton type="button" [mat-dialog-close]="false">Cancelar</button>
      <button matButton="filled" type="button" [mat-dialog-close]="true">{{ dados.acao }}</button>
    </mat-dialog-actions>
  `,
})
export class DialogoConfirmacao {
  protected readonly dados = inject<DadosConfirmacao>(MAT_DIALOG_DATA);
}
