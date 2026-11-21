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
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';

import { mensagemDeErro } from '../../acesso/problema';
import { Sessao } from '../../acesso/sessao';
import { dataIso, hoje } from './datas';
import { HistoricoSituacao, Inscricao, ListaEsperaApi } from './lista-espera-api';
import {
  DOCUMENTOS_OBRIGATORIOS,
  MOTIVOS_SAIDA,
  MotivoSaida,
  ORIGENS_ENCAMINHAMENTO,
  rotuloDe,
  SITUACOES,
  SITUACOES_ATIVAS,
  SituacaoInscricao,
  TIPOS_DEFICIENCIA,
} from './rotulos';

/** Formata `31987654321` como `(31) 98765-4321`. */
function formatarTelefone(digitos: string): string {
  const numero = digitos.slice(2);
  const meio = numero.length === 9 ? 5 : 4;
  return `(${digitos.slice(0, 2)}) ${numero.slice(0, meio)}-${numero.slice(meio)}`;
}

/**
 * Ficha do candidato (`/lista-espera/:id`, RF002): dados, documentos, posição, situação e histórico.
 * Quem tem `LISTA_ESPERA_EDITAR` edita os dados e muda a situação, com data e motivo (ADR 0090 e
 * ADR 0091).
 */
@Component({
  selector: 'app-ficha-candidato',
  imports: [
    DatePipe,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule,
    ReactiveFormsModule,
    RouterLink,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './ficha-candidato.html',
  styleUrl: './ficha-candidato.scss',
})
export class FichaCandidato implements OnInit {
  readonly id = input.required<string>();

  private readonly api = inject(ListaEsperaApi);
  private readonly avisos = inject(MatSnackBar);

  protected readonly podeEditar = inject(Sessao).temPermissao('LISTA_ESPERA_EDITAR');
  protected readonly motivos = MOTIVOS_SAIDA;
  protected readonly hoje = hoje();

  protected readonly inscricao = signal<Inscricao | null>(null);
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);
  protected readonly mudando = signal(false);
  protected readonly enviando = signal(false);
  protected readonly erroMudanca = signal<string | null>(null);

  protected readonly mudanca = new FormGroup({
    data: new FormControl<Date | null>(hoje(), Validators.required),
    motivoSaida: new FormControl<MotivoSaida | null>(null),
    observacao: new FormControl('', { nonNullable: true, validators: Validators.maxLength(1000) }),
  });

  /**
   * Situação para a qual o botão leva: quem está na fila (aguardando ou em avaliação) sai; quem saiu
   * volta. Inserido não muda aqui.
   */
  protected readonly proxima = computed<SituacaoInscricao | null>(() => {
    const situacao = this.inscricao()?.situacao;
    if (!situacao) {
      return null;
    }
    return SITUACOES_ATIVAS.has(situacao) ? 'SAIU' : situacao === 'SAIU' ? 'AGUARDANDO' : null;
  });

  protected readonly historico = computed(() => [...(this.inscricao()?.historico ?? [])].reverse());

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

  protected abrirMudanca(): void {
    this.mudanca.reset({ data: hoje(), motivoSaida: null, observacao: '' });
    this.erroMudanca.set(null);
    this.mudando.set(true);
  }

  protected cancelarMudanca(): void {
    this.mudando.set(false);
  }

  protected confirmarMudanca(): void {
    const situacao = this.proxima();
    const { data, motivoSaida, observacao } = this.mudanca.getRawValue();
    const texto = observacao.trim();
    let problema: string | null = null;
    if (!data) {
      problema = 'Informe a data.';
    } else if (situacao === 'SAIU' && !motivoSaida) {
      problema = 'Escolha o motivo da saída.';
    } else if (situacao === 'SAIU' && motivoSaida === 'OUTRO' && !texto) {
      problema = 'Descreva o motivo da saída na observação.';
    } else if (situacao === 'AGUARDANDO' && !texto) {
      problema = 'Explique na observação por que o candidato volta para a fila.';
    }
    if (problema || !situacao || this.enviando()) {
      this.mudanca.markAllAsTouched();
      this.erroMudanca.set(problema);
      return;
    }
    this.enviando.set(true);
    this.erroMudanca.set(null);
    this.api
      .mudarSituacao(this.id(), {
        situacao,
        data: dataIso(data!),
        motivoSaida: situacao === 'SAIU' ? motivoSaida : null,
        observacao: texto || null,
      })
      .subscribe({
        next: (inscricao) => {
          this.inscricao.set(inscricao);
          this.enviando.set(false);
          this.mudando.set(false);
          this.avisos.open(
            situacao === 'SAIU' ? 'Candidato saiu da lista.' : 'Candidato voltou para a fila.',
            'Fechar',
            { duration: 5000 },
          );
        },
        error: (erro: unknown) => {
          this.enviando.set(false);
          this.erroMudanca.set(mensagemDeErro(erro, 'Não foi possível mudar a situação.'));
        },
      });
  }

  protected tipo(inscricao: Inscricao): string {
    return rotuloDe(TIPOS_DEFICIENCIA, inscricao.tipoDeficiencia);
  }

  protected origem(inscricao: Inscricao): string {
    return rotuloDe(ORIGENS_ENCAMINHAMENTO, inscricao.origemEncaminhamento);
  }

  protected situacao(valor: SituacaoInscricao): string {
    return rotuloDe(SITUACOES, valor);
  }

  protected motivo(valor: MotivoSaida | null | undefined): string {
    return rotuloDe(MOTIVOS_SAIDA, valor);
  }

  protected telefone(inscricao: Inscricao): string {
    return formatarTelefone(inscricao.telefone);
  }

  /** Descrição de uma linha do histórico, do ponto de vista de quem lê a ficha. */
  protected evento(item: HistoricoSituacao): string {
    if (!item.situacaoAnterior) {
      return 'Entrou na fila';
    }
    if (item.situacaoNova === 'SAIU') {
      return `Saiu da lista: ${this.motivo(item.motivoSaida).toLowerCase()}`;
    }
    if (item.situacaoNova === 'AGUARDANDO') {
      return 'Voltou para a fila';
    }
    return this.situacao(item.situacaoNova);
  }
}
