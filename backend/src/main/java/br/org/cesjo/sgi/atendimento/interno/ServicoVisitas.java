package br.org.cesjo.sgi.atendimento.interno;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.LinkedHashSet;
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
import br.org.cesjo.sgi.listaespera.CandidatoAtivo;
import br.org.cesjo.sgi.listaespera.ListaEspera;
import br.org.cesjo.sgi.pessoas.Pessoas;

/** Agendamento e agenda das visitas domiciliares (RF004, ADR 0070). */
@Service
class ServicoVisitas {

	static final String ENTIDADE = "visita_domiciliar";

	/** Maior período da agenda numa consulta, em dias (ADR 0070). */
	static final int PERIODO_MAXIMO_DIAS = 62;

	private final VisitaRepositorio visitas;

	private final ListaEspera listaEspera;

	private final Colaboradores colaboradores;

	private final Pessoas pessoas;

	private final Auditoria auditoria;

	private final IdentidadeAtual identidadeAtual;

	private final Clock relogio = Fuso.RELOGIO;

	ServicoVisitas(VisitaRepositorio visitas, ListaEspera listaEspera, Colaboradores colaboradores, Pessoas pessoas,
			Auditoria auditoria, IdentidadeAtual identidadeAtual) {
		this.visitas = visitas;
		this.listaEspera = listaEspera;
		this.colaboradores = colaboradores;
		this.pessoas = pessoas;
		this.auditoria = auditoria;
		this.identidadeAtual = identidadeAtual;
	}

	/**
	 * Agenda a visita e coloca o candidato em avaliação, numa transação só. A pessoa precisa estar na lista
	 * de espera e os profissionais precisam estar ativos.
	 */
	@Transactional
	VisitaResposta agendar(NovaVisita nova) {
		Set<UUID> profissionais = exigirAgendavel(nova.dataHora(), nova.profissionais());
		listaEspera.colocarEmAvaliacao(nova.pessoaId());
		VisitaDomiciliar visita = visitas
			.save(new VisitaDomiciliar(nova.pessoaId(), nova.dataHora(), profissionais, autor(), relogio.instant()));
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE, visita.getId()));
		return respostas(List.of(visita)).getFirst();
	}

	/** Dia que não passou e profissionais ativos (ADR 0070). Devolve os profissionais sem repetição. */
	Set<UUID> exigirAgendavel(java.time.LocalDateTime dataHora, Collection<UUID> profissionais) {
		if (dataHora.toLocalDate().isBefore(LocalDate.now(relogio))) {
			throw new RegraNegocioException("A visita não pode ser agendada para um dia que já passou.");
		}
		Set<UUID> distintos = new LinkedHashSet<>(profissionais);
		colaboradores.exigirAtivos(distintos);
		return distintos;
	}

	/** Agenda de {@code de} a {@code ate}, inclusive, em ordem de horário. */
	@Transactional(readOnly = true)
	List<VisitaResposta> agenda(LocalDate de, LocalDate ate) {
		if (ate.isBefore(de)) {
			throw new RegraNegocioException("A data final do período não pode ser anterior à inicial.");
		}
		if (ChronoUnit.DAYS.between(de, ate) >= PERIODO_MAXIMO_DIAS) {
			throw new RegraNegocioException("Consulte no máximo " + PERIODO_MAXIMO_DIAS + " dias de cada vez.");
		}
		return respostas(visitas.findByDataHoraGreaterThanEqualAndDataHoraLessThanOrderByDataHoraAscIdAsc(
				de.atStartOfDay(), ate.plusDays(1).atStartOfDay()));
	}

	@Transactional(readOnly = true)
	VisitaResposta obter(UUID id) {
		return respostas(List.of(buscar(id))).getFirst();
	}

	@Transactional(readOnly = true)
	List<CandidatoAtivo> candidatos() {
		return listaEspera.listarCandidatosAtivos();
	}

	@Transactional(readOnly = true)
	List<ResumoColaborador> profissionais() {
		return colaboradores.listarAtivos();
	}

	VisitaDomiciliar buscar(UUID id) {
		return visitas.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Visita não encontrada."));
	}

	/** Monta as respostas lendo nomes de pessoas e profissionais de uma vez só. */
	List<VisitaResposta> respostas(Collection<VisitaDomiciliar> lista) {
		Map<UUID, String> nomes = pessoas.nomes(lista.stream().map(VisitaDomiciliar::getPessoaId).distinct().toList());
		Map<UUID, ResumoColaborador> equipe = colaboradores
			.obterVarios(lista.stream().flatMap(v -> v.getProfissionais().stream()).distinct().toList());
		Map<UUID, UUID> reagendamentos = visitas
			.findByVisitaOrigemIdIn(lista.stream().map(VisitaDomiciliar::getId).toList())
			.stream()
			.collect(java.util.stream.Collectors.toMap(VisitaDomiciliar::getVisitaOrigemId, VisitaDomiciliar::getId));
		return lista.stream()
			.map(v -> VisitaResposta.de(v, nomes.get(v.getPessoaId()), equipe, reagendamentos.get(v.getId())))
			.toList();
	}

	UUID autor() {
		return identidadeAtual.obter().map(Identidade::id).orElse(null);
	}

}
