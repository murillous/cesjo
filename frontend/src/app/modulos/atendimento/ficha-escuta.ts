import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';

import { mensagemDeErro } from '../../acesso/problema';
import { Sessao } from '../../acesso/sessao';
import { ROTULOS_FUNCAO } from '../colaboradores/rotulos';
import { hoje } from '../lista-espera/datas';
import { dataIso, lerDataIso } from '../visitas/datas-visita';
import { Profissional } from '../visitas/visitas-api';
import { AtendimentoApi, DadosEscuta, Escuta } from './atendimento-api';

/** Os quatro textos do RF005, na ordem do documento de requisitos. */
const CAMPOS = [
  { nome: 'dinamicaFamiliar', rotulo: 'Dinâmica familiar' },
  { nome: 'necessidadesImediatas', rotulo: 'Necessidades imediatas' },
  { nome: 'habilidades', rotulo: 'Habilidades identificadas' },
  { nome: 'potencialidades', rotulo: 'Potencialidades' },
] as const;

/**
 * Registro (`/atendimento/pessoas/:pessoaId/escutas/nova`) e leitura (`/atendimento/escutas/:id`) da
 * escuta ativa e qualificada (RF005, ADR 0102). Abrir uma escuta é leitura sensível, registrada no log.
 */
@Component({
  selector: 'app-ficha-escuta',
  imports: [
    DatePipe,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    ReactiveFormsModule,
    RouterLink,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './ficha-escuta.html',
  styleUrl: './ficha-escuta.scss',
})
export class FichaEscuta {
  /** Parâmetro `:pessoaId` da rota de registro. */
  readonly pessoaId = input<string>();
  /** Parâmetro `:id` da rota de leitura. */
  readonly id = input<string>();

  private readonly api = inject(AtendimentoApi);
  private readonly avisos = inject(MatSnackBar);
  private readonly router = inject(Router);

  protected readonly campos = CAMPOS;
  protected readonly funcoes = ROTULOS_FUNCAO;
  protected readonly hoje = hoje();
  protected readonly data = lerDataIso;
  protected readonly podeRegistrar = inject(Sessao).temPermissao('ESCUTA_REGISTRAR');
  protected readonly escuta = signal<Escuta | null>(null);
  protected readonly nomePessoa = signal<string | null>(null);
  protected readonly profissionais = signal<Profissional[]>([]);
  protected readonly editando = signal(false);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly nova = computed(() => !this.id());
  protected readonly pessoaDaFicha = computed(() => this.escuta()?.pessoaId ?? this.pessoaId());

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    dataEscuta: [hoje() as Date | null, Validators.required],
    profissionalId: ['', Validators.required],
    dinamicaFamiliar: ['', Validators.maxLength(10000)],
    necessidadesImediatas: ['', Validators.maxLength(10000)],
    habilidades: ['', Validators.maxLength(10000)],
    potencialidades: ['', Validators.maxLength(10000)],
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
        switchMap((id) => this.api.escuta(id)),
      )
      .subscribe({
        next: (escuta) => this.mostrar(escuta),
        error: (erro: unknown) =>
          this.erro.set(mensagemDeErro(erro, 'Não foi possível abrir a escuta.')),
      });
    if (this.podeRegistrar) {
      this.api.profissionaisDaEscuta().subscribe({
        next: ({ profissionais, sugeridoId }) => {
          this.profissionais.set(profissionais);
          const controle = this.formulario.controls.profissionalId;
          if (sugeridoId && !controle.value && !this.id()) {
            controle.setValue(sugeridoId);
          }
        },
        error: () => this.profissionais.set([]),
      });
    }
  }

  /** Profissionais para escolher, com o da escuta mesmo que tenha ficado inativo. */
  protected readonly opcoes = computed(() => {
    const atual = this.escuta()?.profissional;
    const lista = this.profissionais();
    return atual && !lista.some((p) => p.id === atual.id) ? [atual, ...lista] : lista;
  });

  protected corrigir(): void {
    this.editando.set(true);
  }

  protected desistir(): void {
    const escuta = this.escuta();
    if (escuta) {
      this.mostrar(escuta);
    }
  }

  protected salvar(): void {
    const valor = this.formulario.getRawValue();
    const algumTexto = CAMPOS.some((c) => valor[c.nome].trim());
    if (this.formulario.invalid || !algumTexto || this.enviando()) {
      this.formulario.markAllAsTouched();
      if (!algumTexto) {
        this.erro.set('Preencha pelo menos um dos campos da escuta.');
      }
      return;
    }
    const dados: DadosEscuta = {
      dataEscuta: dataIso(valor.dataEscuta!),
      profissionalId: valor.profissionalId,
      dinamicaFamiliar: valor.dinamicaFamiliar.trim() || null,
      necessidadesImediatas: valor.necessidadesImediatas.trim() || null,
      habilidades: valor.habilidades.trim() || null,
      potencialidades: valor.potencialidades.trim() || null,
    };
    const id = this.id();
    const operacao = id
      ? this.api.corrigirEscuta(id, dados)
      : this.api.registrarEscuta(this.pessoaId()!, dados);
    this.enviando.set(true);
    this.erro.set(null);
    operacao.subscribe({
      next: (escuta) => {
        this.enviando.set(false);
        this.avisos.open(id ? 'Escuta corrigida.' : 'Escuta registrada.', 'Fechar', {
          duration: 4000,
        });
        if (id) {
          this.mostrar(escuta);
        } else {
          void this.router.navigate(['/atendimento', 'pessoas', escuta.pessoaId]);
        }
      },
      error: (erro: unknown) => {
        this.enviando.set(false);
        this.erro.set(mensagemDeErro(erro, 'Não foi possível salvar a escuta.'));
      },
    });
  }

  private mostrar(escuta: Escuta): void {
    this.escuta.set(escuta);
    this.nomePessoa.set(escuta.nomePessoa);
    this.editando.set(false);
    this.erro.set(null);
    this.formulario.setValue({
      dataEscuta: lerDataIso(escuta.dataEscuta),
      profissionalId: escuta.profissional.id,
      dinamicaFamiliar: escuta.dinamicaFamiliar ?? '',
      necessidadesImediatas: escuta.necessidadesImediatas ?? '',
      habilidades: escuta.habilidades ?? '',
      potencialidades: escuta.potencialidades ?? '',
    });
  }
}
