package br.org.cesjo.sgi.listaespera.interno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import br.org.cesjo.sgi.listaespera.MotivoSaida;
import br.org.cesjo.sgi.listaespera.OrigemEncaminhamento;
import br.org.cesjo.sgi.listaespera.SituacaoInscricao;

/**
 * Inscrição de uma pessoa na lista de espera (tabela {@code inscricao_lista_espera}, ADR 0037). Os
 * dados da pessoa ficam no módulo Pessoas; aqui só o {@code pessoaId}.
 */
@Entity
@Table(name = "inscricao_lista_espera")
class InscricaoListaEspera {

	@Id
	private UUID id;

	@Column(name = "pessoa_id", nullable = false, updatable = false)
	private UUID pessoaId;

	@Column(name = "data_cadastro", nullable = false)
	private LocalDate dataCadastro;

	@Enumerated(EnumType.STRING)
	@Column(name = "origem_encaminhamento", nullable = false, length = 30)
	private OrigemEncaminhamento origemEncaminhamento;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private SituacaoInscricao situacao;

	@Column(name = "data_saida")
	private LocalDate dataSaida;

	@Enumerated(EnumType.STRING)
	@Column(name = "motivo_saida", length = 30)
	private MotivoSaida motivoSaida;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm;

	@Column(name = "criado_por", updatable = false)
	private UUID criadoPor;

	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm;

	@Column(name = "atualizado_por")
	private UUID atualizadoPor;

	@Version
	private long versao;

	protected InscricaoListaEspera() {
	}

	/** Inscrição nova, aguardando vaga. */
	InscricaoListaEspera(UUID pessoaId, LocalDate dataCadastro, OrigemEncaminhamento origem, UUID autor,
			Instant agora) {
		this.id = UUID.randomUUID();
		this.pessoaId = pessoaId;
		this.dataCadastro = dataCadastro;
		this.origemEncaminhamento = origem;
		this.situacao = SituacaoInscricao.AGUARDANDO;
		this.criadoEm = agora;
		this.criadoPor = autor;
		this.atualizadoEm = agora;
		this.atualizadoPor = autor;
	}

	/** Aplica origem e data de cadastro novas e devolve os nomes das colunas que mudaram. */
	Set<String> atualizar(OrigemEncaminhamento origem, LocalDate novaDataCadastro, UUID autor, Instant agora) {
		Set<String> campos = new LinkedHashSet<>();
		if (origemEncaminhamento != origem) {
			origemEncaminhamento = origem;
			campos.add("origem_encaminhamento");
		}
		if (!Objects.equals(dataCadastro, novaDataCadastro)) {
			dataCadastro = novaDataCadastro;
			campos.add("data_cadastro");
		}
		if (!campos.isEmpty()) {
			tocar(autor, agora);
		}
		return campos;
	}

	/** Sai da fila sem inserção. */
	void sair(MotivoSaida motivo, LocalDate data, UUID autor, Instant agora) {
		situacao = SituacaoInscricao.SAIU;
		motivoSaida = motivo;
		dataSaida = data;
		tocar(autor, agora);
	}

	/** Volta para a fila na posição original, que sai da data de cadastro. */
	void voltarParaFila(UUID autor, Instant agora) {
		situacao = SituacaoInscricao.AGUARDANDO;
		motivoSaida = null;
		dataSaida = null;
		tocar(autor, agora);
	}

	private void tocar(UUID autor, Instant agora) {
		atualizadoEm = agora;
		atualizadoPor = autor;
	}

	LocalDate getDataSaida() {
		return dataSaida;
	}

	MotivoSaida getMotivoSaida() {
		return motivoSaida;
	}

	/** Marca o candidato como em avaliação. Devolve se a situação mudou. */
	boolean colocarEmAvaliacao(UUID autor, Instant agora) {
		if (situacao == SituacaoInscricao.EM_AVALIACAO) {
			return false;
		}
		this.situacao = SituacaoInscricao.EM_AVALIACAO;
		this.atualizadoEm = agora;
		this.atualizadoPor = autor;
		return true;
	}

	/** Tira a inscrição da fila porque a pessoa foi inserida no serviço. */
	void registrarInsercao(UUID autor, Instant agora) {
		if (situacao != SituacaoInscricao.AGUARDANDO) {
			throw new IllegalStateException("Inscrição fora da fila: " + situacao);
		}
		this.situacao = SituacaoInscricao.INSERIDO;
		this.atualizadoEm = agora;
		this.atualizadoPor = autor;
	}

	UUID getId() {
		return id;
	}

	UUID getPessoaId() {
		return pessoaId;
	}

	LocalDate getDataCadastro() {
		return dataCadastro;
	}

	OrigemEncaminhamento getOrigemEncaminhamento() {
		return origemEncaminhamento;
	}

	SituacaoInscricao getSituacao() {
		return situacao;
	}

	Instant getCriadoEm() {
		return criadoEm;
	}

}
