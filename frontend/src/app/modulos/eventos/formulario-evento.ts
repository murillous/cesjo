import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import {
  FormControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';

import { mensagemDeErro } from '../../acesso/problema';
import { Sessao } from '../../acesso/sessao';
import { dataDeIso, dataIso, hoje } from '../lista-espera/datas';
import { ParceriasApi } from '../parcerias/parcerias-api';
import { DadosEvento, EventoInstitucional, EventosApi } from './eventos-api';
import { DirecaoEvento, TIPOS_EVENTO, TipoEvento } from './rotulos';

/** Quantos parceiros ativos a escolha carrega; o Centro-Dia tem poucas dezenas. */
const LIMITE_PARCEIROS = 200;

/**
 * Registro (`/eventos/novo`) e ficha (`/eventos/:id`) de reunião ou evento institucional (RF012,
 * ADR 0175 e ADR 0177). A indicação de realizado, recebido ou participado só aparece para visitas e
 * eventos. Sem `EVENTOS_REGISTRAR`, só mostra os dados.
 */
@Component({
  selector: 'app-formulario-evento',
  imports: [
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatRadioModule,
    MatSelectModule,
    ReactiveFormsModule,
    RouterLink,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './formulario-evento.html',
  styleUrl: './formulario-evento.scss',
})
export class FormularioEvento {
  /** Parâmetro `:id` da rota; vazio no registro. */
  readonly id = input<string>();

  private readonly api = inject(EventosApi);
  private readonly parceriasApi = inject(ParceriasApi);
  private readonly avisos = inject(MatSnackBar);
  private readonly router = inject(Router);
  private readonly sessao = inject(Sessao);

  protected readonly tipos = TIPOS_EVENTO;
  protected readonly hoje = hoje();
  protected readonly podeRegistrar = this.sessao.temPermissao('EVENTOS_REGISTRAR');
  protected readonly evento = signal<EventoInstitucional | null>(null);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly edicao = computed(() => !!this.id());
  private readonly parceirosAtivos = signal<{ id: string; nome: string }[]>([]);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    tipo: ['' as TipoEvento | '', Validators.required],
    direcao: ['' as DirecaoEvento | ''],
    data: new FormControl<Date | null>(hoje(), Validators.required),
    descricao: ['', [Validators.required, Validators.maxLength(4000)]],
    participantes: ['', Validators.maxLength(2000)],
    parceiroId: [''],
  });

  private readonly tipo = toSignal(this.formulario.controls.tipo.valueChanges, {
    initialValue: this.formulario.controls.tipo.value,
  });

  /** Indicações aceitas pelo tipo escolhido; vazio esconde a escolha. */
  protected readonly direcoes = computed(
    () => TIPOS_EVENTO.find((t) => t.valor === this.tipo())?.direcoes ?? [],
  );

  /** Parceiros que podem ser escolhidos, sempre com o já ligado, mesmo inativo. */
  protected readonly parceiros = computed(() => {
    const lista = this.parceirosAtivos();
    const ligado = this.evento()?.parceiro;
    return ligado && !lista.some((p) => p.id === ligado.id) ? [ligado, ...lista] : lista;
  });

  protected readonly podeEscolherParceiro = this.sessao.temPermissao('PARCERIAS_LER');

  constructor() {
    if (!this.podeRegistrar) {
      this.formulario.disable();
    }
    if (this.podeRegistrar && this.podeEscolherParceiro) {
      this.parceriasApi.listar(true, 0, LIMITE_PARCEIROS).subscribe({
        next: (pagina) =>
          this.parceirosAtivos.set(pagina.content.map(({ id, nome }) => ({ id, nome }))),
        error: () => this.parceirosAtivos.set([]),
      });
    }
    // Trocar para um tipo sem a indicação escolhida limpa a indicação.
    this.formulario.controls.tipo.valueChanges.subscribe(() => {
      const direcao = this.formulario.controls.direcao;
      if (!this.direcoes().some((d) => d.valor === direcao.value)) {
        direcao.setValue('');
      }
    });
    toObservable(this.id)
      .pipe(
        filter((id): id is string => !!id),
        switchMap((id) => this.api.obter(id)),
      )
      .subscribe({
        next: (evento) => this.mostrar(evento),
        error: (erro: unknown) =>
          this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar o registro.')),
      });
  }

  protected faltaDirecao(): boolean {
    return this.direcoes().length > 0 && !this.formulario.controls.direcao.value;
  }

  protected salvar(): void {
    if (this.formulario.invalid || this.faltaDirecao() || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const valor = this.formulario.getRawValue();
    const dados: DadosEvento = {
      tipo: valor.tipo as TipoEvento,
      direcao: valor.direcao || null,
      data: dataIso(valor.data!),
      descricao: valor.descricao.trim(),
      participantes: valor.participantes.trim() || null,
      parceiroId: valor.parceiroId || null,
    };
    const id = this.id();
    this.enviando.set(true);
    this.erro.set(null);
    (id ? this.api.editar(id, dados) : this.api.registrar(dados)).subscribe({
      next: (salvo) => {
        this.enviando.set(false);
        this.avisos.open(id ? 'Registro salvo.' : 'Reunião ou evento registrado.', 'Fechar', {
          duration: 4000,
        });
        if (id) {
          this.mostrar(salvo);
        } else {
          void this.router.navigate(['/eventos', salvo.id]);
        }
      },
      error: (erro: unknown) => {
        this.enviando.set(false);
        this.erro.set(mensagemDeErro(erro, 'Não foi possível salvar o registro.'));
      },
    });
  }

  private mostrar(evento: EventoInstitucional): void {
    this.evento.set(evento);
    this.formulario.setValue({
      tipo: evento.tipo,
      direcao: evento.direcao ?? '',
      data: dataDeIso(evento.data),
      descricao: evento.descricao,
      participantes: evento.participantes ?? '',
      parceiroId: evento.parceiro?.id ?? '',
    });
  }
}
