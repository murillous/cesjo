package br.org.cesjo.sgi.atendimento.interno;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.seguranca.Identidade;
import br.org.cesjo.sgi.compartilhado.seguranca.IdentidadeAtual;
import br.org.cesjo.sgi.compartilhado.web.RecursoNaoEncontradoException;
import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;
import br.org.cesjo.sgi.pessoas.Pessoas;

/** Elaboração, edição e leitura auditada do PIA (RF006, ADR 0103). */
@Service
class ServicoPias {

	static final String ENTIDADE = "pia";

	/** Conversor próprio do conteúdo: o formato guardado não muda com a configuração da API HTTP. */
	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final PiaRepositorio pias;

	private final Pessoas pessoas;

	private final Auditoria auditoria;

	private final IdentidadeAtual identidadeAtual;

	private final Clock relogio = Fuso.RELOGIO;

	ServicoPias(PiaRepositorio pias, Pessoas pessoas, Auditoria auditoria, IdentidadeAtual identidadeAtual) {
		this.pias = pias;
		this.pessoas = pessoas;
		this.auditoria = auditoria;
		this.identidadeAtual = identidadeAtual;
	}

	/** Primeira versão do PIA da pessoa, em elaboração. */
	@Transactional
	PiaResposta elaborar(NovoPia novo) {
		if (pessoas.obter(novo.pessoaId()).isEmpty()) {
			throw new RegraNegocioException("A pessoa escolhida não existe.");
		}
		if (pias.findFirstByPessoaIdOrderByNumeroVersaoDesc(novo.pessoaId()).isPresent()) {
			throw new RegraNegocioException("Esta pessoa já tem PIA. Edite a versão atual ou crie uma nova versão.");
		}
		Pia pia = pias.save(new Pia(novo.pessoaId(), null, json(novo.conteudo()), autor(), relogio.instant()));
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE, pia.getId()));
		return resposta(pia);
	}

	/** Edita a versão mais nova, enquanto ela estiver em elaboração. */
	@Transactional
	PiaResposta editar(UUID id, ConteudoPia conteudo) {
		Pia pia = buscar(id);
		exigirMaisNova(pia);
		if (pia.editar(json(conteudo), autor(), relogio.instant())) {
			auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE, id, Set.of("conteudo")));
		}
		return resposta(pia);
	}

	/** Versão com o conteúdo. Toda leitura vai para o log como leitura sensível (RNF 3.3). */
	@Transactional
	PiaResposta ler(UUID id) {
		Pia pia = buscar(id);
		auditoria.registrar(EventoAuditoria.leituraSensivel(ENTIDADE, id));
		return resposta(pia);
	}

	/** Apresentação à família, à equipe ou aprovação da versão mais nova (ADR 0106). */
	@Transactional
	PiaResposta avancar(UUID id, StatusPia novo, LocalDate data) {
		Pia pia = buscar(id);
		exigirMaisNova(pia);
		if (data.isAfter(LocalDate.now(relogio))) {
			throw new RegraNegocioException("A data não pode ser futura.");
		}
		String coluna = pia.avancar(novo, data, autor(), relogio.instant());
		Set<String> campos = novo == StatusPia.APROVADO ? Set.of("status", coluna, "aprovado_por")
				: Set.of("status", coluna);
		auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE, id, campos));
		return resposta(pia);
	}

	/**
	 * Nova versão em elaboração a partir da mais nova já apresentada ou aprovada, com o mesmo conteúdo. A
	 * versão de origem fica como estava (ADR 0106).
	 */
	@Transactional
	PiaResposta novaVersao(UUID id) {
		Pia origem = buscar(id);
		exigirMaisNova(origem);
		if (origem.getStatus() == StatusPia.ELABORADO) {
			throw new RegraNegocioException("Esta versão ainda está em elaboração. Edite-a em vez de criar outra.");
		}
		Pia nova = pias.save(new Pia(origem.getPessoaId(), origem, origem.getConteudo(), autor(), relogio.instant()));
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE, nova.getId()));
		return resposta(nova);
	}

	@Transactional(readOnly = true)
	List<ResumoPia> versoes(UUID pessoaId) {
		return pias.findByPessoaIdOrderByNumeroVersaoDesc(pessoaId).stream().map(ResumoPia::de).toList();
	}

	Pia buscar(UUID id) {
		return pias.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("PIA não encontrado."));
	}

	/** Versões antigas ficam como estavam: só a mais nova muda. */
	void exigirMaisNova(Pia pia) {
		boolean maisNova = pias.findFirstByPessoaIdOrderByNumeroVersaoDesc(pia.getPessoaId())
			.map(atual -> atual.getId().equals(pia.getId()))
			.orElse(false);
		if (!maisNova) {
			throw new RegraNegocioException("Esta versão do PIA foi substituída por uma mais nova e não muda mais.");
		}
	}

	PiaResposta resposta(Pia pia) {
		String nome = pessoas.nomes(List.of(pia.getPessoaId())).get(pia.getPessoaId());
		boolean maisNova = pias.findFirstByPessoaIdOrderByNumeroVersaoDesc(pia.getPessoaId())
			.map(atual -> atual.getId().equals(pia.getId()))
			.orElse(false);
		return new PiaResposta(pia.getId(), pia.getPessoaId(), nome, pia.getNumeroVersao(), pia.getStatus(),
				conteudo(pia.getConteudo()), maisNova && pia.getStatus() == StatusPia.ELABORADO, maisNova,
				pia.getVersaoAnteriorId(), pia.getApresentadoFamiliaEm(), pia.getApresentadoEquipeEm(),
				pia.getAprovadoEm(), pia.getCriadoEm(), pia.getAtualizadoEm());
	}

	static String json(ConteudoPia conteudo) {
		return JSON.writeValueAsString(conteudo);
	}

	static ConteudoPia conteudo(String json) {
		return JSON.readValue(json, ConteudoPia.class);
	}

	UUID autor() {
		return identidadeAtual.obter().map(Identidade::id).orElse(null);
	}

}
