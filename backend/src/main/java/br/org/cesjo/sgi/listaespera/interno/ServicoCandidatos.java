package br.org.cesjo.sgi.listaespera.interno;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import br.org.cesjo.sgi.listaespera.CandidatoAtivo;
import br.org.cesjo.sgi.listaespera.DadosInscricao;
import br.org.cesjo.sgi.listaespera.ListaEspera;
import br.org.cesjo.sgi.listaespera.MotivoSaida;
import br.org.cesjo.sgi.listaespera.SituacaoInscricao;
import br.org.cesjo.sgi.pessoas.Pessoas;

/** Implementação da API pública {@link ListaEspera} (ADR 0049 e ADR 0081). */
@Service
class ServicoCandidatos implements ListaEspera {

	private final InscricaoRepositorio inscricoes;

	private final HistoricoSituacaoRepositorio historicos;

	private final Pessoas pessoas;

	private final Auditoria auditoria;

	private final IdentidadeAtual identidadeAtual;

	private final ContagensListaEspera contagens;

	private final Clock relogio = Clock.systemUTC();

	ServicoCandidatos(InscricaoRepositorio inscricoes, HistoricoSituacaoRepositorio historicos, Pessoas pessoas,
			Auditoria auditoria, IdentidadeAtual identidadeAtual, ContagensListaEspera contagens) {
		this.inscricoes = inscricoes;
		this.contagens = contagens;
		this.historicos = historicos;
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
					EventoAuditoria.alteracao(ENTIDADE, inscricao.getId(), Set.of("situacao")));
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<DadosInscricao> obter(UUID inscricaoId) {
		return inscricoes.findById(inscricaoId).map(ServicoCandidatos::dados);
	}

	@Override
	@Transactional
	public DadosInscricao registrarInsercao(UUID inscricaoId, LocalDate dataInsercao) {
		InscricaoListaEspera inscricao = inscricoes.findById(inscricaoId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Inscrição não encontrada."));
		SituacaoInscricao anterior = inscricao.getSituacao();
		if (!anterior.ativa()) {
			throw new RegraNegocioException("Só um candidato que está na fila pode ser inserido no serviço.");
		}
		UUID autor = identidadeAtual.obter().map(Identidade::id).orElse(null);
		Instant agora = relogio.instant();
		inscricao.registrarInsercao(autor, agora);
		// saveAndFlush: a versão (@Version) é conferida agora, e duas inserções simultâneas da mesma
		// inscrição não passam as duas.
		inscricoes.saveAndFlush(inscricao);
		auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE, inscricaoId, Set.of("situacao")));
		HistoricoSituacao historico = historicos.save(new HistoricoSituacao(inscricaoId, anterior,
				SituacaoInscricao.INSERIDO, dataInsercao, null, null, autor, agora));
		auditoria.registrar(EventoAuditoria.criacao(ServicoListaEspera.ENTIDADE_HISTORICO, historico.getId()));
		return dados(inscricao);
	}

	@Override
	@Transactional(readOnly = true)
	public long contarInscricoes(LocalDate inicio, LocalDate fim) {
		exigirPeriodo(inicio, fim);
		return contagens.inscricoes(inicio, fim);
	}

	@Override
	@Transactional(readOnly = true)
	public long contarNaFilaEm(LocalDate dia) {
		return contagens.naFilaEm(Objects.requireNonNull(dia, "dia"));
	}

	@Override
	@Transactional(readOnly = true)
	public Map<MotivoSaida, Long> contarSaidasPorMotivo(LocalDate inicio, LocalDate fim) {
		exigirPeriodo(inicio, fim);
		return contagens.saidasPorMotivo(inicio, fim);
	}

	private static void exigirPeriodo(LocalDate inicio, LocalDate fim) {
		if (inicio == null || fim == null || fim.isBefore(inicio)) {
			throw new IllegalArgumentException("Período inválido: " + inicio + " a " + fim);
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

	private static DadosInscricao dados(InscricaoListaEspera inscricao) {
		return new DadosInscricao(inscricao.getId(), inscricao.getPessoaId(), inscricao.getDataCadastro(),
				inscricao.getOrigemEncaminhamento(), inscricao.getSituacao());
	}

}
