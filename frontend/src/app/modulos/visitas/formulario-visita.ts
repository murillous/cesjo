import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';

import { mensagemDeErro } from '../../acesso/problema';
import { hoje } from '../lista-espera/datas';
import { ROTULOS_FUNCAO } from '../colaboradores/rotulos';
import { dataIso } from './datas-visita';
import { Candidato, VisitasApi } from './visitas-api';

/** Hora `HH:mm` de 00:00 a 23:59. */
const HORA_VALIDA = /^([01]\d|2[0-3]):[0-5]\d$/;

/** Sem acento e em minúsculas, para a busca do candidato ignorar acentos. */
function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
}

/**
 * Agendamento de visita domiciliar (`/visitas/nova`, issue #34): candidato da lista de espera, data,
 * hora e um ou mais profissionais ativos.
 */
@Component({
  selector: 'app-formulario-visita',
  imports: [
    MatAutocompleteModule,
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
  templateUrl: './formulario-visita.html',
  styleUrl: './formulario-visita.scss',
})
export class FormularioVisita {
  private readonly api = inject(VisitasApi);
  private readonly avisos = inject(MatSnackBar);
  private readonly router = inject(Router);

  protected readonly hoje = hoje();
  protected readonly funcoes = ROTULOS_FUNCAO;
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly candidatos = toSignal(this.api.candidatos().pipe(catchError(() => of([]))), {
    initialValue: [] as Candidato[],
  });
  protected readonly profissionais = toSignal(
    this.api.profissionais().pipe(catchError(() => of([]))),
    { initialValue: [] },
  );

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    candidato: [null as Candidato | string | null, Validators.required],
    data: [null as Date | null, Validators.required],
    hora: ['', [Validators.required, Validators.pattern(HORA_VALIDA)]],
    profissionais: [[] as string[], Validators.required],
  });

  private readonly textoBusca = toSignal(this.formulario.controls.candidato.valueChanges, {
    initialValue: null,
  });

  protected readonly sugestoes = computed(() => {
    const busca = this.textoBusca();
    const termo = typeof busca === 'string' ? normalizar(busca.trim()) : '';
    const lista = this.candidatos();
    return termo ? lista.filter((c) => normalizar(c.nomeCompleto).includes(termo)) : lista;
  });

  protected nomeDoCandidato(candidato: Candidato | string | null): string {
    return typeof candidato === 'string' ? candidato : (candidato?.nomeCompleto ?? '');
  }

  /** Só vale um candidato escolhido na lista, não o texto digitado. */
  protected candidatoEscolhido(): Candidato | null {
    const valor = this.formulario.controls.candidato.value;
    return valor && typeof valor !== 'string' ? valor : null;
  }

  protected salvar(): void {
    const candidato = this.candidatoEscolhido();
    if (!candidato) {
      this.formulario.controls.candidato.setErrors({ naoEscolhido: true });
    }
    if (this.formulario.invalid || !candidato || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const { data, hora, profissionais } = this.formulario.getRawValue();
    const dia = dataIso(data!);
    this.enviando.set(true);
    this.erro.set(null);
    this.api
      .agendar({ pessoaId: candidato.pessoaId, dataHora: `${dia}T${hora}`, profissionais })
      .subscribe({
        next: () => {
          this.enviando.set(false);
          this.avisos.open('Visita agendada.', 'Fechar', { duration: 4000 });
          void this.router.navigate(['/visitas'], { queryParams: { semana: dia } });
        },
        error: (erro: unknown) => {
          this.enviando.set(false);
          this.erro.set(mensagemDeErro(erro, 'Não foi possível agendar a visita.'));
        },
      });
  }
}
