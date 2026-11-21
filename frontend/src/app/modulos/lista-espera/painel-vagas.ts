import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';

import { mensagemDeErro } from '../../acesso/problema';
import { Sessao } from '../../acesso/sessao';
import { ListaEsperaApi, PainelVagas as Painel } from './lista-espera-api';

/**
 * Vagas do serviço e tamanho da fila (RF003, ADR 0092), no topo da fila. O Coordenador
 * (`CAPACIDADE_EDITAR`) muda a capacidade aqui mesmo.
 */
@Component({
  selector: 'app-painel-vagas',
  imports: [MatButtonModule, MatFormFieldModule, MatInputModule, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './painel-vagas.html',
  styleUrl: './painel-vagas.scss',
})
export class PainelVagas implements OnInit {
  private readonly api = inject(ListaEsperaApi);
  private readonly avisos = inject(MatSnackBar);

  protected readonly podeEditar = inject(Sessao).temPermissao('CAPACIDADE_EDITAR');

  protected readonly painel = signal<Painel | null>(null);
  protected readonly erro = signal(false);
  protected readonly editando = signal(false);
  protected readonly enviando = signal(false);
  protected readonly erroEdicao = signal<string | null>(null);

  protected readonly vagas = new FormControl<number | null>(null, [
    Validators.required,
    Validators.min(0),
    Validators.max(10000),
  ]);

  ngOnInit(): void {
    this.api.vagas().subscribe({
      next: (painel) => this.painel.set(painel),
      error: () => this.erro.set(true),
    });
  }

  protected editar(): void {
    this.vagas.reset(this.painel()?.capacidade ?? null);
    this.erroEdicao.set(null);
    this.editando.set(true);
  }

  protected cancelar(): void {
    this.editando.set(false);
  }

  protected salvar(): void {
    const vagas = this.vagas.value;
    if (this.vagas.invalid || vagas === null || this.enviando()) {
      this.vagas.markAsTouched();
      return;
    }
    this.enviando.set(true);
    this.api.definirCapacidade(Math.trunc(vagas)).subscribe({
      next: (painel) => {
        this.painel.set(painel);
        this.enviando.set(false);
        this.editando.set(false);
        this.avisos.open(`Capacidade definida em ${painel.capacidade} vagas.`, 'Fechar', {
          duration: 5000,
        });
      },
      error: (erro: unknown) => {
        this.enviando.set(false);
        this.erroEdicao.set(mensagemDeErro(erro, 'Não foi possível salvar a capacidade.'));
      },
    });
  }
}
