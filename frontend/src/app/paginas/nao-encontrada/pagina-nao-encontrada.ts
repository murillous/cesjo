import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';

@Component({
  selector: 'app-pagina-nao-encontrada',
  imports: [ButtonModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="nao-encontrada">
      <h1>Página não encontrada</h1>
      <p class="texto-secundario">O endereço acessado não existe ou foi removido.</p>
      <p-button label="Voltar para o início" icon="pi pi-home" routerLink="/" />
    </section>
  `,
  styles: `
    .nao-encontrada {
      padding: var(--sgi-space-3) 0;
    }
  `,
})
export class PaginaNaoEncontrada {}
