package br.org.cesjo.sgi.atendimento.interno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;
import br.org.cesjo.sgi.listaespera.ListaEspera;

/** Realização, cancelamento e reagendamento das visitas, e leitura das observações (RF004, ADR 0073). */
@Service
class ServicoResultadoVisitas {

	private static final String ENTIDADE = ServicoVisitas.ENTIDADE;

	private final ServicoVisitas visitas;

	private final VisitaRepositorio repositorio;

	private final ListaEspera listaEspera;

	private final Auditoria auditoria;

	ServicoResultadoVisitas(ServicoVisitas visitas, VisitaRepositorio repositorio, ListaEspera listaEspera,
			Auditoria auditoria) {
		this.visitas = visitas;
		this.repositorio = repositorio;
		this.listaEspera = listaEspera;
		this.auditoria = auditoria;
	}

	/** Só depois do dia da visita começar: visita futura ainda não aconteceu. */
	@Transactional
	VisitaResposta realizar(UUID id, RealizacaoVisita realizacao) {
		VisitaDomiciliar visita = visitas.buscar(id);
		if (visita.getDataHora().toLocalDate().isAfter(LocalDate.now(Fuso.RELOGIO))) {
			throw new RegraNegocioException("Uma visita marcada para um dia futuro ainda não pode ser realizada.");
		}
		String observacoes = semTexto(realizacao.observacoes()) ? null : realizacao.observacoes().strip();
		visita.realizar(observacoes, visitas.autor(), agora());
		auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE, id,
				observacoes == null ? Set.of("status") : Set.of("status", "observacoes")));
		return resposta(visita);
	}

	@Transactional
	VisitaResposta cancelar(UUID id, CancelamentoVisita cancelamento) {
		VisitaDomiciliar visita = visitas.buscar(id);
		visita.cancelar(cancelamento.motivo().strip(), visitas.autor(), agora());
		auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE, id, Set.of("status", "motivo_cancelamento")));
		return resposta(visita);
	}

	/**
	 * Cancela a visita com o motivo e cria a nova, numa transação só. A visita nova segue as regras do
	 * agendamento, inclusive a pessoa ainda estar na lista de espera.
	 */
	@Transactional
	VisitaResposta reagendar(UUID id, ReagendamentoVisita reagendamento) {
		VisitaDomiciliar origem = visitas.buscar(id);
		Set<UUID> profissionais = visitas.exigirAgendavel(reagendamento.dataHora(), reagendamento.profissionais());
		UUID autor = visitas.autor();
		Instant agora = agora();
		origem.cancelar(reagendamento.motivo().strip(), autor, agora);
		listaEspera.colocarEmAvaliacao(origem.getPessoaId());
		VisitaDomiciliar nova = repositorio.save(
				VisitaDomiciliar.reagendamento(origem, reagendamento.dataHora(), profissionais, autor, agora));
		auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE, id, Set.of("status", "motivo_cancelamento")));
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE, nova.getId()));
		return resposta(nova);
	}

	/**
	 * Lê as observações decifradas e registra a leitura de dado sensível (RNF 3.3). A transação não é só
	 * de leitura porque grava o registro de auditoria.
	 */
	@Transactional
	ObservacoesVisita observacoes(UUID id) {
		VisitaDomiciliar visita = visitas.buscar(id);
		auditoria.registrar(EventoAuditoria.leituraSensivel(ENTIDADE, id));
		return new ObservacoesVisita(visita.getObservacoes());
	}

	private VisitaResposta resposta(VisitaDomiciliar visita) {
		return visitas.respostas(List.of(visita)).getFirst();
	}

	private Instant agora() {
		return Fuso.RELOGIO.instant();
	}

	private static boolean semTexto(String texto) {
		return texto == null || texto.isBlank();
	}

}
