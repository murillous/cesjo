package br.org.cesjo.sgi.auditoria.interno;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.simple.JdbcClient;
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
 * ordenação conforme o ADR 0024; o total é contado só até {@link #TETO_CONTAGEM} (ADR 0236).
 */
@RestController
@RequestMapping("/api/auditoria")
@PreAuthorize("hasAuthority('AUDITORIA_LER')")
@Transactional(readOnly = true)
class ConsultaAuditoriaController {

	/** Mais recente primeiro; o id desempata registros do mesmo instante para a paginação ser estável. */
	private static final Sort ORDEM = Sort.by(Sort.Order.desc("ocorridoEm"), Sort.Order.desc("id"));

	/**
	 * Até quantos registros a listagem conta. Acima disso o total sai como o teto, com
	 * {@code totalExato} falso, e a tela pede um filtro: contar o log inteiro a cada página lia a tabela
	 * toda (ADR 0236).
	 */
	static final long TETO_CONTAGEM = 10_000;

	private final RegistroAuditoriaRepositorio registros;

	private final JdbcClient jdbc;

	ConsultaAuditoriaController(RegistroAuditoriaRepositorio registros, JdbcClient jdbc) {
		this.registros = registros;
		this.jdbc = jdbc;
	}

	/**
	 * Lista o log paginado. {@code de} é inclusivo e {@code ate} é exclusivo, ambos instantes ISO-8601;
	 * {@code login} compara sem diferenciar maiúsculas; {@code entidade} e {@code acao} são exatos.
	 */
	@GetMapping
	PaginaAuditoria listar(@RequestParam(required = false) Instant de, @RequestParam(required = false) Instant ate,
			@RequestParam(required = false) String login, @RequestParam(required = false) String entidade,
			@RequestParam(required = false) AcaoAuditoria acao, Pageable pageable) {
		if (de != null && ate != null && !de.isBefore(ate)) {
			throw new RegraNegocioException("O início do período deve ser anterior ao fim.");
		}
		FiltroAuditoria filtro = new FiltroAuditoria(de, ate, login, entidade, acao);
		PageRequest pagina = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ORDEM);
		List<RegistroAuditoriaResposta> conteudo = registros
			.findBy(filtro.especificacao(), consulta -> consulta.slice(pagina))
			.map(RegistroAuditoriaResposta::de)
			.getContent();
		long contados = filtro.contarAte(jdbc, TETO_CONTAGEM);
		boolean exato = contados <= TETO_CONTAGEM;
		long total = Math.min(contados, TETO_CONTAGEM);
		int paginas = (int) ((total + pagina.getPageSize() - 1) / pagina.getPageSize());
		return new PaginaAuditoria(conteudo,
				new PaginaAuditoria.Metadados(pagina.getPageSize(), pagina.getPageNumber(), total, paginas, exato));
	}

	/** Entidades que já aparecem no log, para o filtro da tela. */
	@GetMapping("/entidades")
	List<String> entidades() {
		return registros.entidades();
	}

	/**
	 * Página do log no mesmo formato das outras listagens ({@code content} e {@code page}), com
	 * {@code totalExato} falso quando o filtro tem mais de {@link #TETO_CONTAGEM} registros.
	 */
	record PaginaAuditoria(List<RegistroAuditoriaResposta> content, Metadados page) {

		record Metadados(int size, int number, long totalElements, int totalPages, boolean totalExato) {
		}

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
