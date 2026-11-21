import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { Router, RouterLink } from '@angular/router';

import { ROTULOS_PERFIL } from '../perfis';
import { Sessao } from '../sessao';
import { SessaoApi } from '../sessao-api';

/** Menu da conta na barra superior: quem está logado, trocar senha e sair. */
@Component({
  selector: 'app-menu-conta',
  imports: [MatButtonModule, MatDividerModule, MatIconModule, MatMenuModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button matIconButton type="button" aria-label="Minha conta" [matMenuTriggerFor]="menu">
      <mat-icon aria-hidden="true">account_circle</mat-icon>
    </button>
    <mat-menu #menu="matMenu" xPosition="before">
      @if (sessao.conta(); as conta) {
        <div class="quem">
          <span class="nome">{{ conta.nome }}</span>
          <span class="texto-secundario">{{ conta.login }}</span>
          <span class="texto-secundario">{{ perfil() }}</span>
        </div>
        <mat-divider />
      }
      <a mat-menu-item routerLink="/trocar-senha">
        <mat-icon aria-hidden="true">password</mat-icon>
        <span>Trocar senha</span>
      </a>
      <button mat-menu-item type="button" (click)="sair()">
        <mat-icon aria-hidden="true">logout</mat-icon>
        <span>Sair</span>
      </button>
    </mat-menu>
  `,
  styles: `
    .quem {
      display: flex;
      flex-direction: column;
      padding: var(--sgi-space-1-5) var(--sgi-space-2);
      max-width: 280px;
    }

    .nome {
      font-weight: var(--sgi-font-weight-semibold);
      overflow-wrap: anywhere;
    }
  `,
})
export class MenuConta {
  protected readonly sessao = inject(Sessao);
  private readonly api = inject(SessaoApi);
  private readonly router = inject(Router);

  protected readonly perfil = computed(() => {
    const conta = this.sessao.conta();
    return conta ? ROTULOS_PERFIL[conta.perfil] : '';
  });

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
