import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';

import { mensagemDeErro } from '../../acesso/problema';
import { Sessao } from '../../acesso/sessao';
import { salvarNoNavegador } from '../../compartilhado/arquivos/arquivos-api';
import {
  ACEITE_INPUT,
  formatarTamanho,
  TAMANHO_MAXIMO_BYTES,
  validarArquivo,
} from '../../compartilhado/arquivos/regras-arquivo';
import { hoje } from '../lista-espera/datas';
import { dataIso, lerDataIso } from '../visitas/datas-visita';
import { Comunicacao, ComunicacoesApi, DadosComunicacao } from './comunicacoes-api';
import {
  DirecaoComunicacao,
  rotuloCompleto,
  TIPO_POR_VALOR,
  TipoComunicacao,
  TIPOS_COMUNICACAO,
} from './rotulos';

/**
 * Registro (`/comunicacoes/nova`), leitura e correção (`/comunicacoes/:id`) de um contato ou
 * comunicação do RF011 (ADR 0163 e ADR 0165). O tipo se escolhe por cartões com ícone, porque muda o
 * rótulo do campo "pessoa ou instituição" e, nos ofícios, declarações e encaminhamentos, pede a
 * direção. O anexo é opcional: no registro vai junto, e na ficha pode ser trocado ou removido.
 */
@Component({
  selector: 'app-ficha-comunicacao',
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
  templateUrl: './ficha-comunicacao.html',
  styleUrl: './ficha-comunicacao.scss',
})
export class FichaComunicacao {
  /** Parâmetro `:id` da rota de leitura. */
  readonly id = input<string>();

  private readonly api = inject(ComunicacoesApi);
  private readonly avisos = inject(MatSnackBar);
  private readonly router = inject(Router);

