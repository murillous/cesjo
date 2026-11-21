import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  OnInit,
  signal,
} from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';

import { mensagemDeErro } from '../../acesso/problema';
import { Sessao } from '../../acesso/sessao';
import { salvarNoNavegador } from '../../compartilhado/arquivos/arquivos-api';
import { EnvioArquivo } from '../../compartilhado/arquivos/envio-arquivo';
import { formatarTamanho } from '../../compartilhado/arquivos/regras-arquivo';
import { DocumentoInscricao, Inscricao, ListaEsperaApi } from './lista-espera-api';
import { DOCUMENTOS_OBRIGATORIOS, TipoDocumento } from './rotulos';

interface Linha {
  tipo: TipoDocumento;
  rotulo: string;
  documento: DocumentoInscricao | null;
  campo: FormControl<string | null>;
}

/**
 * Documentos de um candidato (`/lista-espera/:id/documentos`, RF001): mostra o laudo médico e o
 * comprovante de residência, baixa (`DOCUMENTOS_LER`) e anexa ou substitui (`DOCUMENTOS_ENVIAR`).
 * É o caminho para completar um cadastro incompleto (ADR 0056).
 */
@Component({
  selector: 'app-documentos-candidato',
  imports: [
    DatePipe,
    EnvioArquivo,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    ReactiveFormsModule,
    RouterLink,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './documentos-candidato.html',
  styleUrl: './documentos-candidato.scss',
})
export class DocumentosCandidato implements OnInit {
  /** Id da inscrição, vindo da rota. */
  readonly id = input.required<string>();

  private readonly api = inject(ListaEsperaApi);
  private readonly avisos = inject(MatSnackBar);
  private readonly sessao = inject(Sessao);

  protected readonly podeBaixar = this.sessao.temPermissao('DOCUMENTOS_LER');
  protected readonly podeEnviar = this.sessao.temPermissao('DOCUMENTOS_ENVIAR');

  protected readonly inscricao = signal<Inscricao | null>(null);
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);
  protected readonly baixando = signal<TipoDocumento | null>(null);

  private readonly campos: Record<TipoDocumento, FormControl<string | null>> = {
    LAUDO_MEDICO: new FormControl<string | null>(null),
    COMPROVANTE_RESIDENCIA: new FormControl<string | null>(null),
  };

  protected readonly linhas = computed<Linha[]>(() => {
    const documentos = this.inscricao()?.documentos ?? [];
    return DOCUMENTOS_OBRIGATORIOS.map(({ valor, rotulo }) => ({
      tipo: valor,
      rotulo,
      documento: documentos.find((documento) => documento.tipo === valor) ?? null,
      campo: this.campos[valor],
    }));
  });

  protected readonly pendentes = computed(() => {
    const pendentes = this.inscricao()?.documentosPendentes ?? [];
    return DOCUMENTOS_OBRIGATORIOS.filter((d) => pendentes.includes(d.valor))
      .map((d) => d.rotulo.toLowerCase())
      .join(' e ');
  });

  ngOnInit(): void {
    this.api.obter(this.id()).subscribe({
      next: (inscricao) => {
        this.inscricao.set(inscricao);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.carregando.set(false);
        this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar o candidato.'));
      },
    });
  }

  protected tamanho(bytes: number): string {
    return formatarTamanho(bytes);
  }

  /** O arquivo já foi guardado pelo `app-envio-arquivo`; aqui ele é ligado ao candidato. */
  protected anexar(tipo: TipoDocumento, arquivoId: string, rotulo: string): void {
    this.erro.set(null);
    this.api.anexar(this.id(), tipo, arquivoId).subscribe({
      next: (inscricao) => {
        this.inscricao.set(inscricao);
        this.campos[tipo].setValue(null);
        this.avisos.open(`${rotulo} anexado.`, 'Fechar', { duration: 5000 });
      },
      error: (erro: unknown) => {
        this.campos[tipo].setValue(null);
        this.erro.set(mensagemDeErro(erro, `Não foi possível anexar o ${rotulo.toLowerCase()}.`));
      },
    });
  }

  protected baixar(tipo: TipoDocumento): void {
    this.baixando.set(tipo);
    this.erro.set(null);
    this.api.baixarDocumento(this.id(), tipo).subscribe({
      next: (arquivo) => {
        this.baixando.set(null);
        salvarNoNavegador(arquivo);
      },
      error: (erro: unknown) => {
        this.baixando.set(null);
        this.erro.set(mensagemDeErro(erro, 'Não foi possível baixar o documento.'));
      },
    });
  }
}
