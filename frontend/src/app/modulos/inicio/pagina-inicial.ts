import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-pagina-inicial',
  imports: [DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="boas-vindas">
      <h1>Sistema de Gestão do Centro-Dia</h1>
      <p class="texto-secundario">{{ hoje | date: 'fullDate' }}</p>
      <p>
        Bem-vindo ao SGI do CESJO. Use o menu para acessar os módulos disponíveis para a sua conta.
      </p>
    </section>
  `,
  styles: `
    .boas-vindas {
      padding: var(--sgi-space-3);
      background: var(--sgi-color-background-secondary);
      border-radius: var(--sgi-radius-lg);
      border-left: var(--sgi-space-0-5) solid var(--sgi-color-primary);
    }
  `,
})
export class PaginaInicial {
  protected readonly hoje = new Date();
}
