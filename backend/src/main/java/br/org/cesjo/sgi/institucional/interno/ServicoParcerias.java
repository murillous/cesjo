package br.org.cesjo.sgi.institucional.interno;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.seguranca.Identidade;
import br.org.cesjo.sgi.compartilhado.seguranca.IdentidadeAtual;
import br.org.cesjo.sgi.compartilhado.web.RecursoNaoEncontradoException;
import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;

/** Cadastro dos parceiros e registro das ações de articulação (RF013, ADR 0172). */
@Service
class ServicoParcerias {

	/** Nomes das entidades no log de auditoria. */
	static final String ENTIDADE_PARCEIRO = "parceiro";

	static final String ENTIDADE_ARTICULACAO = "articulacao_parceria";

	private final ParceiroRepositorio parceiros;

	private final ArticulacaoRepositorio articulacoes;

	private final Auditoria auditoria;

	private final IdentidadeAtual identidadeAtual;

	private final Clock relogio = Clock.systemUTC();

	ServicoParcerias(ParceiroRepositorio parceiros, ArticulacaoRepositorio articulacoes, Auditoria auditoria,
			IdentidadeAtual identidadeAtual) {
		this.parceiros = parceiros;
		this.articulacoes = articulacoes;
		this.auditoria = auditoria;
		this.identidadeAtual = identidadeAtual;
	}

	@Transactional(readOnly = true)
	Page<ParceiroResposta> listar(Boolean ativo, String nome, Pageable paginacao) {
		String filtroNome = DadosParceiro.texto(nome);
		return parceiros.buscar(ativo, filtroNome, paginacao).map(ParceiroResposta::de);
	}

	@Transactional(readOnly = true)
	ParceiroResposta obter(UUID id) {
		return ParceiroResposta.de(buscar(id));
	}

	@Transactional
	ParceiroResposta criar(DadosParceiro dados) {
		Parceiro parceiro = parceiros.save(new Parceiro(dados, autor(), relogio.instant()));
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE_PARCEIRO, parceiro.getId()));
		return ParceiroResposta.de(parceiro);
	}

	/** Troca os dados e registra na auditoria só os campos que mudaram de fato. */
	@Transactional
	ParceiroResposta editar(UUID id, DadosParceiro dados) {
		if (dados.ativo() == null) {
			throw new RegraNegocioException("Informe se o parceiro está ativo.");
		}
		Parceiro parceiro = buscar(id);
		Set<String> alterados = camposAlterados(parceiro, dados);
		if (!alterados.isEmpty()) {
			UUID autor = autor();
			Instant agora = relogio.instant();
			parceiro.aplicar(dados, autor, agora);
			parceiro.alterarAtivo(dados.ativo(), autor, agora);
			auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE_PARCEIRO, parceiro.getId(), alterados));
		}
		return ParceiroResposta.de(parceiro);
	}

	@Transactional(readOnly = true)
	List<ArticulacaoResposta> listarArticulacoes(UUID parceiroId) {
		buscar(parceiroId);
		return articulacoes.findByParceiroIdOrderByDataDescCriadoEmDesc(parceiroId)
			.stream()
			.map(ArticulacaoResposta::de)
			.toList();
	}

	@Transactional
	ArticulacaoResposta registrarArticulacao(UUID parceiroId, NovaArticulacao nova) {
		buscar(parceiroId);
		ArticulacaoParceria articulacao = articulacoes
			.save(new ArticulacaoParceria(parceiroId, nova.data(), nova.descricao(), autor(), relogio.instant()));
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE_ARTICULACAO, articulacao.getId()));
		return ArticulacaoResposta.de(articulacao);
	}

	private static Set<String> camposAlterados(Parceiro atual, DadosParceiro novo) {
		Set<String> alterados = new LinkedHashSet<>();
		marcar(alterados, "nome", atual.getNome(), novo.nome().strip());
		marcar(alterados, "tipo", atual.getTipo(), novo.tipo());
		marcar(alterados, "instrumento", atual.getInstrumento(), DadosParceiro.texto(novo.instrumento()));
		marcar(alterados, "vigencia_inicio", atual.getVigenciaInicio(), novo.vigenciaInicio());
		marcar(alterados, "vigencia_fim", atual.getVigenciaFim(), novo.vigenciaFim());
		marcar(alterados, "contato", atual.getContato(), DadosParceiro.texto(novo.contato()));
		marcar(alterados, "observacoes", atual.getObservacoes(), DadosParceiro.texto(novo.observacoes()));
		marcar(alterados, "ativo", atual.isAtivo(), novo.ativo());
		return alterados;
	}

	private static void marcar(Set<String> alterados, String campo, Object antes, Object depois) {
		if (!Objects.equals(antes, depois)) {
			alterados.add(campo);
		}
	}

	private Parceiro buscar(UUID id) {
		return parceiros.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Parceiro não encontrado."));
	}

	private UUID autor() {
		return identidadeAtual.obter().map(Identidade::id).orElse(null);
	}

}
