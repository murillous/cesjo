package br.org.cesjo.sgi.institucional.interno;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import br.org.cesjo.sgi.institucional.FuncaoColaborador;
import br.org.cesjo.sgi.institucional.ResumoColaborador;

/** Integrante da equipe do Centro-Dia (tabela {@code colaborador}, ADR 0045). */
@Entity
@Table(name = "colaborador")
class Colaborador {

	@Id
	private UUID id;

	@Column(nullable = false, length = 150)
	private String nome;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private FuncaoColaborador funcao;

	@Column(nullable = false)
	private boolean ativo;

	/** Conta de acesso ligada, opcional (ADR 0046). */
	@Column(name = "conta_acesso_id")
	private UUID contaAcessoId;

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

	protected Colaborador() {
	}

	/** Colaborador novo, sempre ativo. */
	Colaborador(String nome, FuncaoColaborador funcao, UUID contaAcessoId, UUID autor, Instant agora) {
		this.id = UUID.randomUUID();
		this.nome = nome.strip();
		this.funcao = funcao;
		this.ativo = true;
		this.contaAcessoId = contaAcessoId;
		this.criadoEm = agora;
		this.criadoPor = autor;
		this.atualizadoEm = agora;
		this.atualizadoPor = autor;
	}

	void alterarNome(String nome, UUID autor, Instant agora) {
		this.nome = nome.strip();
		tocar(autor, agora);
	}

	void alterarFuncao(FuncaoColaborador funcao, UUID autor, Instant agora) {
		this.funcao = funcao;
		tocar(autor, agora);
	}

	void alterarAtivo(boolean ativo, UUID autor, Instant agora) {
		this.ativo = ativo;
		tocar(autor, agora);
	}

	void ligarConta(UUID contaAcessoId, UUID autor, Instant agora) {
		this.contaAcessoId = contaAcessoId;
		tocar(autor, agora);
	}

	private void tocar(UUID autor, Instant agora) {
		this.atualizadoEm = agora;
		this.atualizadoPor = autor;
	}

	ResumoColaborador resumo() {
		return new ResumoColaborador(id, nome, funcao);
	}

	UUID getId() {
		return id;
	}

	String getNome() {
		return nome;
	}

	FuncaoColaborador getFuncao() {
		return funcao;
	}

	boolean isAtivo() {
		return ativo;
	}

	UUID getContaAcessoId() {
		return contaAcessoId;
	}

	Instant getCriadoEm() {
		return criadoEm;
	}

	Instant getAtualizadoEm() {
		return atualizadoEm;
	}

}
