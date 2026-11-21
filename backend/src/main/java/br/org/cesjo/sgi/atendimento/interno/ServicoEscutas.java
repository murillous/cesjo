package br.org.cesjo.sgi.atendimento.interno;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
import br.org.cesjo.sgi.institucional.Colaboradores;
import br.org.cesjo.sgi.institucional.ResumoColaborador;
import br.org.cesjo.sgi.pessoas.Pessoas;

/** Registro, correção e leitura auditada das escutas (RF005, ADR 0100). */
@Service
class ServicoEscutas {

	static final String ENTIDADE = "escuta";

	private final EscutaRepositorio escutas;

	private final Pessoas pessoas;

	private final Colaboradores colaboradores;

	private final Auditoria auditoria;

	private final IdentidadeAtual identidadeAtual;

	private final Clock relogio = Fuso.RELOGIO;

	ServicoEscutas(EscutaRepositorio escutas, Pessoas pessoas, Colaboradores colaboradores, Auditoria auditoria,
			IdentidadeAtual identidadeAtual) {
		this.escutas = escutas;
		this.pessoas = pessoas;
		this.colaboradores = colaboradores;
		this.auditoria = auditoria;
		this.identidadeAtual = identidadeAtual;
	}

	@Transactional
	EscutaResposta registrar(NovaEscuta nova) {
		if (pessoas.obter(nova.pessoaId()).isEmpty()) {
			throw new RegraNegocioException("A pessoa escolhida não existe.");
		}
		exigirDataValida(nova.dados().dataEscuta());
		colaboradores.exigirAtivos(List.of(nova.dados().profissionalId()));
		Escuta escuta = escutas.save(new Escuta(nova.pessoaId(), nova.dados(), autor(), relogio.instant()));
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE, escuta.getId()));
		return resposta(escuta);
	}

	/** Corrige a escuta. Um profissional trocado precisa estar ativo; o mesmo de antes pode já estar inativo. */
	@Transactional
	EscutaResposta corrigir(UUID id, DadosEscuta dados) {
		Escuta escuta = buscar(id);
		exigirDataValida(dados.dataEscuta());
		if (!escuta.getProfissionalId().equals(dados.profissionalId())) {
			colaboradores.exigirAtivos(List.of(dados.profissionalId()));
		}
		Set<String> alterados = escuta.corrigir(dados, autor(), relogio.instant());
		if (!alterados.isEmpty()) {
			auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE, id, alterados));
		}
		return resposta(escuta);
	}

	/** Escuta completa. Toda leitura vai para o log como leitura sensível (RNF 3.3). */
	@Transactional
	EscutaResposta ler(UUID id) {
		Escuta escuta = buscar(id);
		auditoria.registrar(EventoAuditoria.leituraSensivel(ENTIDADE, id));
		return resposta(escuta);
	}

	/** Escutas da pessoa, da mais recente para a mais antiga, sem os textos. */
	@Transactional(readOnly = true)
	List<ResumoEscuta> listar(UUID pessoaId) {
		List<Escuta> lista = escutas.findByPessoaIdOrderByDataEscutaDescCriadoEmDesc(pessoaId);
		Map<UUID, ResumoColaborador> equipe = colaboradores
			.obterVarios(lista.stream().map(Escuta::getProfissionalId).distinct().toList());
		return lista.stream()
			.map(e -> new ResumoEscuta(e.getId(), e.getPessoaId(), e.getDataEscuta(), equipe.get(e.getProfissionalId())))
			.toList();
	}

	@Transactional(readOnly = true)
	ProfissionaisDaEscuta profissionais() {
		UUID sugerido = identidadeAtual.obter()
			.flatMap(identidade -> colaboradores.obterAtivoPorConta(identidade.id()))
			.map(ResumoColaborador::id)
			.orElse(null);
		return new ProfissionaisDaEscuta(colaboradores.listarAtivos(), sugerido);
	}

	private void exigirDataValida(LocalDate data) {
		if (data.isAfter(LocalDate.now(relogio))) {
			throw new RegraNegocioException("A data da escuta não pode ser futura.");
		}
	}

	private EscutaResposta resposta(Escuta escuta) {
		String nome = pessoas.nomes(List.of(escuta.getPessoaId())).get(escuta.getPessoaId());
		ResumoColaborador profissional = colaboradores.obterVarios(List.of(escuta.getProfissionalId()))
			.get(escuta.getProfissionalId());
		return EscutaResposta.de(escuta, nome, profissional);
	}

	private Escuta buscar(UUID id) {
		return escutas.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Escuta não encontrada."));
	}

	private UUID autor() {
		return identidadeAtual.obter().map(Identidade::id).orElse(null);
	}

}
