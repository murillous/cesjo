package br.org.cesjo.sgi.atendidos.interno;

import java.text.Collator;
import java.time.Clock;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.atendidos.PerfilAtendido;
import br.org.cesjo.sgi.atendidos.SituacaoInsercao;
import br.org.cesjo.sgi.atendidos.Turno;
import br.org.cesjo.sgi.atendidos.UsuarioAtivo;
import br.org.cesjo.sgi.atendidos.UsuariosAtendidos;
import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.seguranca.Identidade;
import br.org.cesjo.sgi.compartilhado.seguranca.IdentidadeAtual;
import br.org.cesjo.sgi.compartilhado.web.RecursoNaoEncontradoException;
import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;
import br.org.cesjo.sgi.listaespera.DadosInscricao;
import br.org.cesjo.sgi.listaespera.ListaEspera;
import br.org.cesjo.sgi.listaespera.SituacaoInscricao;
import br.org.cesjo.sgi.pessoas.DadosPessoa;
import br.org.cesjo.sgi.pessoas.DadosResponsavel;
import br.org.cesjo.sgi.pessoas.Pessoas;

/** Inserção de candidato da fila no serviço (RF007, RF003, ADR 0080 e ADR 0081). */
@Service
class ServicoInsercao implements UsuariosAtendidos {

	private final InsercaoRepositorio insercoes;

	private final ListaEspera listaEspera;

	private final Pessoas pessoas;

	private final Auditoria auditoria;

	private final IdentidadeAtual identidadeAtual;

	private final ConsultaAtendidos consulta;

	private final Clock relogio = Clock.systemUTC();

	ServicoInsercao(InsercaoRepositorio insercoes, ListaEspera listaEspera, Pessoas pessoas, Auditoria auditoria,
			IdentidadeAtual identidadeAtual, ConsultaAtendidos consulta) {
		this.insercoes = insercoes;
		this.consulta = consulta;
		this.listaEspera = listaEspera;
		this.pessoas = pessoas;
		this.auditoria = auditoria;
		this.identidadeAtual = identidadeAtual;
	}

	@Transactional(readOnly = true)
	CandidatoInsercao candidato(UUID inscricaoId) {
		DadosInscricao inscricao = inscricao(inscricaoId);
		DadosPessoa pessoa = pessoa(inscricao.pessoaId());
		List<CandidatoInsercao.Responsavel> responsaveis = pessoas.responsaveis(pessoa.id())
			.stream()
			.map(r -> new CandidatoInsercao.Responsavel(r.id(), r.nomeCompleto(), r.principal()))
			.toList();
		return new CandidatoInsercao(inscricao.id(), pessoa.id(), pessoa.nomeCompleto(), pessoa.tipoDeficiencia(),
				inscricao.dataCadastro(), inscricao.situacao(), responsaveis);
	}

	/**
	 * Insere no serviço, numa transação só: tira a inscrição da fila, cadastra o cuidador novo quando
	 * houver e cria a inserção ativa.
	 */
	@Transactional
	InsercaoResposta inserir(NovaInsercao nova) {
		DadosInscricao inscricao = inscricao(nova.inscricaoId());
		if (!inscricao.situacao().ativa()) {
			throw new RegraNegocioException("Só um candidato que está na fila pode ser inserido no serviço.");
		}
		if (nova.dataInsercao().isBefore(inscricao.dataCadastro())) {
			throw new RegraNegocioException("A data de inserção não pode ser anterior à data de cadastro na fila.");
		}
		UUID pessoaId = inscricao.pessoaId();
		if (insercoes.existsByPessoaIdAndSituacao(pessoaId, SituacaoInsercao.ATIVA)) {
			throw new RegraNegocioException("Esta pessoa já é usuária ativa do serviço.");
		}
		DadosResponsavel cuidador = cuidador(pessoaId, nova);

		listaEspera.registrarInsercao(inscricao.id(), nova.dataInsercao());
		UUID autor = identidadeAtual.obter().map(Identidade::id).orElse(null);
		Insercao insercao = new Insercao(pessoaId, inscricao.id(), nova.dataInsercao(), nova.turno(), cuidador.id(),
				autor, relogio.instant());
		try {
			insercoes.saveAndFlush(insercao);
		}
		catch (DataIntegrityViolationException erro) {
			// Duas inserções simultâneas da mesma pessoa: o índice único barra a segunda.
			throw new RegraNegocioException("Esta pessoa já é usuária ativa do serviço.");
		}
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE, insercao.getId()));
		return new InsercaoResposta(insercao.getId(), pessoaId, inscricao.id(), pessoa(pessoaId).nomeCompleto(),
				insercao.getDataInsercao(), insercao.getTurno(), cuidador.id(), cuidador.nomeCompleto(),
				insercao.getSituacao());
	}

	@Override
	@Transactional(readOnly = true)
	public long contarAtivos() {
		return insercoes.countBySituacao(SituacaoInsercao.ATIVA);
	}

	@Override
	@Transactional(readOnly = true)
	public Map<Turno, Long> contarAtivosPorTurno() {
		Map<Turno, Long> contagem = new EnumMap<>(Turno.class);
		for (Turno turno : Turno.values()) {
			contagem.put(turno, 0L);
		}
		for (Object[] linha : insercoes.contarPorTurno(SituacaoInsercao.ATIVA)) {
			contagem.put((Turno) linha[0], (Long) linha[1]);
		}
		return contagem;
	}

	@Override
	@Transactional(readOnly = true)
	public List<UsuarioAtivo> listarAtivos() {
		List<UUID> pessoaIds = insercoes.findBySituacao(SituacaoInsercao.ATIVA).stream().map(Insercao::getPessoaId).toList();
		Map<UUID, String> nomes = pessoas.nomes(pessoaIds);
		Collator ordem = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
		return pessoaIds.stream()
			.filter(nomes::containsKey)
			.map(id -> new UsuarioAtivo(id, nomes.get(id)))
			.sorted(Comparator.comparing(UsuarioAtivo::nomeCompleto, ordem))
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public void exigirAtivos(Collection<UUID> pessoaIds) {
		Set<UUID> distintas = new HashSet<>(pessoaIds);
		if (!distintas.isEmpty() && insercoes.contarPessoas(distintas, SituacaoInsercao.ATIVA) != distintas.size()) {
			throw new RegraNegocioException("Escolha somente usuários atendidos ativos.");
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<PerfilAtendido> perfisDosAtivos() {
		return consulta.perfisDosAtivos();
	}

	private DadosResponsavel cuidador(UUID pessoaId, NovaInsercao nova) {
		if (nova.cuidadorResponsavelId() == null) {
			return pessoas.cadastrarResponsavel(pessoaId, nova.nomeNovoCuidador());
		}
		return pessoas.responsaveis(pessoaId)
			.stream()
			.filter(responsavel -> responsavel.id().equals(nova.cuidadorResponsavelId()))
			.findFirst()
			.orElseThrow(() -> new RegraNegocioException("O cuidador escolhido não é responsável por esta pessoa."));
	}

	private DadosInscricao inscricao(UUID inscricaoId) {
		return listaEspera.obter(inscricaoId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Inscrição não encontrada."));
	}

	private DadosPessoa pessoa(UUID pessoaId) {
		return pessoas.obter(pessoaId)
			.orElseThrow(() -> new IllegalStateException("Inscrição sem pessoa: " + pessoaId));
	}

}
