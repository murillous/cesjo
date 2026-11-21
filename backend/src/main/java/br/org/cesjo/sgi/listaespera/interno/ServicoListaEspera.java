package br.org.cesjo.sgi.listaespera.interno;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.seguranca.Identidade;
import br.org.cesjo.sgi.compartilhado.seguranca.IdentidadeAtual;
import br.org.cesjo.sgi.compartilhado.web.RecursoNaoEncontradoException;
import br.org.cesjo.sgi.pessoas.DadosPessoa;
import br.org.cesjo.sgi.pessoas.NovaPessoa;
import br.org.cesjo.sgi.pessoas.Pessoas;

/** Cadastro e consulta das inscrições na lista de espera (RF001, RF002). */
@Service
class ServicoListaEspera {

	static final String ENTIDADE = "inscricao_lista_espera";

	private final InscricaoRepositorio inscricoes;

	private final Pessoas pessoas;

	private final Auditoria auditoria;

	private final IdentidadeAtual identidadeAtual;

	private final Clock relogio = Clock.systemUTC();

	ServicoListaEspera(InscricaoRepositorio inscricoes, Pessoas pessoas, Auditoria auditoria,
			IdentidadeAtual identidadeAtual) {
		this.inscricoes = inscricoes;
		this.pessoas = pessoas;
		this.auditoria = auditoria;
		this.identidadeAtual = identidadeAtual;
	}

	/** Cadastra a pessoa e a inscreve na fila, com situação aguardando, numa transação só. */
	@Transactional
	InscricaoResposta inscrever(NovaInscricao nova) {
		UUID pessoaId = pessoas.cadastrar(new NovaPessoa(nova.nomeCompleto(), nova.dataNascimento(),
				nova.tipoDeficiencia(), nova.telefone(), nova.nomeResponsavel()));
		UUID autor = identidadeAtual.obter().map(Identidade::id).orElse(null);
		Instant agora = relogio.instant();
		InscricaoListaEspera inscricao = inscricoes
			.save(new InscricaoListaEspera(pessoaId, nova.dataCadastro(), nova.origemEncaminhamento(), autor, agora));
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE, inscricao.getId()));
		return InscricaoResposta.de(inscricao, pessoa(pessoaId));
	}

	@Transactional(readOnly = true)
	InscricaoResposta obter(UUID id) {
		InscricaoListaEspera inscricao = inscricoes.findById(id)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Inscrição não encontrada."));
		return InscricaoResposta.de(inscricao, pessoa(inscricao.getPessoaId()));
	}

	private DadosPessoa pessoa(UUID pessoaId) {
		return pessoas.obter(pessoaId)
			.orElseThrow(() -> new IllegalStateException("Inscrição sem pessoa: " + pessoaId));
	}

}
