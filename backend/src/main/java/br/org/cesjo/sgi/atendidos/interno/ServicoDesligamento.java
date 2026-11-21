package br.org.cesjo.sgi.atendidos.interno;

import java.time.Clock;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.atendidos.SituacaoInsercao;
import br.org.cesjo.sgi.atendidos.UsuarioDesligado;
import br.org.cesjo.sgi.atendidos.UsuariosAtendidos;
import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.seguranca.Identidade;
import br.org.cesjo.sgi.compartilhado.seguranca.IdentidadeAtual;
import br.org.cesjo.sgi.compartilhado.web.RecursoNaoEncontradoException;
import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;

/** Desligamento do usuário atendido (RF008, RF003, ADR 0110 e ADR 0111). */
@Service
class ServicoDesligamento {

	static final String ENTIDADE = "desligamento";

	private final InsercaoRepositorio insercoes;

	private final DesligamentoRepositorio desligamentos;

	private final Auditoria auditoria;

	private final IdentidadeAtual identidadeAtual;

	private final ApplicationEventPublisher eventos;

	private final Clock relogio = Clock.systemUTC();

	ServicoDesligamento(InsercaoRepositorio insercoes, DesligamentoRepositorio desligamentos, Auditoria auditoria,
			IdentidadeAtual identidadeAtual, ApplicationEventPublisher eventos) {
		this.insercoes = insercoes;
		this.desligamentos = desligamentos;
		this.auditoria = auditoria;
		this.identidadeAtual = identidadeAtual;
		this.eventos = eventos;
	}

	/**
	 * Desliga numa transação só: grava o desligamento, muda a inserção para desligada e publica
	 * {@link UsuarioDesligado}. Ouvintes síncronos participam da mesma transação (ADR 0111).
	 */
	@Transactional
	DesligamentoResposta desligar(UUID insercaoId, NovoDesligamento novo) {
		Insercao insercao = insercoes.findById(insercaoId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Usuário atendido não encontrado."));
		if (insercao.getSituacao() != SituacaoInsercao.ATIVA) {
			throw new RegraNegocioException("Este usuário já foi desligado do serviço.");
		}
		if (novo.data().isBefore(insercao.getDataInsercao())) {
			throw new RegraNegocioException("A data de desligamento não pode ser anterior à data de inserção.");
		}
		UUID autor = identidadeAtual.obter().map(Identidade::id).orElse(null);
		var agora = relogio.instant();
		insercao.desligar(autor, agora);
		insercoes.saveAndFlush(insercao);
		Desligamento desligamento = desligamentos.save(new Desligamento(insercaoId, novo.data(), novo.motivo(),
				novo.observacaoNormalizada(), autor, agora));
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE, desligamento.getId()));
		auditoria.registrar(EventoAuditoria.alteracao(UsuariosAtendidos.ENTIDADE, insercaoId, Set.of("situacao")));
		eventos.publishEvent(new UsuarioDesligado(insercaoId, novo.data()));
		return new DesligamentoResposta(desligamento.getId(), insercaoId, desligamento.getData(),
				desligamento.getMotivo(), insercao.getSituacao());
	}

}
