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
import { DadosDoacao, Doacao, DoacoesApi } from './doacoes-api';
import { DIRECOES_DOACAO, DirecaoDoacao, TIPOS_DOACAO, TipoDoacao } from './rotulos';

/** Valor em reais como a pessoa digita: até 10 dígitos e 2 casas, com vírgula ou ponto. */
const FORMATO_VALOR = /^\d{1,10}([.,]\d{1,2})?$/;

/** "150,5" vira 150.5; vazio vira `null`. */
function valorEmNumero(texto: string): number | null {
  const limpo = texto.trim();
  return limpo ? Number(limpo.replace(',', '.')) : null;
}

/** 150.5 vira "150,50" para o campo. */
function valorEmTexto(valor: number | null): string {
  return valor === null ? '' : valor.toFixed(2).replace('.', ',');
}

/**
 * Registro (`/doacoes/nova`) e ficha (`/doacoes/:id`) de doação (RF015, ADR 0178 e ADR 0180). O campo
 * de valor só aparece para doação em dinheiro. Sem `DOACOES_REGISTRAR`, só mostra os dados.
 */
@Component({
  selector: 'app-formulario-doacao',
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
  templateUrl: './formulario-doacao.html',
  styleUrl: './formulario-doacao.scss',
})
export class FormularioDoacao {
  /** Parâmetro `:id` da rota; vazio no registro. */
  readonly id = input<string>();

  private readonly api = inject(DoacoesApi);
  private readonly avisos = inject(MatSnackBar);
  private readonly router = inject(Router);

  protected readonly direcoes = DIRECOES_DOACAO;
  protected readonly tipos = TIPOS_DOACAO;
  protected readonly hoje = hoje();
  protected readonly podeRegistrar = inject(Sessao).temPermissao('DOACOES_REGISTRAR');
  protected readonly doacao = signal<Doacao | null>(null);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly edicao = computed(() => !!this.id());

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    direcao: ['' as DirecaoDoacao | '', Validators.required],
    data: new FormControl<Date | null>(hoje(), Validators.required),
    contraparte: ['', [Validators.required, Validators.maxLength(200)]],
    tipo: ['' as TipoDoacao | '', Validators.required],
    descricao: ['', [Validators.required, Validators.maxLength(2000)]],
    quantidade: ['', Validators.maxLength(100)],
    valor: ['', Validators.pattern(FORMATO_VALOR)],
  });

  private readonly tipo = toSignal(this.formulario.controls.tipo.valueChanges, {
    initialValue: this.formulario.controls.tipo.value,
  });

  protected readonly emDinheiro = computed(() => this.tipo() === 'DINHEIRO');

  constructor() {
    if (!this.podeRegistrar) {
      this.formulario.disable();
    }
    toObservable(this.id)
      .pipe(
        filter((id): id is string => !!id),
        switchMap((id) => this.api.obter(id)),
      )
      .subscribe({
        next: (doacao) => this.mostrar(doacao),
        error: (erro: unknown) =>
          this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar a doação.')),
      });
  }

  /** Valor zerado não vale: o campo fica vazio quando não se sabe o valor. */
  protected valorZerado(): boolean {
    return this.emDinheiro() && valorEmNumero(this.formulario.controls.valor.value) === 0;
  }

  protected salvar(): void {
    if (this.formulario.invalid || this.valorZerado() || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const valor = this.formulario.getRawValue();
    const dados: DadosDoacao = {
      direcao: valor.direcao as DirecaoDoacao,
      data: dataIso(valor.data!),
      contraparte: valor.contraparte.trim(),
      tipo: valor.tipo as TipoDoacao,
      descricao: valor.descricao.trim(),
      // Cada tipo usa só um dos dois campos: dinheiro tem valor, o resto tem quantidade.
      quantidade: this.emDinheiro() ? null : valor.quantidade.trim() || null,
      valor: this.emDinheiro() ? valorEmNumero(valor.valor) : null,
    };
    const id = this.id();
    this.enviando.set(true);
    this.erro.set(null);
    (id ? this.api.editar(id, dados) : this.api.registrar(dados)).subscribe({
      next: (salva) => {
        this.enviando.set(false);
        this.avisos.open(id ? 'Doação salva.' : 'Doação registrada.', 'Fechar', {
          duration: 4000,
        });
        if (id) {
          this.mostrar(salva);
        } else {
          void this.router.navigate(['/doacoes', salva.id]);
        }
      },
      error: (erro: unknown) => {
        this.enviando.set(false);
        this.erro.set(mensagemDeErro(erro, 'Não foi possível salvar a doação.'));
      },
    });
  }

  private mostrar(doacao: Doacao): void {
    this.doacao.set(doacao);
    this.formulario.setValue({
      direcao: doacao.direcao,
      data: dataDeIso(doacao.data),
      contraparte: doacao.contraparte,
      tipo: doacao.tipo,
      descricao: doacao.descricao,
      quantidade: doacao.quantidade ?? '',
      valor: valorEmTexto(doacao.valor),
    });
  }
}
