package br.org.cesjo.sgi.atividades.interno;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.atendidos.UsuariosAtendidos;
import br.org.cesjo.sgi.atividades.Atividades;
import br.org.cesjo.sgi.atividades.TipoAtividade;
import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.seguranca.Identidade;
import br.org.cesjo.sgi.compartilhado.seguranca.IdentidadeAtual;
import br.org.cesjo.sgi.compartilhado.web.RecursoNaoEncontradoException;
import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;
import br.org.cesjo.sgi.institucional.Colaboradores;
import br.org.cesjo.sgi.institucional.ResumoColaborador;
import br.org.cesjo.sgi.pessoas.Pessoas;

/** Registro, correção, leitura e contagem das atividades (RF010, ADR 0150 e ADR 0151). */
@Service
class ServicoAtividades implements Atividades {

	private final AtividadeRepositorio atividades;

	private final UsuariosAtendidos usuariosAtendidos;

	private final Pessoas pessoas;

	private final Colaboradores colaboradores;

	private final Auditoria auditoria;

	private final IdentidadeAtual identidadeAtual;

	private final Clock relogio = Fuso.RELOGIO;

	ServicoAtividades(AtividadeRepositorio atividades, UsuariosAtendidos usuariosAtendidos, Pessoas pessoas,
			Colaboradores colaboradores, Auditoria auditoria, IdentidadeAtual identidadeAtual) {
		this.atividades = atividades;
		this.usuariosAtendidos = usuariosAtendidos;
		this.pessoas = pessoas;
		this.colaboradores = colaboradores;
		this.auditoria = auditoria;
		this.identidadeAtual = identidadeAtual;
	}

	/** Registra a atividade. Participantes precisam ser usuários ativos; profissionais, colaboradores ativos. */
	@Transactional
	AtividadeResposta registrar(DadosAtividade dados) {
		exigirDataValida(dados.data());
		usuariosAtendidos.exigirAtivos(dados.participantes());
		colaboradores.exigirAtivos(dados.profissionais());
		Atividade atividade = atividades.save(new Atividade(dados, autor(), relogio.instant()));
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE, atividade.getId()));
		return resposta(atividade);
	}

	/**
	 * Corrige a atividade. Só os participantes e profissionais acrescentados precisam estar ativos; os que
	 * já estavam continuam mesmo se o usuário foi desligado ou o colaborador ficou inativo.
	 */
	@Transactional
	AtividadeResposta corrigir(UUID id, DadosAtividade dados) {
		Atividade atividade = buscar(id);
		exigirDataValida(dados.data());
		usuariosAtendidos.exigirAtivos(novos(dados.participantes(), atividade.getParticipantes()));
		colaboradores.exigirAtivos(novos(dados.profissionais(), atividade.getProfissionais()));
		Set<String> alterados = atividade.corrigir(dados, autor(), relogio.instant());
		if (!alterados.isEmpty()) {
			auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE, id, alterados));
		}
		return resposta(atividade);
	}

	@Transactional(readOnly = true)
	AtividadeResposta ler(UUID id) {
		return resposta(buscar(id));
	}

	/** Atividades da mais recente para a mais antiga, sem a descrição. */
	@Transactional(readOnly = true)
	Page<ResumoAtividade> listar(Pageable pagina) {
		Page<Atividade> encontradas = atividades.findAll(pagina);
		Map<UUID, String> nomes = pessoas
			.nomes(encontradas.stream().flatMap(a -> a.getParticipantes().stream()).distinct().toList());
		Map<UUID, ResumoColaborador> equipe = colaboradores
			.obterVarios(encontradas.stream().flatMap(a -> a.getProfissionais().stream()).distinct().toList());
		return encontradas.map(a -> new ResumoAtividade(a.getId(), a.getTipo(), a.getData(), a.getTitulo(),
				participantes(a, nomes), profissionais(a, equipe)));
	}

	@Transactional(readOnly = true)
	OpcoesAtividade opcoes() {
		UUID sugerido = identidadeAtual.obter()
			.flatMap(identidade -> colaboradores.obterAtivoPorConta(identidade.id()))
			.map(ResumoColaborador::id)
			.orElse(null);
		return new OpcoesAtividade(usuariosAtendidos.listarAtivos(), colaboradores.listarAtivos(), sugerido);
	}

	@Override
	@Transactional(readOnly = true)
	public Map<TipoAtividade, Long> contarPorTipo(LocalDate inicio, LocalDate fim) {
		if (fim.isBefore(inicio)) {
			throw new IllegalArgumentException("O fim do período não pode ser anterior ao início.");
		}
		Map<TipoAtividade, Long> contagem = new EnumMap<>(TipoAtividade.class);
		for (TipoAtividade tipo : TipoAtividade.values()) {
			contagem.put(tipo, 0L);
		}
		for (Object[] linha : atividades.contarPorTipo(inicio, fim)) {
			contagem.put((TipoAtividade) linha[0], (Long) linha[1]);
		}
		return contagem;
	}

	// PENDENTE-CESJO(P24): só se registra atividade já realizada; a data não pode ser futura, e não há agenda
	// de atividades planejadas (ADR 0150).
	private void exigirDataValida(LocalDate data) {
		if (data.isAfter(LocalDate.now(relogio))) {
			throw new RegraNegocioException("A data da atividade não pode ser futura.");
		}
	}

	private AtividadeResposta resposta(Atividade atividade) {
		Map<UUID, String> nomes = pessoas.nomes(atividade.getParticipantes());
		Map<UUID, ResumoColaborador> equipe = colaboradores.obterVarios(atividade.getProfissionais());
		return new AtividadeResposta(atividade.getId(), atividade.getTipo(), atividade.getData(), atividade.getTitulo(),
				atividade.getDescricao(), participantes(atividade, nomes), profissionais(atividade, equipe),
				atividade.getCriadoEm(), atividade.getAtualizadoEm());
	}

	/** Participantes em ordem de nome. */
	private static List<Participante> participantes(Atividade atividade, Map<UUID, String> nomes) {
		return atividade.getParticipantes()
			.stream()
			.filter(nomes::containsKey)
			.map(id -> new Participante(id, nomes.get(id)))
			.sorted((a, b) -> a.nomeCompleto().compareToIgnoreCase(b.nomeCompleto()))
			.toList();
	}

	/** Profissionais em ordem de nome. */
	private static List<ResumoColaborador> profissionais(Atividade atividade, Map<UUID, ResumoColaborador> equipe) {
		return atividade.getProfissionais()
			.stream()
			.map(equipe::get)
			.filter(p -> p != null)
			.sorted((a, b) -> a.nome().compareToIgnoreCase(b.nome()))
			.toList();
	}

	private static Set<UUID> novos(Collection<UUID> escolhidos, Set<UUID> anteriores) {
		Set<UUID> novos = new HashSet<>(escolhidos);
		novos.removeAll(anteriores);
		return novos;
	}

	private Atividade buscar(UUID id) {
		return atividades.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Atividade não encontrada."));
	}

	private UUID autor() {
		return identidadeAtual.obter().map(Identidade::id).orElse(null);
	}

}
