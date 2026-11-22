package br.org.cesjo.sgi.institucional.interno;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.seguranca.Identidade;
import br.org.cesjo.sgi.compartilhado.seguranca.IdentidadeAtual;
import br.org.cesjo.sgi.compartilhado.web.RecursoNaoEncontradoException;
import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;
import br.org.cesjo.sgi.institucional.Colaboradores;

/**
 * Contratações e desligamentos dos colaboradores (RF014, ADR 0170). A contratação abre um vínculo e
 * deixa o colaborador ativo; o desligamento fecha o vínculo e o deixa inativo, e por isso ele some das
 * listas de profissionais para novas visitas, escutas e atividades (API {@code Colaboradores}).
 */
@Service
class ServicoVinculos {

	/** Nome da entidade vínculo no log de auditoria. */
	static final String ENTIDADE = "vinculo_colaborador";

	private final VinculoColaboradorRepositorio vinculos;

	private final ColaboradorRepositorio colaboradores;

	private final Auditoria auditoria;

	private final IdentidadeAtual identidadeAtual;

	private final Clock relogio = Clock.systemUTC();

	ServicoVinculos(VinculoColaboradorRepositorio vinculos, ColaboradorRepositorio colaboradores, Auditoria auditoria,
			IdentidadeAtual identidadeAtual) {
		this.vinculos = vinculos;
		this.colaboradores = colaboradores;
		this.auditoria = auditoria;
		this.identidadeAtual = identidadeAtual;
	}

	@Transactional(readOnly = true)
	List<VinculoResposta> listar(UUID colaboradorId) {
		buscarColaborador(colaboradorId);
		return vinculos.findByColaboradorIdOrderByDataContratacaoDescCriadoEmDesc(colaboradorId)
			.stream()
			.map(VinculoResposta::de)
			.toList();
	}

	/**
	 * Abre um vínculo. Não pode haver outro aberto, e a data não pode vir antes do último desligamento.
	 */
	@Transactional
	VinculoResposta contratar(UUID colaboradorId, LocalDate dataContratacao) {
		Colaborador colaborador = buscarColaborador(colaboradorId);
		List<VinculoColaborador> historico = vinculos
			.findByColaboradorIdOrderByDataContratacaoDescCriadoEmDesc(colaboradorId);
		if (historico.stream().anyMatch(VinculoColaborador::isAberto)) {
			throw new RegraNegocioException("O colaborador já tem um vínculo em aberto. Registre o desligamento antes.");
		}
		boolean antesDoUltimoDesligamento = historico.stream()
			.anyMatch(anterior -> dataContratacao.isBefore(anterior.getDataDesligamento()));
		if (antesDoUltimoDesligamento) {
			throw new RegraNegocioException("A contratação não pode ser anterior ao último desligamento.");
		}
		UUID autor = autor();
		Instant agora = relogio.instant();
		VinculoColaborador vinculo = vinculos.save(new VinculoColaborador(colaboradorId, dataContratacao, autor, agora));
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE, vinculo.getId()));
		reativar(colaborador, autor, agora);
		return VinculoResposta.de(vinculo);
	}

	/** Fecha o vínculo aberto com data e motivo e deixa o colaborador inativo. */
	@Transactional
	VinculoResposta desligar(UUID colaboradorId, NovoDesligamentoColaborador novo) {
		Colaborador colaborador = buscarColaborador(colaboradorId);
		VinculoColaborador vinculo = vinculos.findByColaboradorIdAndDataDesligamentoIsNull(colaboradorId)
			.orElseThrow(() -> new RegraNegocioException(
					"O colaborador não tem vínculo em aberto. Registre a contratação antes do desligamento."));
		if (novo.data().isBefore(vinculo.getDataContratacao())) {
			throw new RegraNegocioException("O desligamento não pode ser anterior à contratação.");
		}
		UUID autor = autor();
		Instant agora = relogio.instant();
		vinculo.desligar(novo.data(), novo.motivo(), autor, agora);
		auditoria.registrar(
				EventoAuditoria.alteracao(ENTIDADE, vinculo.getId(), Set.of("data_desligamento", "motivo_desligamento")));
		if (colaborador.isAtivo()) {
			colaborador.alterarAtivo(false, autor, agora);
			auditoria.registrar(EventoAuditoria.alteracao(Colaboradores.ENTIDADE, colaborador.getId(), Set.of("ativo")));
		}
		return VinculoResposta.de(vinculo);
	}

	/** Tem vínculos e nenhum aberto: só volta a ficar ativo por uma nova contratação. */
	@Transactional(readOnly = true)
	boolean desligado(UUID colaboradorId) {
		return vinculos.existsByColaboradorId(colaboradorId)
				&& vinculos.findByColaboradorIdAndDataDesligamentoIsNull(colaboradorId).isEmpty();
	}

	private void reativar(Colaborador colaborador, UUID autor, Instant agora) {
		if (!colaborador.isAtivo()) {
			colaborador.alterarAtivo(true, autor, agora);
			auditoria.registrar(EventoAuditoria.alteracao(Colaboradores.ENTIDADE, colaborador.getId(), Set.of("ativo")));
		}
	}

	private Colaborador buscarColaborador(UUID id) {
		return colaboradores.findById(id)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Colaborador não encontrado."));
	}

	private UUID autor() {
		return identidadeAtual.obter().map(Identidade::id).orElse(null);
	}

}
