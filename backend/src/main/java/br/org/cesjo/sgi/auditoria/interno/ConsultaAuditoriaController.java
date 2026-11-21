package br.org.cesjo.sgi.auditoria.interno;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.org.cesjo.sgi.auditoria.AcaoAuditoria;
import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;

/**
 * Consulta do log de auditoria, só para quem tem {@code AUDITORIA_LER} (Coordenador). Filtros e
 * ordenação conforme o ADR 0024.
 */
@RestController
@RequestMapping("/api/auditoria")
@PreAuthorize("hasAuthority('AUDITORIA_LER')")
@Transactional(readOnly = true)
class ConsultaAuditoriaController {

	/** Mais recente primeiro; o id desempata registros do mesmo instante para a paginação ser estável. */
	private static final Sort ORDEM = Sort.by(Sort.Order.desc("ocorridoEm"), Sort.Order.desc("id"));

	private final RegistroAuditoriaRepositorio registros;

	ConsultaAuditoriaController(RegistroAuditoriaRepositorio registros) {
		this.registros = registros;
	}

	/**
	 * Lista o log paginado. {@code de} é inclusivo e {@code ate} é exclusivo, ambos instantes ISO-8601;
	 * {@code login} compara sem diferenciar maiúsculas; {@code entidade} e {@code acao} são exatos.
	 */
	@GetMapping
	Page<RegistroAuditoriaResposta> listar(@RequestParam(required = false) Instant de,
			@RequestParam(required = false) Instant ate, @RequestParam(required = false) String login,
			@RequestParam(required = false) String entidade, @RequestParam(required = false) AcaoAuditoria acao,
			Pageable pageable) {
		if (de != null && ate != null && !de.isBefore(ate)) {
			throw new RegraNegocioException("O início do período deve ser anterior ao fim.");
		}
		Specification<RegistroAuditoria> filtro = (raiz, consulta, cb) -> {
			List<Predicate> condicoes = new ArrayList<>();
			if (de != null) {
				condicoes.add(cb.greaterThanOrEqualTo(raiz.get("ocorridoEm"), de));
			}
			if (ate != null) {
				condicoes.add(cb.lessThan(raiz.get("ocorridoEm"), ate));
			}
			if (temTexto(login)) {
				condicoes.add(cb.equal(cb.lower(raiz.get("contaLogin")), login.strip().toLowerCase(Locale.ROOT)));
			}
			if (temTexto(entidade)) {
				condicoes.add(cb.equal(raiz.get("entidade"), entidade.strip()));
			}
			if (acao != null) {
				condicoes.add(cb.equal(raiz.get("acao"), acao));
			}
			return cb.and(condicoes.toArray(Predicate[]::new));
		};
		PageRequest pagina = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ORDEM);
		return registros.findAll(filtro, pagina).map(RegistroAuditoriaResposta::de);
	}

	/** Entidades que já aparecem no log, para o filtro da tela. */
	@GetMapping("/entidades")
	List<String> entidades() {
		return registros.entidades();
	}

	private static boolean temTexto(String texto) {
		return texto != null && !texto.isBlank();
	}

	/** Um registro do log como a API devolve. */
	record RegistroAuditoriaResposta(UUID id, Instant ocorridoEm, UUID contaId, String login, AcaoAuditoria acao,
			String entidade, String entidadeId, List<String> camposAlterados, String ip) {

		static RegistroAuditoriaResposta de(RegistroAuditoria registro) {
			return new RegistroAuditoriaResposta(registro.id(), registro.ocorridoEm(), registro.contaId(),
					registro.contaLogin(), registro.acao(), registro.entidade(), registro.entidadeId(),
					registro.camposAlterados(), registro.ip());
		}

	}

}
