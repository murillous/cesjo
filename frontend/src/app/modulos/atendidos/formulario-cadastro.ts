import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core';
import {
  FormControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';

import { mensagemDeErro } from '../../acesso/problema';
import { Sessao } from '../../acesso/sessao';
import { dataDeIso, dataIso, hoje } from '../lista-espera/datas';
import { TIPOS_DEFICIENCIA, TipoDeficiencia } from '../lista-espera/rotulos';
import { AtendidosApi, FichaCompleta } from './atendidos-api';
import {
  CRENCAS,
  CrencaReligiosa,
  ESCOLARIDADES,
  Escolaridade,
  FaixaEtaria,
  GENEROS,
  Genero,
  rotuloFaixa,
} from './rotulos';

/** Mesma regra do backend (`pessoas.Telefone`): 10 ou 11 dígitos, com ou sem máscara. */
const TELEFONE_VALIDO = /^[\s().+-]*(\d[\s().+-]*){10,11}$/;
const CEP_VALIDO = /^(\d{5}-?\d{3})?$/;
/** CPF e NIS: vazio ou 11 dígitos, com pontos, hífens e espaços aceitos. */
const ONZE_DIGITOS = /^$|^[.\s-]*(\d[.\s-]*){11}$/;

/**
 * Cadastro completo do usuário atendido (`/atendidos/:id/cadastro`, RF007, ADR 0144): dados da
 * pessoa, documentos, endereço, escolaridade, renda e benefícios. A crença e a saúde aparecem só para
 * quem tem `ATENDIDOS_DADOS_SENSIVEIS`; para os outros, nem vão no envio e ficam como estão.
 */
@Component({
  selector: 'app-formulario-cadastro',
  imports: [
    MatButtonModule,
    MatCheckboxModule,
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
  templateUrl: './formulario-cadastro.html',
  styleUrl: './formulario-cadastro.scss',
})
export class FormularioCadastro implements OnInit {
  /** Parâmetro da rota: id da inserção. */
  readonly id = input.required<string>();

  private readonly api = inject(AtendidosApi);
  private readonly avisos = inject(MatSnackBar);
  private readonly router = inject(Router);

  protected readonly podeSensiveis = inject(Sessao).temPermissao('ATENDIDOS_DADOS_SENSIVEIS');
  protected readonly tipos = TIPOS_DEFICIENCIA;
  protected readonly generos = GENEROS;
  protected readonly escolaridades = ESCOLARIDADES;
  protected readonly crencas = CRENCAS;
  protected readonly hoje = hoje();
  protected readonly carregando = signal(true);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly nome = signal('');
  protected readonly faixa = signal<string | null>(null);

  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly formulario = this.fb.group({
    nomeCompleto: ['', [Validators.required, Validators.maxLength(150)]],
    dataNascimento: new FormControl<Date | null>(null),
    tipoDeficiencia: ['' as TipoDeficiencia | '', Validators.required],
    telefone: ['', [Validators.required, Validators.pattern(TELEFONE_VALIDO)]],
    cadastro: this.fb.group({
      genero: new FormControl<Genero | null>(null),
      cpf: ['', [Validators.pattern(ONZE_DIGITOS), Validators.maxLength(14)]],
      nis: ['', [Validators.pattern(ONZE_DIGITOS), Validators.maxLength(14)]],
      rg: ['', Validators.maxLength(20)],
      escolaridade: new FormControl<Escolaridade | null>(null),
      cep: ['', Validators.pattern(CEP_VALIDO)],
      logradouro: ['', Validators.maxLength(150)],
      numero: ['', Validators.maxLength(20)],
      complemento: ['', Validators.maxLength(80)],
      bairro: ['', Validators.maxLength(80)],
      cidade: ['', Validators.maxLength(80)],
      rendaFamiliar: new FormControl<number | null>(null, Validators.min(0)),
      recebeBpc: [false],
      inscritoCadUnico: [false],
      recebeBolsaFamilia: [false],
    }),
    dadosSensiveis: this.fb.group({
      crencaReligiosa: new FormControl<CrencaReligiosa | null>(null),
      medicamentosUsoContinuo: ['', Validators.maxLength(2000)],
      observacoesSaude: ['', Validators.maxLength(2000)],
    }),
  });

  ngOnInit(): void {
    this.api.ficha(this.id()).subscribe({
      next: (ficha) => {
        this.carregando.set(false);
        if (!ficha.completo) {
          this.erro.set('Seu perfil não edita o cadastro deste usuário.');
          return;
        }
        this.preencher(ficha);
      },
      error: (erro: unknown) => {
        this.carregando.set(false);
        this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar o cadastro.'));
      },
    });
  }

  private preencher(ficha: FichaCompleta): void {
    this.nome.set(ficha.nomeCompleto);
    this.faixa.set(ficha.dataNascimento ? rotuloFaixa(ficha.faixaEtaria) : null);
    const c = ficha.cadastro;
    const s = ficha.dadosSensiveis;
    this.formulario.setValue({
      nomeCompleto: ficha.nomeCompleto,
      dataNascimento: ficha.dataNascimento ? dataDeIso(ficha.dataNascimento) : null,
      tipoDeficiencia: ficha.tipoDeficiencia,
      telefone: ficha.telefone,
      cadastro: {
        genero: c.genero,
        cpf: c.cpf ?? '',
        nis: c.nis ?? '',
        rg: c.rg ?? '',
        escolaridade: c.escolaridade,
        cep: c.cep ?? '',
        logradouro: c.logradouro ?? '',
        numero: c.numero ?? '',
        complemento: c.complemento ?? '',
        bairro: c.bairro ?? '',
        cidade: c.cidade ?? '',
        rendaFamiliar: c.rendaFamiliar,
        recebeBpc: c.recebeBpc,
        inscritoCadUnico: c.inscritoCadUnico,
        recebeBolsaFamilia: c.recebeBolsaFamilia,
      },
      dadosSensiveis: {
        crencaReligiosa: s?.crencaReligiosa ?? null,
        medicamentosUsoContinuo: s?.medicamentosUsoContinuo ?? '',
        observacoesSaude: s?.observacoesSaude ?? '',
      },
    });
  }

  /** Faixa etária calculada da data digitada, igual à regra do backend (`FaixaEtaria`). */
  protected atualizarFaixa(): void {
    const nascimento = this.formulario.controls.dataNascimento.value;
    if (!nascimento) {
      this.faixa.set(null);
      return;
    }
    const hoje = this.hoje;
    let idade = hoje.getFullYear() - nascimento.getFullYear();
    const fezAniversario =
      hoje.getMonth() > nascimento.getMonth() ||
      (hoje.getMonth() === nascimento.getMonth() && hoje.getDate() >= nascimento.getDate());
    if (!fezAniversario) {
      idade--;
    }
    const faixa: FaixaEtaria | null =
      idade >= 18 && idade <= 29 ? 'JOVEM' : idade >= 30 && idade <= 59 ? 'ADULTO' : null;
    this.faixa.set(rotuloFaixa(faixa));
  }

  protected salvar(): void {
    if (this.formulario.invalid || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const dados = this.formulario.getRawValue();
    const c = dados.cadastro;
    const s = dados.dadosSensiveis;
    this.enviando.set(true);
    this.erro.set(null);
    this.api
      .salvarCadastro(this.id(), {
        nomeCompleto: dados.nomeCompleto.trim(),
        dataNascimento: dados.dataNascimento ? dataIso(dados.dataNascimento) : null,
        tipoDeficiencia: dados.tipoDeficiencia as TipoDeficiencia,
        telefone: dados.telefone,
        cadastro: {
          ...c,
          cpf: texto(c.cpf),
          nis: texto(c.nis),
          rg: texto(c.rg),
          cep: texto(c.cep),
          logradouro: texto(c.logradouro),
          numero: texto(c.numero),
          complemento: texto(c.complemento),
          bairro: texto(c.bairro),
          cidade: texto(c.cidade),
        },
        dadosSensiveis: this.podeSensiveis
          ? {
              crencaReligiosa: s.crencaReligiosa,
              medicamentosUsoContinuo: texto(s.medicamentosUsoContinuo),
              observacoesSaude: texto(s.observacoesSaude),
            }
          : null,
      })
      .subscribe({
        next: () => {
          this.enviando.set(false);
          this.avisos.open('Cadastro salvo.', 'Fechar', { duration: 5000 });
          void this.router.navigate(['/atendidos', this.id()]);
        },
        error: (erro: unknown) => {
          this.enviando.set(false);
          this.erro.set(mensagemDeErro(erro, 'Não foi possível salvar o cadastro.'));
        },
      });
  }
}

function texto(valor: string): string | null {
  const limpo = valor.trim();
  return limpo ? limpo : null;
}
