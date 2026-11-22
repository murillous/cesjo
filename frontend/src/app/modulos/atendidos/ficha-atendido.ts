import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';

import { mensagemDeErro } from '../../acesso/problema';
import { Sessao } from '../../acesso/sessao';
import { ORIGENS_ENCAMINHAMENTO, TIPOS_DEFICIENCIA } from '../lista-espera/rotulos';
import { AtendidosApi, CadastroComplementar, FichaAtendido } from './atendidos-api';
import { PainelViolacoes } from './painel-violacoes';
import {
  CRENCAS,
  ESCOLARIDADES,
  GENEROS,
  ROTULOS_SITUACAO_INSERCAO,
  rotuloDe,
  rotuloFaixa,
  rotuloMotivo,
  rotuloTurno,
} from './rotulos';

/**
 * Ficha do usuário atendido (`/atendidos/:id`, RF007, ADR 0083). Mostra o que a conta pode ver: a
 * ficha completa (com crença e saúde só para quem tem `ATENDIDOS_DADOS_SENSIVEIS`) ou só os dados
 * básicos. Visitas, escutas e PIA entram aqui com o módulo de
 * atendimento.
 */
@Component({
  selector: 'app-ficha-atendido',
  imports: [
    CurrencyPipe,
    DatePipe,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    PainelViolacoes,
    RouterLink,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './ficha-atendido.html',
  styleUrl: './ficha-atendido.scss',
})
export class FichaAtendidoPagina implements OnInit {
  /** Parâmetro da rota. */
  readonly id = input.required<string>();

  private readonly api = inject(AtendidosApi);

  protected readonly ficha = signal<FichaAtendido | null>(null);
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);
  protected readonly situacoes = ROTULOS_SITUACAO_INSERCAO;
  protected readonly rotuloTurno = rotuloTurno;
  protected readonly rotuloFaixa = rotuloFaixa;
  protected readonly rotuloMotivo = rotuloMotivo;
  protected readonly rotuloDe = rotuloDe;
  protected readonly generos = GENEROS;
  protected readonly escolaridades = ESCOLARIDADES;
  protected readonly crencas = CRENCAS;
  private readonly sessao = inject(Sessao);
  protected readonly podeEditar = this.sessao.temPermissao('ATENDIDOS_EDITAR');
  protected readonly podeDesligar = this.sessao.temPermissao('ATENDIDOS_DESLIGAR');
  protected readonly podeRegistrarViolacao = this.sessao.temPermissao('VIOLACOES_REGISTRAR');
  protected readonly podeLerViolacoes = this.sessao.temPermissao('VIOLACOES_LER');

  ngOnInit(): void {
    this.api.ficha(this.id()).subscribe({
      next: (ficha) => {
        this.ficha.set(ficha);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.carregando.set(false);
        this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar a ficha.'));
      },
    });
  }

  protected rotuloTipo(tipo: string): string {
    return TIPOS_DEFICIENCIA.find((t) => t.valor === tipo)?.rotulo ?? tipo;
  }

  protected rotuloOrigem(origem: string): string {
    return ORIGENS_ENCAMINHAMENTO.find((o) => o.valor === origem)?.rotulo ?? origem;
  }

  /** Rua, número, complemento, cidade e CEP numa linha; vazio se nada foi informado. */
  protected endereco(c: CadastroComplementar): string {
    const rua = [c.logradouro, c.numero].filter(Boolean).join(', ');
    const cep = c.cep ? `CEP ${c.cep.slice(0, 5)}-${c.cep.slice(5)}` : null;
    return [rua, c.complemento, c.cidade, cep].filter(Boolean).join(' – ');
  }

  protected beneficios(c: CadastroComplementar): string {
    const lista = [
      c.recebeBpc ? 'BPC' : null,
      c.inscritoCadUnico ? 'CadÚnico' : null,
      c.recebeBolsaFamilia ? 'Bolsa Família' : null,
    ].filter(Boolean);
    return lista.length ? lista.join(', ') : 'Nenhum informado';
  }

  protected telefone(digitos: string): string {
    const ddd = digitos.slice(0, 2);
    const numero = digitos.slice(2);
    const corte = numero.length - 4;
    return `(${ddd}) ${numero.slice(0, corte)}-${numero.slice(corte)}`;
  }
}
