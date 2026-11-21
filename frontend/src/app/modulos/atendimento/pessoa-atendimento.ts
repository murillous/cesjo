import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';

import { mensagemDeErro } from '../../acesso/problema';
import { Sessao } from '../../acesso/sessao';
import { ROTULOS_FUNCAO } from '../colaboradores/rotulos';
import { lerDataIso } from '../visitas/datas-visita';
import { AtendimentoApi, PessoaAtendida, ResumoEscuta } from './atendimento-api';

/** Atendimento de uma pessoa (`/atendimento/pessoas/:pessoaId`): escutas e, depois, o PIA (ADR 0102). */
@Component({
  selector: 'app-pessoa-atendimento',
  imports: [DatePipe, MatButtonModule, MatIconModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './pessoa-atendimento.html',
  styleUrl: './pessoa-atendimento.scss',
})
export class PessoaAtendimento {
  /** Parâmetro `:pessoaId` da rota. */
  readonly pessoaId = input.required<string>();

  private readonly api = inject(AtendimentoApi);
  private readonly sessao = inject(Sessao);

  protected readonly funcoes = ROTULOS_FUNCAO;
  protected readonly data = lerDataIso;
  protected readonly podeRegistrarEscuta = this.sessao.temPermissao('ESCUTA_REGISTRAR');
  protected readonly pessoa = signal<PessoaAtendida | null>(null);
  protected readonly escutas = signal<ResumoEscuta[] | null>(null);
  protected readonly erro = signal<string | null>(null);

  constructor() {
    const id$ = toObservable(this.pessoaId);
    id$.pipe(switchMap((id) => this.api.pessoa(id))).subscribe({
      next: (pessoa) => this.pessoa.set(pessoa),
      error: (erro: unknown) =>
        this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar a pessoa.')),
    });
    id$.pipe(switchMap((id) => this.api.escutas(id))).subscribe({
      next: (escutas) => this.escutas.set(escutas),
      error: (erro: unknown) =>
        this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar as escutas.')),
    });
  }
}
