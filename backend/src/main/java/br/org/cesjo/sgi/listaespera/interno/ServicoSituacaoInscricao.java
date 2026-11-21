package br.org.cesjo.sgi.listaespera.interno;

import java.time.Clock;
import java.util.Optional;
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
import br.org.cesjo.sgi.listaespera.DadosInscricao;
import br.org.cesjo.sgi.listaespera.ListaEspera;
import br.org.cesjo.sgi.listaespera.SituacaoInscricao;

/** Implementação de {@link ListaEspera} para os outros módulos (ADR 0081). */
@Service
class ServicoSituacaoInscricao implements ListaEspera {

	private final InscricaoRepositorio inscricoes;

	private final Auditoria auditoria;

	private final IdentidadeAtual identidadeAtual;

	private final Clock relogio = Clock.systemUTC();

	ServicoSituacaoInscricao(InscricaoRepositorio inscricoes, Auditoria auditoria, IdentidadeAtual identidadeAtual) {
		this.inscricoes = inscricoes;
		this.auditoria = auditoria;
		this.identidadeAtual = identidadeAtual;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<DadosInscricao> obter(UUID inscricaoId) {
		return inscricoes.findById(inscricaoId).map(ServicoSituacaoInscricao::dados);
	}

	@Override
	@Transactional
	public DadosInscricao registrarInsercao(UUID inscricaoId) {
		InscricaoListaEspera inscricao = inscricoes.findById(inscricaoId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Inscrição não encontrada."));
		if (inscricao.getSituacao() != SituacaoInscricao.AGUARDANDO) {
			throw new RegraNegocioException("Só um candidato que está aguardando na fila pode ser inserido no serviço.");
		}
		inscricao.registrarInsercao(identidadeAtual.obter().map(Identidade::id).orElse(null), relogio.instant());
		// saveAndFlush: a versão (@Version) é conferida agora, e duas inserções simultâneas da mesma
		// inscrição não passam as duas.
		inscricoes.saveAndFlush(inscricao);
		auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE, inscricaoId, Set.of("situacao")));
		return dados(inscricao);
	}

	private static DadosInscricao dados(InscricaoListaEspera inscricao) {
		return new DadosInscricao(inscricao.getId(), inscricao.getPessoaId(), inscricao.getDataCadastro(),
				inscricao.getOrigemEncaminhamento(), inscricao.getSituacao());
	}

}
