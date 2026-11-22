package br.org.cesjo.sgi.pessoas.interno;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import br.org.cesjo.sgi.compartilhado.criptografia.TextoCifradoConverter;
import br.org.cesjo.sgi.pessoas.CadastroComplementar;
import br.org.cesjo.sgi.pessoas.CrencaReligiosa;
import br.org.cesjo.sgi.pessoas.DadosSensiveis;
import br.org.cesjo.sgi.pessoas.Escolaridade;
import br.org.cesjo.sgi.pessoas.Genero;

/**
 * Cadastro completo de uma pessoa (tabela {@code cadastro_complementar}, ADR 0140), uma linha por
 * pessoa. Os campos espelham {@link CadastroComplementar} e {@link DadosSensiveis}; os nomes são os das
 * colunas em camelCase, que a auditoria usa (ver {@link CamposAlterados}).
 */
@Entity
@Table(name = "cadastro_complementar")
class CadastroComplementarPessoa {

	@Id
	@Column(name = "pessoa_id")
	private UUID pessoaId;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private Genero genero;

	@Convert(converter = TextoCifradoConverter.class)
	private String cpf;

	@Convert(converter = TextoCifradoConverter.class)
	private String nis;

	@Convert(converter = TextoCifradoConverter.class)
	private String rg;

	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private Escolaridade escolaridade;

	@Column(length = 8)
	private String cep;

	@Column(length = 150)
	private String logradouro;

	@Column(length = 20)
	private String numero;

	@Column(length = 80)
	private String complemento;

	@Column(length = 80)
	private String bairro;

	@Column(length = 80)
	private String cidade;

	@Column(name = "renda_familiar", precision = 12, scale = 2)
	private BigDecimal rendaFamiliar;

	@Column(name = "recebe_bpc", nullable = false)
	private boolean recebeBpc;

	@Column(name = "inscrito_cad_unico", nullable = false)
	private boolean inscritoCadUnico;

	@Column(name = "recebe_bolsa_familia", nullable = false)
	private boolean recebeBolsaFamilia;

	@Enumerated(EnumType.STRING)
	@Column(name = "crenca_religiosa", length = 30)
	private CrencaReligiosa crencaReligiosa;

	@Column(name = "medicamentos_uso_continuo")
	@Convert(converter = TextoCifradoConverter.class)
	private String medicamentosUsoContinuo;

	@Column(name = "observacoes_saude")
	@Convert(converter = TextoCifradoConverter.class)
	private String observacoesSaude;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm;

	@Column(name = "criado_por", updatable = false)
	private UUID criadoPor;

	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm;

	@Column(name = "atualizado_por")
	private UUID atualizadoPor;

	@Version
	private Long versao;

	protected CadastroComplementarPessoa() {
	}

	/** Cadastro ainda vazio da pessoa. */
	CadastroComplementarPessoa(UUID pessoaId, UUID autor, Instant agora) {
		this.pessoaId = pessoaId;
		this.criadoEm = agora;
		this.criadoPor = autor;
		this.atualizadoEm = agora;
		this.atualizadoPor = autor;
	}

	CadastroComplementar complementar() {
		return new CadastroComplementar(genero, cpf, nis, rg, escolaridade, cep, logradouro, numero, complemento,
				bairro, cidade, rendaFamiliar, recebeBpc, inscritoCadUnico, recebeBolsaFamilia);
	}

	DadosSensiveis sensiveis() {
		return new DadosSensiveis(crencaReligiosa, medicamentosUsoContinuo, observacoesSaude);
	}

	/** Aplica os dados novos e devolve os nomes das colunas que mudaram. */
	Set<String> atualizar(CadastroComplementar dados, UUID autor, Instant agora) {
		Set<String> campos = CamposAlterados.entre(complementar(), dados);
		genero = dados.genero();
		cpf = dados.cpf();
		nis = dados.nis();
		rg = dados.rg();
		escolaridade = dados.escolaridade();
		cep = dados.cep();
		logradouro = dados.logradouro();
		numero = dados.numero();
		complemento = dados.complemento();
		bairro = dados.bairro();
		cidade = dados.cidade();
		rendaFamiliar = dados.rendaFamiliar();
		recebeBpc = dados.recebeBpc();
		inscritoCadUnico = dados.inscritoCadUnico();
		recebeBolsaFamilia = dados.recebeBolsaFamilia();
		return marcar(campos, autor, agora);
	}

	/** Aplica a crença e os dados de saúde novos e devolve os nomes das colunas que mudaram. */
	Set<String> atualizar(DadosSensiveis dados, UUID autor, Instant agora) {
		Set<String> campos = CamposAlterados.entre(sensiveis(), dados);
		crencaReligiosa = dados.crencaReligiosa();
		medicamentosUsoContinuo = dados.medicamentosUsoContinuo();
		observacoesSaude = dados.observacoesSaude();
		return marcar(campos, autor, agora);
	}

	private Set<String> marcar(Set<String> campos, UUID autor, Instant agora) {
		if (!campos.isEmpty()) {
			atualizadoEm = agora;
			atualizadoPor = autor;
		}
		return campos;
	}

	UUID getPessoaId() {
		return pessoaId;
	}

}
