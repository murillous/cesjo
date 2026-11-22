import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
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
import { Atividade, AtividadesApi, DadosAtividade, Participante } from './atividades-api';
import { descricaoTipo, ROTULOS_TIPO_ATIVIDADE, TipoAtividade, TIPOS_ATIVIDADE } from './rotulos';

/**
 * Registro (`/atividades/nova`), leitura e correção (`/atividades/:id`) de uma atividade do RF010
 * (ADR 0153). Participantes são usuários atendidos ativos; profissionais, colaboradores ativos.
 * Atendimentos e estudos de caso têm um só usuário, e abrir um deles fica no log (ADR 0154).
 */
@Component({
  selector: 'app-ficha-atividade',
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
  templateUrl: './ficha-atividade.html',
  styleUrl: './ficha-atividade.scss',
})
export class FichaAtividade {
  /** Parâmetro `:id` da rota de leitura. */
  readonly id = input<string>();

  private readonly api = inject(AtividadesApi);
  private readonly avisos = inject(MatSnackBar);
  private readonly router = inject(Router);

  protected readonly coletivos = TIPOS_ATIVIDADE.filter((t) => !t.individual);
  protected readonly individuais = TIPOS_ATIVIDADE.filter((t) => t.individual);
  protected readonly rotulosTipo = ROTULOS_TIPO_ATIVIDADE;
  protected readonly funcoes = ROTULOS_FUNCAO;
  protected readonly hoje = hoje();
  protected readonly data = lerDataIso;
  protected readonly podeRegistrar = inject(Sessao).temPermissao('ATIVIDADES_REGISTRAR');
  protected readonly atividade = signal<Atividade | null>(null);
  protected readonly usuarios = signal<Participante[]>([]);
  protected readonly profissionais = signal<Profissional[]>([]);
  protected readonly editando = signal(false);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly nova = computed(() => !this.id());

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    tipo: ['' as TipoAtividade | '', Validators.required],
    data: [hoje() as Date | null, Validators.required],
    titulo: ['', [Validators.required, Validators.maxLength(200)]],
    descricao: ['', [Validators.required, Validators.maxLength(10000)]],
    participantes: [[] as string[], Validators.required],
    profissionais: [[] as string[], Validators.required],
  });

  private readonly tipoEscolhido = toSignal(this.formulario.controls.tipo.valueChanges, {
    initialValue: this.formulario.controls.tipo.value,
  });
  /** Tipo escolhido no formulário, ou `undefined` antes da escolha. */
  protected readonly tipo = computed(() => descricaoTipo(this.tipoEscolhido()));
  /** A atividade aberta é atendimento ou estudo de caso. */
  protected readonly sigilosa = computed(() => {
    const atual = this.atividade();
    return !!atual && !!descricaoTipo(atual.tipo)?.individual;
  });

  constructor() {
    toObservable(this.id)
      .pipe(
        filter((id): id is string => !!id),
        switchMap((id) => this.api.atividade(id)),
      )
      .subscribe({
        next: (atividade) => this.mostrar(atividade),
        error: (erro: unknown) =>
          this.erro.set(mensagemDeErro(erro, 'Não foi possível abrir a atividade.')),
      });
    if (this.podeRegistrar) {
      this.api.opcoes().subscribe({
        next: ({ usuarios, profissionais, sugeridoId }) => {
          this.usuarios.set(usuarios);
          this.profissionais.set(profissionais);
          const controle = this.formulario.controls.profissionais;
          if (sugeridoId && controle.value.length === 0 && !this.id()) {
            controle.setValue([sugeridoId]);
          }
        },
        error: () => {
          this.usuarios.set([]);
          this.profissionais.set([]);
        },
      });
    }
  }

  /** Usuários para escolher, com os da atividade mesmo que já tenham sido desligados. */
  protected readonly opcoesUsuarios = computed(() => {
    const lista = this.usuarios();
    const faltando = (this.atividade()?.participantes ?? []).filter(
      (p) => !lista.some((u) => u.pessoaId === p.pessoaId),
    );
    return [...faltando, ...lista];
  });

  /** Profissionais para escolher, com os da atividade mesmo que tenham ficado inativos. */
  protected readonly opcoesProfissionais = computed(() => {
    const lista = this.profissionais();
    const faltando = (this.atividade()?.profissionais ?? []).filter(
      (p) => !lista.some((q) => q.id === p.id),
    );
    return [...faltando, ...lista];
  });

  protected corrigir(): void {
    this.editando.set(true);
  }

  protected desistir(): void {
    const atividade = this.atividade();
    if (atividade) {
      this.mostrar(atividade);
    }
  }

  protected salvar(): void {
    const valor = this.formulario.getRawValue();
    const problema = this.problemaDoTipo(valor.participantes.length, valor.profissionais.length);
    if (this.formulario.invalid || problema || this.enviando()) {
      this.formulario.markAllAsTouched();
      this.erro.set(problema);
      return;
    }
    const dados: DadosAtividade = {
      tipo: valor.tipo as TipoAtividade,
      data: dataIso(valor.data!),
      titulo: valor.titulo.trim(),
      descricao: valor.descricao.trim(),
      participantes: valor.participantes,
      profissionais: valor.profissionais,
    };
    const id = this.id();
    const operacao = id ? this.api.corrigir(id, dados) : this.api.registrar(dados);
    this.enviando.set(true);
    this.erro.set(null);
    operacao.subscribe({
      next: (atividade) => {
        this.enviando.set(false);
        this.avisos.open(id ? 'Atividade corrigida.' : 'Atividade registrada.', 'Fechar', {
          duration: 4000,
        });
        if (id) {
          this.mostrar(atividade);
        } else {
          void this.router.navigate(['/atividades', atividade.id]);
        }
      },
      error: (erro: unknown) => {
        this.enviando.set(false);
        this.erro.set(mensagemDeErro(erro, 'Não foi possível salvar a atividade.'));
      },
    });
  }

  /** Regra do tipo que a escolha não cumpre, na mesma redação do backend. */
  private problemaDoTipo(participantes: number, profissionais: number): string | null {
    const tipo = this.tipo();
    if (!tipo) {
      return null;
    }
    if (tipo.individual && participantes > 1) {
      return 'Atendimentos e estudos de caso são de um só usuário atendido.';
    }
    if (profissionais > 0 && profissionais < tipo.minimoProfissionais) {
      return `O atendimento multiprofissional precisa de pelo menos ${tipo.minimoProfissionais} profissionais.`;
    }
    return null;
  }

  private mostrar(atividade: Atividade): void {
    this.atividade.set(atividade);
    this.editando.set(false);
    this.erro.set(null);
    this.formulario.setValue({
      tipo: atividade.tipo,
      data: lerDataIso(atividade.data),
      titulo: atividade.titulo,
      descricao: atividade.descricao,
      participantes: atividade.participantes.map((p) => p.pessoaId),
      profissionais: atividade.profissionais.map((p) => p.id),
    });
  }
}
