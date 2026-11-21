import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
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
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';

import { mensagemDeErro } from '../../acesso/problema';
import { dataIso, hoje } from '../lista-espera/datas';
import { AtendidosApi } from './atendidos-api';
import {
  AMBITOS_VIOLACAO,
  AmbitoViolacao,
  exigeAmbito,
  TIPOS_VIOLACAO,
  TipoViolacao,
} from './rotulos';

/**
 * Registro de violação de direitos (`/atendidos/:id/violacoes/nova`, RF009, ADR 0112): categoria,
 * âmbito quando a categoria pede, data de identificação e descrição, que fica cifrada.
 */
@Component({
  selector: 'app-formulario-violacao',
  imports: [
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatRadioModule,
    ReactiveFormsModule,
    RouterLink,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './formulario-violacao.html',
  styleUrl: './formulario-violacao.scss',
})
export class FormularioViolacao {
  /** Parâmetro da rota: id da inserção. */
  readonly id = input.required<string>();

  private readonly api = inject(AtendidosApi);
  private readonly avisos = inject(MatSnackBar);
  private readonly router = inject(Router);

  protected readonly tipos = TIPOS_VIOLACAO;
  protected readonly ambitos = AMBITOS_VIOLACAO;
  protected readonly hoje = hoje();
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    tipo: ['' as TipoViolacao | '', Validators.required],
    ambito: ['' as AmbitoViolacao | ''],
    dataIdentificacao: new FormControl<Date | null>(hoje(), Validators.required),
    descricao: ['', [Validators.required, Validators.maxLength(4000)]],
  });

  protected pedeAmbito(): boolean {
    return exigeAmbito(this.formulario.controls.tipo.value);
  }

  protected faltaAmbito(): boolean {
    return this.pedeAmbito() && !this.formulario.controls.ambito.value;
  }

  protected salvar(): void {
    const dados = this.formulario.getRawValue();
    const descricao = dados.descricao.trim();
    if (this.formulario.invalid || !descricao || this.faltaAmbito() || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.erro.set(null);
    this.api
      .registrarViolacao(this.id(), {
        tipo: dados.tipo as TipoViolacao,
        ambito: this.pedeAmbito() ? (dados.ambito as AmbitoViolacao) : null,
        dataIdentificacao: dataIso(dados.dataIdentificacao!),
        descricao,
      })
      .subscribe({
        next: () => {
          this.enviando.set(false);
          this.avisos.open('Violação de direitos registrada.', 'Fechar', { duration: 5000 });
          void this.router.navigate(['/atendidos', this.id()]);
        },
        error: (erro: unknown) => {
          this.enviando.set(false);
          this.erro.set(mensagemDeErro(erro, 'Não foi possível registrar a violação.'));
        },
      });
  }
}
