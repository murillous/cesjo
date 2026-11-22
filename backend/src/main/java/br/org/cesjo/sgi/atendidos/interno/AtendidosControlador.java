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
import br.org.cesjo.sgi.pessoas.Genero;
import br.org.cesjo.sgi.pessoas.TipoDeficiencia;

/**
 * Listagem e ficha dos usuários atendidos (RF007, ADR 0083). Quem tem {@code ATENDIDOS_LER} recebe a
 * visão completa; quem só tem {@code ATENDIDOS_LER_BASICO}, a básica (ADR 0082). A crença e os dados de
 * saúde só entram na ficha de quem tem {@code ATENDIDOS_DADOS_SENSIVEIS} (ADR 0142).
 */
@RestController
@RequestMapping("/api/atendidos")
@PreAuthorize("hasAuthority('ATENDIDOS_LER_BASICO')")
class AtendidosControlador {

	static final String LER_COMPLETO = "ATENDIDOS_LER";

	static final String DADOS_SENSIVEIS = "ATENDIDOS_DADOS_SENSIVEIS";

	private final ServicoConsultaAtendidos servico;

	AtendidosControlador(ServicoConsultaAtendidos servico) {
		this.servico = servico;
	}

	/**
	 * Lista paginada em ordem de nome. Sem {@code situacao}, traz ativos e desligados. Os filtros de tipo
	 * de deficiência, faixa etária, gênero e bairro revelam dado além dos básicos e exigem a ficha completa.
	 */
	@GetMapping
	Page<?> listar(@RequestParam(required = false) String nome, @RequestParam(required = false) Turno turno,
			@RequestParam(required = false) SituacaoInsercao situacao,
			@RequestParam(required = false) TipoDeficiencia tipoDeficiencia,
			@RequestParam(required = false) FaixaEtaria faixaEtaria, @RequestParam(required = false) Genero genero,
			@RequestParam(required = false) String bairro, Pageable pageable, Authentication autenticacao) {
		FiltroAtendidos filtro = new FiltroAtendidos(nome, turno, situacao, tipoDeficiencia, faixaEtaria, genero,
				bairro);
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
		return completo(autenticacao) ? servico.fichaCompleta(id, tem(autenticacao, DADOS_SENSIVEIS))
				: servico.fichaBasica(id);
	}

	private static boolean completo(Authentication autenticacao) {
		return tem(autenticacao, LER_COMPLETO);
	}

	static boolean tem(Authentication autenticacao, String permissao) {
		return autenticacao.getAuthorities().stream().anyMatch(a -> permissao.equals(a.getAuthority()));
	}

}
