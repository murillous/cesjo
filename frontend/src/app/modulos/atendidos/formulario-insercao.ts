import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core';
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
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatRadioModule } from '@angular/material/radio';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';

import { mensagemDeErro } from '../../acesso/problema';
import { dataIso, hoje } from '../lista-espera/datas';
import { SITUACOES_ATIVAS, TIPOS_DEFICIENCIA } from '../lista-espera/rotulos';
import { AtendidosApi, CandidatoInsercao } from './atendidos-api';
import { Turno, TURNOS } from './rotulos';

/** Valor do grupo de cuidador que pede o nome de um cuidador novo. */
export const NOVO_CUIDADOR = 'novo';

/**
 * Inserção de um candidato da fila no serviço (`/atendidos/inserir/:inscricaoId`, RF007, ADR 0080):
 * data de inserção, turno e cuidador familiar responsável.
 */
@Component({
  selector: 'app-formulario-insercao',
  imports: [
    DatePipe,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatRadioModule,
    ReactiveFormsModule,
    RouterLink,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './formulario-insercao.html',
  styleUrl: './formulario-insercao.scss',
})
export class FormularioInsercao implements OnInit {
  /** Parâmetro da rota. */
  readonly inscricaoId = input.required<string>();

  private readonly api = inject(AtendidosApi);
  private readonly avisos = inject(MatSnackBar);
  private readonly router = inject(Router);

  protected readonly turnos = TURNOS;
  protected readonly novoCuidador = NOVO_CUIDADOR;
  protected readonly hoje = hoje();
  protected readonly candidato = signal<CandidatoInsercao | null>(null);
  protected readonly carregando = signal(true);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    dataInsercao: new FormControl<Date | null>(hoje(), Validators.required),
    turno: ['' as Turno | '', Validators.required],
    // PENDENTE-CESJO(P7): um cuidador, escolhido entre os responsáveis ou cadastrado na hora (ADR 0080).
    cuidador: ['', Validators.required],
    nomeNovoCuidador: ['', Validators.maxLength(150)],
  });

  ngOnInit(): void {
    this.api.candidato(this.inscricaoId()).subscribe({
      next: (candidato) => {
        this.candidato.set(candidato);
        this.carregando.set(false);
        const principal = candidato.responsaveis.find((r) => r.principal);
        if (principal) {
          this.formulario.controls.cuidador.setValue(principal.id);
        }
      },
      error: (erro: unknown) => {
        this.carregando.set(false);
        this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar o candidato.'));
      },
    });
  }

  /** Aguardando ou em avaliação: pode ser inserido (ADR 0136). */
  protected naFila(candidato: CandidatoInsercao): boolean {
    return SITUACOES_ATIVAS.has(candidato.situacao);
  }

  protected rotuloTipo(candidato: CandidatoInsercao): string {
    return TIPOS_DEFICIENCIA.find((t) => t.valor === candidato.tipoDeficiencia)?.rotulo ?? '';
  }

  protected querNovoCuidador(): boolean {
    return this.formulario.controls.cuidador.value === NOVO_CUIDADOR;
  }

  protected salvar(): void {
    const dados = this.formulario.getRawValue();
    const novo = this.querNovoCuidador();
    const nomeNovo = dados.nomeNovoCuidador.trim();
    if (this.formulario.invalid || (novo && !nomeNovo) || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.erro.set(null);
    this.api
      .inserir({
        inscricaoId: this.inscricaoId(),
        dataInsercao: dataIso(dados.dataInsercao!),
        turno: dados.turno as Turno,
        cuidadorResponsavelId: novo ? null : dados.cuidador,
        nomeNovoCuidador: novo ? nomeNovo : null,
      })
      .subscribe({
        next: (insercao) => {
          this.enviando.set(false);
          this.avisos.open(`${insercao.nomeCompleto} foi inserido no serviço.`, 'Fechar', {
            duration: 5000,
          });
          void this.router.navigate(['/atendidos', insercao.id]);
        },
        error: (erro: unknown) => {
          this.enviando.set(false);
          this.erro.set(mensagemDeErro(erro, 'Não foi possível inserir no serviço.'));
        },
      });
  }
}
