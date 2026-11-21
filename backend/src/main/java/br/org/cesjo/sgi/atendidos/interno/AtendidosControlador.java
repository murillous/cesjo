package br.org.cesjo.sgi.atendidos.interno;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.org.cesjo.sgi.atendidos.FaixaEtaria;
import br.org.cesjo.sgi.atendidos.SituacaoInsercao;
import br.org.cesjo.sgi.atendidos.Turno;
import br.org.cesjo.sgi.pessoas.TipoDeficiencia;

/**
 * Listagem e ficha dos usuários atendidos (RF007, ADR 0083). Quem tem {@code ATENDIDOS_LER} recebe a
 * visão completa; quem só tem {@code ATENDIDOS_LER_BASICO}, a básica (ADR 0082).
 */
@RestController
@RequestMapping("/api/atendidos")
@PreAuthorize("hasAuthority('ATENDIDOS_LER_BASICO')")
class AtendidosControlador {

	static final String LER_COMPLETO = "ATENDIDOS_LER";

	private final ServicoConsultaAtendidos servico;

	AtendidosControlador(ServicoConsultaAtendidos servico) {
		this.servico = servico;
	}

	/**
	 * Lista paginada em ordem de nome. Sem {@code situacao}, traz ativos e desligados. Os filtros de tipo
	 * de deficiência e faixa etária revelam dado além dos básicos e exigem a ficha completa.
	 */
	@GetMapping
	Page<?> listar(@RequestParam(required = false) String nome, @RequestParam(required = false) Turno turno,
			@RequestParam(required = false) SituacaoInsercao situacao,
			@RequestParam(required = false) TipoDeficiencia tipoDeficiencia,
			@RequestParam(required = false) FaixaEtaria faixaEtaria, Pageable pageable, Authentication autenticacao) {
		FiltroAtendidos filtro = new FiltroAtendidos(nome, turno, situacao, tipoDeficiencia, faixaEtaria);
		if (completo(autenticacao)) {
			return servico.listarCompleto(filtro, pageable);
		}
		if (filtro.usaDadoCompleto()) {
			throw new AccessDeniedException("Filtro disponível só para quem lê a ficha completa.");
		}
		return servico.listarBasico(filtro, pageable);
	}

	@GetMapping("/{id}")
	Object ficha(@PathVariable UUID id, Authentication autenticacao) {
		return completo(autenticacao) ? servico.fichaCompleta(id) : servico.fichaBasica(id);
	}

	private static boolean completo(Authentication autenticacao) {
		return autenticacao.getAuthorities().stream().anyMatch(a -> LER_COMPLETO.equals(a.getAuthority()));
	}

}
