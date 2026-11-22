import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { EstatisticasApi, PainelPerfil as Painel } from './estatisticas-api';
import { GraficoBarras } from './grafico-barras';

/**
 * Dashboard de perfil dos usuários atendidos (RF017, issue #56, ADR 0195). Busca os números toda
 * vez que a tela abre e quando a pessoa pede para atualizar; nada fica guardado.
 */
@Component({
  selector: 'app-painel-perfil',
  imports: [DatePipe, GraficoBarras, MatButtonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './painel-perfil.html',
  styleUrl: './painel-perfil.scss',
})
export class PainelPerfil implements OnInit {
  private readonly api = inject(EstatisticasApi);

  protected readonly painel = signal<Painel | null>(null);
  protected readonly erro = signal(false);
  protected readonly carregando = signal(false);

  ngOnInit(): void {
    this.carregar();
  }

  protected carregar(): void {
    this.carregando.set(true);
    this.erro.set(false);
    this.api.perfil().subscribe({
      next: (painel) => {
        this.painel.set(painel);
        this.carregando.set(false);
      },
      error: () => {
        this.erro.set(true);
        this.carregando.set(false);
      },
    });
  }
}
