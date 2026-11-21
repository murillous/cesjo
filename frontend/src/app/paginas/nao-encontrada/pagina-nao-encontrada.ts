import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-pagina-nao-encontrada',
  imports: [MatButtonModule, MatIconModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="nao-encontrada">
      <h1>Página não encontrada</h1>
      <p class="texto-secundario">
        O endereço digitado não existe ou foi removido. Volte para o início e use o menu.
      </p>
      <a matButton="filled" routerLink="/">
        <mat-icon aria-hidden="true">home</mat-icon>
        Voltar para o início
      </a>
    </section>
  `,
  styles: `
    .nao-encontrada {
      max-width: 60ch;
    }
  `,
})
export class PaginaNaoEncontrada {}
