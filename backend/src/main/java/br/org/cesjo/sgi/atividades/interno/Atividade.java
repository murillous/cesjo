package br.org.cesjo.sgi.atividades.interno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import br.org.cesjo.sgi.atividades.TipoAtividade;
import br.org.cesjo.sgi.compartilhado.criptografia.TextoCifradoConverter;

/**
 * Atividade do RF010 (tabela {@code atividade}, ADR 0150). Guarda só os {@code id} das pessoas
 * participantes e dos colaboradores; a descrição é cifrada na aplicação.
 */
@Entity
@Table(name = "atividade")
class Atividade {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private TipoAtividade tipo;

	@Column(name = "data_atividade", nullable = false)
	private LocalDate data;

	@Column(nullable = false, length = 200)
	private String titulo;

	@Convert(converter = TextoCifradoConverter.class)
	@Column(nullable = false, columnDefinition = "text")
	private String descricao;

	/** Usuários atendidos que participaram, pelo {@code pessoa_id}. */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "atividade_participante", joinColumns = @JoinColumn(name = "atividade_id"))
	@Column(name = "pessoa_id", nullable = false)
	private Set<UUID> participantes = new LinkedHashSet<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "atividade_profissional", joinColumns = @JoinColumn(name = "atividade_id"))
	@Column(name = "colaborador_id", nullable = false)
	private Set<UUID> profissionais = new LinkedHashSet<>();

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

	protected Atividade() {
	}

	Atividade(DadosAtividade dados, UUID autor, Instant agora) {
		this.id = UUID.randomUUID();
		aplicar(dados);
		this.criadoEm = agora;
		this.criadoPor = autor;
		this.atualizadoEm = agora;
		this.atualizadoPor = autor;
	}

	/** Aplica a correção e devolve os nomes das colunas e listas que mudaram. */
	Set<String> corrigir(DadosAtividade dados, UUID autor, Instant agora) {
		Set<String> alterados = new LinkedHashSet<>();
		if (tipo != dados.tipo()) {
			alterados.add("tipo");
		}
		if (!data.equals(dados.data())) {
			alterados.add("data_atividade");
		}
		if (!titulo.equals(dados.titulo().strip())) {
			alterados.add("titulo");
		}
		if (!descricao.equals(dados.descricao().strip())) {
			alterados.add("descricao");
		}
		if (!participantes.equals(Set.copyOf(dados.participantes()))) {
			alterados.add("participantes");
		}
		if (!profissionais.equals(Set.copyOf(dados.profissionais()))) {
			alterados.add("profissionais");
		}
		if (!alterados.isEmpty()) {
			aplicar(dados);
			this.atualizadoEm = agora;
			this.atualizadoPor = autor;
		}
		return alterados;
	}

	private void aplicar(DadosAtividade dados) {
		this.tipo = dados.tipo();
		this.data = dados.data();
		this.titulo = dados.titulo().strip();
		this.descricao = dados.descricao().strip();
		this.participantes.clear();
		this.participantes.addAll(dados.participantes());
		this.profissionais.clear();
		this.profissionais.addAll(dados.profissionais());
	}

	UUID getId() {
		return id;
	}

	TipoAtividade getTipo() {
		return tipo;
	}

	LocalDate getData() {
		return data;
	}

	String getTitulo() {
		return titulo;
	}

	String getDescricao() {
		return descricao;
	}

	Set<UUID> getParticipantes() {
		return Set.copyOf(participantes);
	}

	Set<UUID> getProfissionais() {
		return Set.copyOf(profissionais);
	}

	Instant getCriadoEm() {
		return criadoEm;
	}

	Instant getAtualizadoEm() {
		return atualizadoEm;
	}

}
