package br.org.cesjo.sgi.atendidos.interno;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.seguranca.Identidade;
import br.org.cesjo.sgi.compartilhado.seguranca.IdentidadeAtual;
import br.org.cesjo.sgi.compartilhado.web.RecursoNaoEncontradoException;

/** Registro das violações de direitos (RF009, ADR 0112). */
@Service
class ServicoViolacoes {

	static final String ENTIDADE = "violacao_direito";

	private final InsercaoRepositorio insercoes;

	private final ViolacaoRepositorio violacoes;

	private final Auditoria auditoria;

	private final IdentidadeAtual identidadeAtual;

	private final Clock relogio = Clock.systemUTC();

	ServicoViolacoes(InsercaoRepositorio insercoes, ViolacaoRepositorio violacoes, Auditoria auditoria,
			IdentidadeAtual identidadeAtual) {
		this.insercoes = insercoes;
		this.violacoes = violacoes;
		this.auditoria = auditoria;
		this.identidadeAtual = identidadeAtual;
	}

	/** Registra para a pessoa do usuário atendido, ativo ou desligado. */
	@Transactional
	ViolacaoResposta registrar(UUID insercaoId, NovaViolacao nova) {
		Insercao insercao = insercoes.findById(insercaoId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Usuário atendido não encontrado."));
		UUID autor = identidadeAtual.obter().map(Identidade::id).orElse(null);
		ViolacaoDireito violacao = violacoes.save(new ViolacaoDireito(insercao.getPessoaId(), nova.tipo(),
				nova.ambito(), nova.dataIdentificacao(), nova.descricao().strip(), autor, relogio.instant()));
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE, violacao.getId()));
		return new ViolacaoResposta(violacao.getId(), insercaoId, violacao.getTipo(), violacao.getAmbito(),
				violacao.getDataIdentificacao());
	}

}
