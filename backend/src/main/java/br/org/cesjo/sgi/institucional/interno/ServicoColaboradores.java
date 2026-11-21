package br.org.cesjo.sgi.institucional.interno;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.acesso.Contas;
import br.org.cesjo.sgi.acesso.ResumoConta;
import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.seguranca.Identidade;
import br.org.cesjo.sgi.compartilhado.seguranca.IdentidadeAtual;
import br.org.cesjo.sgi.compartilhado.web.RecursoNaoEncontradoException;
import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;
import br.org.cesjo.sgi.institucional.ColaboradorIndisponivelException;
import br.org.cesjo.sgi.institucional.Colaboradores;
import br.org.cesjo.sgi.institucional.ResumoColaborador;

/**
 * Cadastro dos colaboradores (RF014 parcial, ADR 0045) e implementação da API pública
 * {@link Colaboradores}. A ligação com a conta de acesso segue o ADR 0046.
 */
@Service
class ServicoColaboradores implements Colaboradores {

	private final ColaboradorRepositorio colaboradores;

	private final Contas contas;

	private final Auditoria auditoria;

	private final IdentidadeAtual identidadeAtual;

	private final Clock relogio = Clock.systemUTC();

	ServicoColaboradores(ColaboradorRepositorio colaboradores, Contas contas, Auditoria auditoria,
			IdentidadeAtual identidadeAtual) {
		this.colaboradores = colaboradores;
		this.contas = contas;
		this.auditoria = auditoria;
		this.identidadeAtual = identidadeAtual;
	}

	// API pública

	@Override
	@Transactional(readOnly = true)
	public List<ResumoColaborador> listarAtivos() {
		return colaboradores.findByAtivoTrueOrderByNomeAscIdAsc().stream().map(Colaborador::resumo).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ResumoColaborador> obterAtivo(UUID id) {
		return id == null ? Optional.empty() : colaboradores.findByIdAndAtivoTrue(id).map(Colaborador::resumo);
	}

	@Override
	@Transactional(readOnly = true)
	public void exigirAtivos(Collection<UUID> ids) {
		Set<UUID> distintos = new LinkedHashSet<>(ids);
		if (distintos.contains(null) || colaboradores.countByIdInAndAtivoTrue(distintos) != distintos.size()) {
			throw new ColaboradorIndisponivelException();
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ResumoColaborador> obterAtivoPorConta(UUID contaId) {
		return contaId == null ? Optional.empty()
				: colaboradores.findByContaAcessoIdAndAtivoTrue(contaId).map(Colaborador::resumo);
	}

	// Cadastro, usado pela API HTTP

	/** Lista paginada; {@code ativo} nulo traz todos. */
	@Transactional(readOnly = true)
	Page<ColaboradorResposta> listar(Boolean ativo, Pageable paginacao) {
		Page<Colaborador> pagina = ativo == null ? colaboradores.findAll(paginacao)
				: colaboradores.findByAtivo(ativo, paginacao);
		return pagina.map(this::resposta);
	}

	@Transactional(readOnly = true)
	ColaboradorResposta obter(UUID id) {
		return resposta(buscar(id));
	}

	@Transactional
	ColaboradorResposta criar(NovoColaborador novo) {
		exigirContaLivre(novo.contaId(), null);
		Colaborador colaborador = colaboradores
			.save(new Colaborador(novo.nome(), novo.funcao(), novo.contaId(), autor(), relogio.instant()));
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE, colaborador.getId()));
		return resposta(colaborador);
	}

	/** Altera os campos e registra na auditoria só os que mudaram de fato. */
	@Transactional
	ColaboradorResposta editar(UUID id, EdicaoColaborador edicao) {
		Colaborador colaborador = buscar(id);
		UUID autor = autor();
		Instant agora = relogio.instant();
		Set<String> alterados = new LinkedHashSet<>();
		if (!colaborador.getNome().equals(edicao.nome().strip())) {
			colaborador.alterarNome(edicao.nome(), autor, agora);
			alterados.add("nome");
		}
		if (colaborador.getFuncao() != edicao.funcao()) {
			colaborador.alterarFuncao(edicao.funcao(), autor, agora);
			alterados.add("funcao");
		}
		if (colaborador.isAtivo() != edicao.ativo()) {
			colaborador.alterarAtivo(edicao.ativo(), autor, agora);
			alterados.add("ativo");
		}
		if (!Objects.equals(colaborador.getContaAcessoId(), edicao.contaId())) {
			exigirContaLivre(edicao.contaId(), colaborador.getId());
			colaborador.ligarConta(edicao.contaId(), autor, agora);
			alterados.add("conta_acesso_id");
		}
		if (!alterados.isEmpty()) {
			auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE, colaborador.getId(), alterados));
		}
		return resposta(colaborador);
	}

	/** A conta precisa existir e não pode estar ligada a outro colaborador (ADR 0046). */
	private void exigirContaLivre(UUID contaId, UUID colaboradorId) {
		if (contaId == null) {
			return;
		}
		if (contas.obter(contaId).isEmpty()) {
			throw new RegraNegocioException("A conta de acesso escolhida não existe.");
		}
		boolean ligadaAOutro = colaboradores.findByContaAcessoId(contaId)
			.filter(outro -> !outro.getId().equals(colaboradorId))
			.isPresent();
		if (ligadaAOutro) {
			throw new RegraNegocioException("Esta conta de acesso já está ligada a outro colaborador.");
		}
	}

	private ColaboradorResposta resposta(Colaborador colaborador) {
		ResumoConta conta = colaborador.getContaAcessoId() == null ? null
				: contas.obter(colaborador.getContaAcessoId()).orElse(null);
		return ColaboradorResposta.de(colaborador, conta);
	}

	private Colaborador buscar(UUID id) {
		return colaboradores.findById(id)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Colaborador não encontrado."));
	}

	private UUID autor() {
		return identidadeAtual.obter().map(Identidade::id).orElse(null);
	}

}
