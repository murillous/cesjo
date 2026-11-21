import { DatePipe, TitleCasePipe } from '@angular/common';
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
import { filter, Observable, switchMap } from 'rxjs';

import { mensagemDeErro } from '../../acesso/problema';
import { Sessao } from '../../acesso/sessao';
import { ROTULOS_FUNCAO } from '../colaboradores/rotulos';
import { hoje } from '../lista-espera/datas';
import { dataIso, diaDe, horaDe, lerDataIso } from './datas-visita';
import { ROTULOS_STATUS } from './rotulos';
import { Profissional, Visita, VisitasApi } from './visitas-api';

type Acao = 'realizar' | 'cancelar' | 'reagendar';

const HORA_VALIDA = /^([01]\d|2[0-3]):[0-5]\d$/;

/**
 * Ficha de uma visita domiciliar (`/visitas/:id`), com as ações de registrar a realização, cancelar
 * e reagendar enquanto ela está agendada (issue #35, ADR 0075). Uma ação aberta por vez.
 */
@Component({
  selector: 'app-ficha-visita',
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
    TitleCasePipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './ficha-visita.html',
  styleUrl: './ficha-visita.scss',
})
export class FichaVisita {
  /** Parâmetro `:id` da rota. */
  readonly id = input.required<string>();

  private readonly api = inject(VisitasApi);
  private readonly avisos = inject(MatSnackBar);
  private readonly router = inject(Router);
  private readonly sessao = inject(Sessao);
  private readonly campos = inject(NonNullableFormBuilder);

  protected readonly hoje = hoje();
  protected readonly funcoes = ROTULOS_FUNCAO;
  protected readonly rotulos = ROTULOS_STATUS;
  protected readonly podeRegistrar = this.sessao.temPermissao('VISITAS_REGISTRAR');
  protected readonly podeAgendar = this.sessao.temPermissao('VISITAS_AGENDAR');
  protected readonly visita = signal<Visita | null>(null);
  protected readonly erro = signal<string | null>(null);
  protected readonly acao = signal<Acao | null>(null);
  protected readonly enviando = signal(false);
  protected readonly observacoes = signal<string | null | undefined>(undefined);
  protected readonly profissionais = signal<Profissional[]>([]);

  protected readonly data = computed(() => {
    const visita = this.visita();
    return visita ? lerDataIso(diaDe(visita.dataHora)) : null;
  });
  protected readonly hora = computed(() => {
    const visita = this.visita();
    return visita ? horaDe(visita.dataHora) : '';
  });
  /** Dia da visita, para voltar à semana certa da agenda. */
  protected readonly semana = computed(() => {
    const visita = this.visita();
    return visita ? diaDe(visita.dataHora) : null;
  });
  protected readonly agendada = computed(() => this.visita()?.status === 'AGENDADA');
  /** Visita de dia futuro ainda não aconteceu (ADR 0073). */
  protected readonly jaPodeRealizar = computed(() => {
    const visita = this.visita();
    return !!visita && diaDe(visita.dataHora) <= dataIso(new Date());
  });

  protected readonly formRealizar = this.campos.group({
    observacoes: ['', Validators.maxLength(5000)],
  });
  protected readonly formCancelar = this.campos.group({
    motivo: ['', [Validators.required, Validators.maxLength(500)]],
  });
  protected readonly formReagendar = this.campos.group({
    data: [null as Date | null, Validators.required],
    hora: ['', [Validators.required, Validators.pattern(HORA_VALIDA)]],
    profissionais: [[] as string[], Validators.required],
    motivo: ['', [Validators.required, Validators.maxLength(500)]],
  });

  constructor() {
    toObservable(this.id)
      .pipe(
        filter((id) => !!id),
        switchMap((id) => {
          this.acao.set(null);
          this.observacoes.set(undefined);
          return this.api.obter(id);
        }),
      )
      .subscribe({
        next: (visita) => this.mostrar(visita),
        error: (erro: unknown) =>
          this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar a visita.')),
      });
  }

  protected abrir(acao: Acao | null): void {
    this.acao.set(acao);
    this.erro.set(null);
    const visita = this.visita();
    if (acao === 'reagendar' && visita) {
      this.formReagendar.reset({
        data: null,
        hora: horaDe(visita.dataHora),
        profissionais: visita.profissionais.map((p) => p.id),
        motivo: '',
      });
      this.api.profissionais().subscribe({
        next: (lista) => this.profissionais.set(lista),
        error: () => this.profissionais.set([]),
      });
    }
  }

  protected realizar(visita: Visita): void {
    if (this.formRealizar.invalid) {
      this.formRealizar.markAllAsTouched();
      return;
    }
    const texto = this.formRealizar.getRawValue().observacoes.trim();
    this.executar(this.api.realizar(visita.id, texto || null), (atualizada) => {
      this.mostrar(atualizada);
      this.formRealizar.reset();
      this.avisar('Realização registrada.');
    });
  }

  protected cancelar(visita: Visita): void {
    if (this.formCancelar.invalid) {
      this.formCancelar.markAllAsTouched();
      return;
    }
    this.executar(this.api.cancelar(visita.id, this.formCancelar.getRawValue().motivo), (atual) => {
      this.mostrar(atual);
      this.formCancelar.reset();
      this.avisar('Visita cancelada.');
    });
  }

  protected reagendar(visita: Visita): void {
    if (this.formReagendar.invalid) {
      this.formReagendar.markAllAsTouched();
      return;
    }
    const { data, hora, profissionais, motivo } = this.formReagendar.getRawValue();
    const dados = { dataHora: `${dataIso(data!)}T${hora}`, profissionais, motivo };
    this.executar(this.api.reagendar(visita.id, dados), (nova) => {
      this.avisar('Visita reagendada.');
      void this.router.navigate(['/visitas', nova.id]);
    });
  }

  /** Lê as observações sob demanda: cada leitura vai para o log de auditoria. */
  protected verObservacoes(visita: Visita): void {
    this.api.observacoes(visita.id).subscribe({
      next: ({ observacoes }) => this.observacoes.set(observacoes),
      error: (erro: unknown) =>
        this.erro.set(mensagemDeErro(erro, 'Não foi possível ler as observações.')),
    });
  }

  private mostrar(visita: Visita): void {
    this.visita.set(visita);
    this.erro.set(null);
    this.acao.set(null);
  }

  private executar(operacao: Observable<Visita>, sucesso: (visita: Visita) => void): void {
    if (this.enviando()) {
      return;
    }
    this.enviando.set(true);
    this.erro.set(null);
    operacao.subscribe({
      next: (visita) => {
        this.enviando.set(false);
        sucesso(visita);
      },
      error: (erro: unknown) => {
        this.enviando.set(false);
        this.erro.set(mensagemDeErro(erro, 'Não foi possível concluir a operação.'));
      },
    });
  }

  private avisar(mensagem: string): void {
    this.avisos.open(mensagem, 'Fechar', { duration: 4000 });
  }
}
