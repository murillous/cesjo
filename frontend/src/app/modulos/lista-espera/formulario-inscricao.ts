import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
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

import { mensagemDeErro } from '../../acesso/problema';
import { Sessao } from '../../acesso/sessao';
import { EnvioArquivo } from '../../compartilhado/arquivos/envio-arquivo';
import { dataIso, hoje } from './datas';
import { ListaEsperaApi } from './lista-espera-api';
import {
  ORIGENS_ENCAMINHAMENTO,
  OrigemEncaminhamento,
  TIPOS_DEFICIENCIA,
  TipoDeficiencia,
} from './rotulos';

/** Mesma regra do backend (`pessoas.Telefone`): 10 ou 11 dígitos, com ou sem máscara. */
const TELEFONE_VALIDO = /^[\s().+-]*(\d[\s().+-]*){10,11}$/;

/**
 * Cadastro de candidato na lista de espera (`/lista-espera/nova`, RF001). O laudo médico e o
 * comprovante de residência são opcionais aqui; sem eles o cadastro fica incompleto (ADR 0056).
 */
@Component({
  selector: 'app-formulario-inscricao',
  imports: [
    EnvioArquivo,
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
  templateUrl: './formulario-inscricao.html',
  styleUrl: './formulario-inscricao.scss',
})
export class FormularioInscricao {
  private readonly api = inject(ListaEsperaApi);
  private readonly avisos = inject(MatSnackBar);
  private readonly router = inject(Router);

  protected readonly podeEnviarDocumentos = inject(Sessao).temPermissao('DOCUMENTOS_ENVIAR');
  protected readonly tipos = TIPOS_DEFICIENCIA;
  protected readonly origens = ORIGENS_ENCAMINHAMENTO;
  protected readonly hoje = hoje();
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    nomeCompleto: ['', [Validators.required, Validators.maxLength(150)]],
    // PENDENTE-CESJO(P2): data de nascimento opcional, sem bloqueio por idade (ADR 0038).
    dataNascimento: new FormControl<Date | null>(null),
    tipoDeficiencia: ['' as TipoDeficiencia | '', Validators.required],
    telefone: ['', [Validators.required, Validators.pattern(TELEFONE_VALIDO)]],
    nomeResponsavel: ['', [Validators.required, Validators.maxLength(150)]],
    origemEncaminhamento: ['' as OrigemEncaminhamento | '', Validators.required],
    dataCadastro: new FormControl<Date | null>(hoje(), Validators.required),
    laudoMedicoArquivoId: new FormControl<string | null>(null),
    comprovanteResidenciaArquivoId: new FormControl<string | null>(null),
  });

  protected salvar(): void {
    if (this.formulario.invalid || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const dados = this.formulario.getRawValue();
    this.enviando.set(true);
    this.erro.set(null);
    this.api
      .inscrever({
        nomeCompleto: dados.nomeCompleto.trim(),
        dataNascimento: dados.dataNascimento ? dataIso(dados.dataNascimento) : null,
        tipoDeficiencia: dados.tipoDeficiencia as TipoDeficiencia,
        telefone: dados.telefone,
        nomeResponsavel: dados.nomeResponsavel.trim(),
        origemEncaminhamento: dados.origemEncaminhamento as OrigemEncaminhamento,
        dataCadastro: dataIso(dados.dataCadastro!),
        laudoMedicoArquivoId: dados.laudoMedicoArquivoId,
        comprovanteResidenciaArquivoId: dados.comprovanteResidenciaArquivoId,
      })
      .subscribe({
        next: (inscricao) => {
          this.enviando.set(false);
          const aviso = inscricao.cadastroCompleto
            ? `${inscricao.nomeCompleto} entrou na lista de espera.`
            : `${inscricao.nomeCompleto} entrou na lista de espera com cadastro incompleto.`;
          this.avisos.open(aviso, 'Fechar', { duration: 5000 });
          void this.router.navigate(['/lista-espera']);
        },
        error: (erro: unknown) => {
          this.enviando.set(false);
          this.erro.set(mensagemDeErro(erro, 'Não foi possível cadastrar o candidato.'));
        },
      });
  }
}
