package br.org.cesjo.sgi.atendidos.interno;

import java.time.Clock;
import java.util.List;
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

/** Registro e consulta das violações de direitos (RF009, ADR 0112 e ADR 0145). */
@Service
class ServicoViolacoes {

	static final String ENTIDADE = "violacao_direito";

	private final InsercaoRepositorio insercoes;

	private final ViolacaoRepositorio violacoes;

	private final ConsultaViolacoes consulta;

	private final Auditoria auditoria;

	private final IdentidadeAtual identidadeAtual;

	private final Clock relogio = Clock.systemUTC();

	ServicoViolacoes(InsercaoRepositorio insercoes, ViolacaoRepositorio violacoes, ConsultaViolacoes consulta,
			Auditoria auditoria, IdentidadeAtual identidadeAtual) {
		this.insercoes = insercoes;
		this.violacoes = violacoes;
		this.consulta = consulta;
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

	// PENDENTE-CESJO(P19): as listas mostram categoria e data sem gerar evento de auditoria; só a leitura
	// da descrição é registrada como leitura sensível (ADR 0145).
	/** Violações da pessoa do usuário atendido, de todas as inserções dela, sem a descrição. */
	@Transactional(readOnly = true)
	List<LinhaViolacao> listarDoAtendido(UUID insercaoId, FiltroViolacoes filtro) {
		Insercao insercao = insercoes.findById(insercaoId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Usuário atendido não encontrado."));
		return consulta.daPessoa(insercao.getPessoaId(), filtro);
	}

	/** Violações de todos os usuários atendidos, paginadas, sem a descrição. */
	@Transactional(readOnly = true)
	Page<LinhaViolacao> listar(FiltroViolacoes filtro, Pageable pagina) {
		return consulta.todas(filtro, pagina);
	}

	/** Violação com a descrição. Toda leitura vai para o log como leitura sensível (RNF 3.3). */
	@Transactional
	ViolacaoCompleta ler(UUID id) {
		ViolacaoDireito violacao = violacoes.findById(id)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Violação de direitos não encontrada."));
		auditoria.registrar(EventoAuditoria.leituraSensivel(ENTIDADE, id));
		return ViolacaoCompleta.de(violacao);
	}

}
