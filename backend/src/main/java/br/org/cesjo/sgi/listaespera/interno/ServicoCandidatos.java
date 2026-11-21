package br.org.cesjo.sgi.listaespera.interno;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.seguranca.Identidade;
import br.org.cesjo.sgi.compartilhado.seguranca.IdentidadeAtual;
import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;
import br.org.cesjo.sgi.listaespera.CandidatoAtivo;
import br.org.cesjo.sgi.listaespera.ListaEspera;
import br.org.cesjo.sgi.listaespera.SituacaoInscricao;
import br.org.cesjo.sgi.pessoas.Pessoas;

/** Implementação da API pública {@link ListaEspera} (ADR 0049). */
@Service
class ServicoCandidatos implements ListaEspera {

	private final InscricaoRepositorio inscricoes;

	private final Pessoas pessoas;

	private final Auditoria auditoria;

	private final IdentidadeAtual identidadeAtual;

	private final Clock relogio = Clock.systemUTC();

	ServicoCandidatos(InscricaoRepositorio inscricoes, Pessoas pessoas, Auditoria auditoria,
			IdentidadeAtual identidadeAtual) {
		this.inscricoes = inscricoes;
		this.pessoas = pessoas;
		this.auditoria = auditoria;
		this.identidadeAtual = identidadeAtual;
	}

	@Override
	@Transactional(readOnly = true)
	public List<CandidatoAtivo> listarCandidatosAtivos() {
		List<InscricaoListaEspera> ativas = inscricoes
			.findBySituacaoInOrderByDataCadastroAscCriadoEmAsc(SituacaoInscricao.ATIVAS);
		Map<UUID, String> nomes = pessoas.nomes(ativas.stream().map(InscricaoListaEspera::getPessoaId).toList());
		return ativas.stream().map(inscricao -> candidato(inscricao, nomes.get(inscricao.getPessoaId()))).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<CandidatoAtivo> candidatoAtivo(UUID pessoaId) {
		return ativa(pessoaId).map(inscricao -> candidato(inscricao,
				pessoas.nomes(List.of(pessoaId)).get(pessoaId)));
	}

	@Override
	@Transactional
	public void colocarEmAvaliacao(UUID pessoaId) {
		InscricaoListaEspera inscricao = ativa(pessoaId)
			.orElseThrow(() -> new RegraNegocioException("A pessoa escolhida não está na lista de espera."));
		UUID autor = identidadeAtual.obter().map(Identidade::id).orElse(null);
		if (inscricao.colocarEmAvaliacao(autor, relogio.instant())) {
			auditoria.registrar(
					EventoAuditoria.alteracao(ServicoListaEspera.ENTIDADE, inscricao.getId(), Set.of("situacao")));
		}
	}

	private Optional<InscricaoListaEspera> ativa(UUID pessoaId) {
		return pessoaId == null ? Optional.empty()
				: inscricoes.findFirstByPessoaIdAndSituacaoInOrderByDataCadastroAsc(pessoaId, SituacaoInscricao.ATIVAS);
	}

	private static CandidatoAtivo candidato(InscricaoListaEspera inscricao, String nome) {
		return new CandidatoAtivo(inscricao.getId(), inscricao.getPessoaId(), nome, inscricao.getDataCadastro(),
				inscricao.getSituacao());
	}

}