  protected readonly tipos = TIPOS_COMUNICACAO;
  protected readonly tipoPorValor = TIPO_POR_VALOR;
  protected readonly rotulo = rotuloCompleto;
  protected readonly tamanho = formatarTamanho;
  protected readonly aceite = ACEITE_INPUT;
  protected readonly dicaArquivo = `PDF, JPG ou PNG, até ${formatarTamanho(TAMANHO_MAXIMO_BYTES)}.`;
  protected readonly hoje = hoje();
  protected readonly data = lerDataIso;
  protected readonly podeRegistrar = inject(Sessao).temPermissao('COMUNICACOES_REGISTRAR');
  protected readonly comunicacao = signal<Comunicacao | null>(null);
  protected readonly editando = signal(false);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly erroArquivo = signal<string | null>(null);
  /** Arquivo escolhido no registro, enviado logo depois de registrar. */
  protected readonly arquivoNovo = signal<File | null>(null);
  protected readonly nova = computed(() => !this.id());

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    tipo: ['' as TipoComunicacao | '', Validators.required],
    direcao: ['' as DirecaoComunicacao | ''],
    data: [hoje() as Date | null, Validators.required],
    contraparte: ['', [Validators.required, Validators.maxLength(200)]],
    assunto: ['', [Validators.required, Validators.maxLength(200)]],
    descricao: ['', Validators.maxLength(10000)],
  });

  private readonly tipoEscolhido = toSignal(this.formulario.controls.tipo.valueChanges, {
    initialValue: this.formulario.controls.tipo.value,
  });

  /** Opções do tipo escolhido, ou `null` antes da escolha. */
  protected readonly opcaoTipo = computed(() => {
    const tipo = this.tipoEscolhido();
    return tipo ? TIPO_POR_VALOR[tipo] : null;
  });

  constructor() {
    toObservable(this.id)
      .pipe(
        filter((id): id is string => !!id),
        switchMap((id) => this.api.comunicacao(id)),
      )
      .subscribe({
        next: (comunicacao) => this.mostrar(comunicacao),
        error: (erro: unknown) =>
          this.erro.set(mensagemDeErro(erro, 'Não foi possível abrir o registro.')),
      });
    // A direção só vale nos tipos que a têm; nos outros, fica vazia e sem validação.
    this.formulario.controls.tipo.valueChanges.subscribe((tipo) => {
      const direcao = this.formulario.controls.direcao;
      if (tipo && TIPO_POR_VALOR[tipo].direcoes) {
        direcao.setValidators(Validators.required);
      } else {
        direcao.clearValidators();
        direcao.setValue('');
      }
      direcao.updateValueAndValidity();
    });
  }

  protected corrigir(): void {
    this.editando.set(true);
  }

  protected desistir(): void {
    const comunicacao = this.comunicacao();
    if (comunicacao) {
      this.mostrar(comunicacao);
    }
  }

  /** Arquivo escolhido no formulário de registro: conferido aqui, enviado depois de registrar. */
  protected async escolherArquivo(evento: Event): Promise<void> {
    const arquivo = this.arquivoDoEvento(evento);
    this.erroArquivo.set(null);
    this.arquivoNovo.set(null);
    if (!arquivo) {
      return;
    }
    const problema = await validarArquivo(arquivo);
    if (problema) {
      this.erroArquivo.set(problema);
      return;
    }
    this.arquivoNovo.set(arquivo);
  }

  protected tirarArquivoNovo(): void {
    this.arquivoNovo.set(null);
    this.erroArquivo.set(null);
  }

  /** Anexa ou troca o anexo de um registro já gravado. */
  protected async trocarAnexo(evento: Event): Promise<void> {
    const arquivo = this.arquivoDoEvento(evento);
    const id = this.id();
    this.erroArquivo.set(null);
    if (!arquivo || !id) {
      return;
    }
    const problema = await validarArquivo(arquivo);
    if (problema) {
      this.erroArquivo.set(problema);
      return;
    }
    this.enviando.set(true);
    this.api.anexar(id, arquivo).subscribe({
      next: (comunicacao) => {
        this.enviando.set(false);
        this.comunicacao.set(comunicacao);
        this.avisos.open('Anexo salvo.', 'Fechar', { duration: 4000 });
      },
      error: (erro: unknown) => {
        this.enviando.set(false);
        this.erroArquivo.set(mensagemDeErro(erro, 'Não foi possível salvar o anexo.'));
      },
    });
  }

  protected removerAnexo(): void {
    const id = this.id();
    if (!id || this.enviando()) {
      return;
    }
    this.enviando.set(true);
    this.api.removerAnexo(id).subscribe({
      next: (comunicacao) => {
        this.enviando.set(false);
        this.comunicacao.set(comunicacao);
        this.avisos.open('Anexo removido.', 'Fechar', { duration: 4000 });
      },
      error: (erro: unknown) => {
        this.enviando.set(false);
        this.erroArquivo.set(mensagemDeErro(erro, 'Não foi possível remover o anexo.'));
      },
    });
  }

  protected baixarAnexo(): void {
    const id = this.id();
    if (!id) {
      return;
    }
    this.api.baixarAnexo(id).subscribe({
      next: (arquivo) => salvarNoNavegador(arquivo),
      error: (erro: unknown) =>
        this.erroArquivo.set(mensagemDeErro(erro, 'Não foi possível baixar o anexo.')),
    });
  }

  protected salvar(): void {
    if (this.formulario.invalid || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const valor = this.formulario.getRawValue();
    const dados: DadosComunicacao = {
      tipo: valor.tipo as TipoComunicacao,
      direcao: valor.direcao || null,
      data: dataIso(valor.data!),
      contraparte: valor.contraparte.trim(),
      assunto: valor.assunto.trim(),
      descricao: valor.descricao.trim() || null,
    };
    const id = this.id();
    const operacao = id ? this.api.corrigir(id, dados) : this.api.registrar(dados);
    this.enviando.set(true);
    this.erro.set(null);
    operacao.subscribe({
      next: (comunicacao) => {
        if (id) {
          this.enviando.set(false);
          this.avisos.open('Registro corrigido.', 'Fechar', { duration: 4000 });
          this.mostrar(comunicacao);
          return;
        }
        this.depoisDeRegistrar(comunicacao);
      },
      error: (erro: unknown) => {
        this.enviando.set(false);
        this.erro.set(mensagemDeErro(erro, 'Não foi possível salvar o registro.'));
      },
    });
  }

  /** Envia o anexo escolhido, se houver, e abre a ficha. Falha no anexo não desfaz o registro. */
  private depoisDeRegistrar(comunicacao: Comunicacao): void {
    const abrir = (mensagem: string) => {
      this.enviando.set(false);
      this.avisos.open(mensagem, 'Fechar', { duration: 6000 });
      void this.router.navigate(['/comunicacoes', comunicacao.id]);
    };
    const arquivo = this.arquivoNovo();
    if (!arquivo) {
      abrir('Registro salvo.');
      return;
    }
    this.api.anexar(comunicacao.id, arquivo).subscribe({
      next: () => abrir('Registro salvo com o anexo.'),
      error: () => abrir('Registro salvo, mas o anexo não foi enviado. Anexe de novo na ficha.'),
    });
  }

  private arquivoDoEvento(evento: Event): File | null {
    const campo = evento.target as HTMLInputElement;
    const arquivo = campo.files?.item(0) ?? null;
    campo.value = '';
    return arquivo;
  }

  private mostrar(comunicacao: Comunicacao): void {
    this.comunicacao.set(comunicacao);
    this.editando.set(false);
    this.erro.set(null);
    this.erroArquivo.set(null);
    this.formulario.setValue({
      tipo: comunicacao.tipo,
      direcao: comunicacao.direcao ?? '',
      data: lerDataIso(comunicacao.data),
      contraparte: comunicacao.contraparte,
      assunto: comunicacao.assunto,
      descricao: comunicacao.descricao ?? '',
    });
  }
}
