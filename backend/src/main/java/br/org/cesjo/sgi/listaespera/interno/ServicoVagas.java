package br.org.cesjo.sgi.listaespera.interno;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.seguranca.Identidade;
import br.org.cesjo.sgi.compartilhado.seguranca.IdentidadeAtual;
import br.org.cesjo.sgi.listaespera.OcupacaoDeVagas;
import br.org.cesjo.sgi.listaespera.SituacaoInscricao;

/** Capacidade do serviço e vagas disponíveis (RF003, ADR 0092 e ADR 0093). */
@Service
class ServicoVagas {

	static final String ENTIDADE = "capacidade_servico";

	private final CapacidadeServicoRepositorio capacidades;

	private final InscricaoRepositorio inscricoes;

	private final OcupacaoDeVagas ocupacao;

	private final Auditoria auditoria;

	private final IdentidadeAtual identidadeAtual;

	private final Clock relogio = Clock.systemUTC();

	ServicoVagas(CapacidadeServicoRepositorio capacidades, InscricaoRepositorio inscricoes, OcupacaoDeVagas ocupacao,
			Auditoria auditoria, IdentidadeAtual identidadeAtual) {
		this.capacidades = capacidades;
		this.inscricoes = inscricoes;
		this.ocupacao = ocupacao;
		this.auditoria = auditoria;
		this.identidadeAtual = identidadeAtual;
	}

	@Transactional(readOnly = true)
	PainelVagas painel() {
		Optional<CapacidadeServico> vigente = capacidades.findFirstByOrderByDefinidaEmDescIdDesc();
		long ocupadas = ocupacao.vagasOcupadas();
		// PENDENTE-CESJO(P3): capacidade total do serviço, sem divisão por turno (ADR 0092).
		long disponiveis = vigente.map(c -> Math.max(0, c.getVagas() - ocupadas)).orElse(0L);
		return new PainelVagas(vigente.map(CapacidadeServico::getVagas).orElse(null), ocupadas, disponiveis,
				inscricoes.countBySituacaoIn(SituacaoInscricao.ATIVAS),
				vigente.map(CapacidadeServico::getDefinidaEm).orElse(null));
	}

	/** Grava a capacidade nova e devolve o painel atualizado. */
	@Transactional
	PainelVagas definirCapacidade(int vagas) {
		CapacidadeServico capacidade = capacidades.save(new CapacidadeServico(vagas,
				identidadeAtual.obter().map(Identidade::id).orElse(null),
				relogio.instant().truncatedTo(ChronoUnit.MICROS)));
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE, capacidade.getId()));
		return painel();
	}

}
