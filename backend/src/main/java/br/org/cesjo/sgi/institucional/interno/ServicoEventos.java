package br.org.cesjo.sgi.institucional.interno;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.seguranca.Identidade;
import br.org.cesjo.sgi.compartilhado.seguranca.IdentidadeAtual;
import br.org.cesjo.sgi.compartilhado.web.RecursoNaoEncontradoException;
import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;
import br.org.cesjo.sgi.institucional.TipoEventoInstitucional;

/** Registro das reuniões e eventos institucionais (RF012, ADR 0175). */
@Service
class ServicoEventos {

	/** Nome da entidade no log de auditoria. */
	static final String ENTIDADE = "evento_institucional";

	private final EventoRepositorio eventos;

	private final ParceiroRepositorio parceiros;

	private final Auditoria auditoria;

	private final IdentidadeAtual identidadeAtual;

	private final Clock relogio = Clock.systemUTC();

	ServicoEventos(EventoRepositorio eventos, ParceiroRepositorio parceiros, Auditoria auditoria,
			IdentidadeAtual identidadeAtual) {
		this.eventos = eventos;
		this.parceiros = parceiros;
		this.auditoria = auditoria;
		this.identidadeAtual = identidadeAtual;
	}

	@Transactional(readOnly = true)
	Page<EventoResposta> listar(TipoEventoInstitucional tipo, LocalDate de, LocalDate ate, UUID parceiroId,
			Pageable paginacao) {
		if (de != null && ate != null && ate.isBefore(de)) {
			throw new RegraNegocioException("O fim do período não pode ser antes do início.");
		}
		Page<EventoInstitucional> pagina = eventos.findAll(EventoRepositorio.filtro(tipo, de, ate, parceiroId),
				paginacao);
		Set<UUID> ids = pagina.stream()
			.map(EventoInstitucional::getParceiroId)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
		Map<UUID, Parceiro> ligados = parceiros.findAllById(ids)
			.stream()
			.collect(Collectors.toMap(Parceiro::getId, Function.identity()));
		return pagina.map(evento -> EventoResposta.de(evento, ligados.get(evento.getParceiroId())));
	}

	@Transactional(readOnly = true)
	EventoResposta obter(UUID id) {
		return resposta(buscar(id));
	}

	@Transactional
	EventoResposta registrar(DadosEvento dados) {
		exigirParceiro(dados.parceiroId());
		EventoInstitucional evento = eventos.save(new EventoInstitucional(dados, autor(), relogio.instant()));
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE, evento.getId()));
		return resposta(evento);
	}

	/** Troca os dados e registra na auditoria só os campos que mudaram de fato. */
	@Transactional
	EventoResposta editar(UUID id, DadosEvento dados) {
		EventoInstitucional evento = buscar(id);
		Set<String> alterados = new LinkedHashSet<>();
		marcar(alterados, "tipo", evento.getTipo(), dados.tipo());
		marcar(alterados, "direcao", evento.getDirecao(), dados.direcao());
		marcar(alterados, "data", evento.getData(), dados.data());
		marcar(alterados, "descricao", evento.getDescricao(), dados.descricao().strip());
		marcar(alterados, "participantes", evento.getParticipantes(), dados.participantesNormalizados());
		marcar(alterados, "parceiro_id", evento.getParceiroId(), dados.parceiroId());
		if (alterados.contains("parceiro_id")) {
			exigirParceiro(dados.parceiroId());
		}
		if (!alterados.isEmpty()) {
			evento.aplicar(dados, autor(), relogio.instant());
			auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE, evento.getId(), alterados));
		}
		return resposta(evento);
	}

	private void exigirParceiro(UUID parceiroId) {
		if (parceiroId != null && !parceiros.existsById(parceiroId)) {
			throw new RegraNegocioException("O parceiro escolhido não existe.");
		}
	}

	private static void marcar(Set<String> alterados, String campo, Object antes, Object depois) {
		if (!Objects.equals(antes, depois)) {
			alterados.add(campo);
		}
	}

	private EventoResposta resposta(EventoInstitucional evento) {
		Parceiro parceiro = evento.getParceiroId() == null ? null
				: parceiros.findById(evento.getParceiroId()).orElse(null);
		return EventoResposta.de(evento, parceiro);
	}

	private EventoInstitucional buscar(UUID id) {
		return eventos.findById(id)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Reunião ou evento não encontrado."));
	}

	private UUID autor() {
		return identidadeAtual.obter().map(Identidade::id).orElse(null);
	}

}
