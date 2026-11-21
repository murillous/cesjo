import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { FormArray, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { filter, Observable, switchMap } from 'rxjs';

import { mensagemDeErro } from '../../acesso/problema';
import { Sessao } from '../../acesso/sessao';
import { dataIso, lerDataIso } from '../visitas/datas-visita';
import {
  AtendimentoApi,
  ConteudoPia,
  ObjetivoPia,
  PassoPia,
  Pia,
  StatusPia,
} from './atendimento-api';
import { ROTULOS_STATUS_PIA } from './rotulos';
import { hoje } from '../lista-espera/datas';

/** Passos do RF006 depois da elaboração, na ordem (ADR 0106). */
const PASSOS: readonly {
  status: StatusPia;
  passo: PassoPia;
  acao: string;
  campo: 'apresentadoFamiliaEm' | 'apresentadoEquipeEm' | 'aprovadoEm';
}[] = [
  {
    status: 'APRESENTADO_FAMILIA',
    passo: 'apresentar-a-familia',
    acao: 'Registrar apresentação à família',
    campo: 'apresentadoFamiliaEm',
  },
  {
    status: 'APRESENTADO_EQUIPE',
    passo: 'apresentar-a-equipe',
    acao: 'Registrar apresentação à equipe',
    campo: 'apresentadoEquipeEm',
  },
  { status: 'APROVADO', passo: 'aprovar', acao: 'Aprovar PIA', campo: 'aprovadoEm' },
];

/**
 * Elaboração (`/atendimento/pessoas/:pessoaId/pia/novo`) e leitura (`/atendimento/pias/:id`) do PIA
 * (RF006, ADR 0105). A estrutura é provisória (P5, ADR 0103). Abrir um PIA é leitura sensível.
 */
@Component({
  selector: 'app-ficha-pia',
  imports: [
    DatePipe,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    ReactiveFormsModule,
    RouterLink,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './ficha-pia.html',
  styleUrl: './ficha-pia.scss',
})
export class FichaPia {
  /** Parâmetro `:pessoaId` da rota de elaboração. */
  readonly pessoaId = input<string>();
  /** Parâmetro `:id` da rota de leitura. */
  readonly id = input<string>();

  private readonly api = inject(AtendimentoApi);
  private readonly avisos = inject(MatSnackBar);
  private readonly router = inject(Router);
  private readonly campos = inject(NonNullableFormBuilder);
  protected readonly sessao = inject(Sessao);

  protected readonly status = ROTULOS_STATUS_PIA;
  protected readonly data = lerDataIso;
  protected readonly podeEditar = this.sessao.temPermissao('PIA_EDITAR');
  protected readonly podeAprovar = this.sessao.temPermissao('PIA_APROVAR');
  protected readonly passos = PASSOS;
  protected readonly hojeData = hoje();
  protected readonly dataDoPasso = signal<Date | null>(hoje());
  /** Próximo passo da versão mais nova, se a conta pode dá-lo. */
  protected readonly proximo = computed(() => {
    const pia = this.pia();
    if (!pia?.maisNova) {
      return null;
    }
    const ordem: StatusPia[] = [
      'ELABORADO',
      'APRESENTADO_FAMILIA',
      'APRESENTADO_EQUIPE',
      'APROVADO',
    ];
    const passo = PASSOS.find((p) => ordem.indexOf(p.status) === ordem.indexOf(pia.status) + 1);
    if (!passo) {
      return null;
    }
    return (passo.status === 'APROVADO' ? this.podeAprovar : this.podeEditar) ? passo : null;
  });
  protected readonly pia = signal<Pia | null>(null);
  protected readonly nomePessoa = signal<string | null>(null);
  protected readonly editando = signal(false);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly novo = computed(() => !this.id());
  protected readonly pessoaDaFicha = computed(() => this.pia()?.pessoaId ?? this.pessoaId());

  protected readonly formulario = this.campos.group({
    diagnostico: ['', Validators.maxLength(10000)],
    objetivos: this.campos.array([this.objetivo()]),
    observacoes: ['', Validators.maxLength(10000)],
  });

  constructor() {
    toObservable(this.pessoaId)
      .pipe(
        filter((id): id is string => !!id),
        switchMap((id) => this.api.pessoa(id)),
      )
      .subscribe({ next: (pessoa) => this.nomePessoa.set(pessoa.nomeCompleto) });
    toObservable(this.id)
      .pipe(
        filter((id): id is string => !!id),
        switchMap((id) => this.api.pia(id)),
      )
      .subscribe({
        next: (pia) => this.mostrar(pia),
        error: (erro: unknown) =>
          this.erro.set(mensagemDeErro(erro, 'Não foi possível abrir o PIA.')),
      });
  }

  protected get objetivos(): FormArray {
    return this.formulario.controls.objetivos;
  }

  protected acrescentarObjetivo(): void {
    this.objetivos.push(this.objetivo());
  }

  protected removerObjetivo(indice: number): void {
    this.objetivos.removeAt(indice);
  }

  protected editar(): void {
    this.editando.set(true);
  }

  protected desistir(): void {
    const pia = this.pia();
    if (pia) {
      this.mostrar(pia);
    }
  }

  protected salvar(): void {
    if (this.formulario.invalid || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const id = this.id();
    const conteudo = this.conteudo();
    this.executar(
      id ? this.api.editarPia(id, conteudo) : this.api.elaborarPia(this.pessoaId()!, conteudo),
      (pia) => {
        this.avisos.open(id ? 'PIA salvo.' : 'PIA elaborado.', 'Fechar', { duration: 4000 });
        if (id) {
          this.mostrar(pia);
        } else {
          void this.router.navigate(['/atendimento', 'pias', pia.id]);
        }
      },
    );
  }

  protected avancar(pia: Pia, passo: PassoPia): void {
    const data = this.dataDoPasso();
    if (!data) {
      this.erro.set('Informe a data.');
      return;
    }
    this.executar(this.api.avancarPia(pia.id, passo, dataIso(data)), (atualizado) => {
      this.mostrar(atualizado);
      this.dataDoPasso.set(hoje());
      this.avisos.open('Andamento do PIA registrado.', 'Fechar', { duration: 4000 });
    });
  }

  protected novaVersao(pia: Pia): void {
    this.executar(this.api.novaVersaoPia(pia.id), (nova) => {
      this.avisos.open(`Versão ${nova.numeroVersao} criada para edição.`, 'Fechar', {
        duration: 4000,
      });
      void this.router.navigate(['/atendimento', 'pias', nova.id]);
    });
  }

  /** O navegador imprime ou salva em PDF a versão aberta, só com o plano (ADR 0108). */
  protected imprimir(): void {
    window.print();
  }

  protected executar(operacao: Observable<Pia>, sucesso: (pia: Pia) => void): void {
    this.enviando.set(true);
    this.erro.set(null);
    operacao.subscribe({
      next: (pia) => {
        this.enviando.set(false);
        sucesso(pia);
      },
      error: (erro: unknown) => {
        this.enviando.set(false);
        this.erro.set(mensagemDeErro(erro, 'Não foi possível salvar o PIA.'));
      },
    });
  }

  protected mostrar(pia: Pia): void {
    this.pia.set(pia);
    this.nomePessoa.set(pia.nomePessoa);
    this.editando.set(false);
    this.erro.set(null);
    this.objetivos.clear();
    for (const objetivo of pia.conteudo.objetivos) {
      this.objetivos.push(this.objetivo(objetivo));
    }
    this.formulario.patchValue({
      diagnostico: pia.conteudo.diagnostico ?? '',
      observacoes: pia.conteudo.observacoes ?? '',
    });
  }

  private conteudo(): ConteudoPia {
    const valor = this.formulario.getRawValue();
    const texto = (t: string) => t.trim() || null;
    return {
      diagnostico: texto(valor.diagnostico),
      objetivos: valor.objetivos.map((o) => ({
        descricao: o.descricao.trim(),
        metas: texto(o.metas),
        acoes: texto(o.acoes),
        prazo: o.prazo ? dataIso(o.prazo) : null,
        responsavel: texto(o.responsavel),
      })),
      observacoes: texto(valor.observacoes),
    };
  }

  private objetivo(objetivo?: ObjetivoPia) {
    return this.campos.group({
      descricao: [objetivo?.descricao ?? '', [Validators.required, Validators.maxLength(2000)]],
      metas: [objetivo?.metas ?? '', Validators.maxLength(5000)],
      acoes: [objetivo?.acoes ?? '', Validators.maxLength(5000)],
      prazo: [lerDataIso(objetivo?.prazo) as Date | null],
      responsavel: [objetivo?.responsavel ?? '', Validators.maxLength(150)],
    });
  }
}
