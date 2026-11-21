package br.org.cesjo.sgi.atendimento.interno;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.org.cesjo.sgi.compartilhado.web.RecursoNaoEncontradoException;
import br.org.cesjo.sgi.listaespera.ListaEspera;
import br.org.cesjo.sgi.pessoas.Pessoas;

/**
 * Pessoas para as telas de atendimento (escuta e PIA, ADR 0102). Por enquanto a lista traz os candidatos
 * ativos da lista de espera; os usuários atendidos entram com o marco M3.
 */
@RestController
@RequestMapping("/api/atendimento/pessoas")
@PreAuthorize("hasAnyAuthority('ESCUTA_LER', 'PIA_LER')")
class PessoasAtendimentoControlador {

	private final ListaEspera listaEspera;

	private final Pessoas pessoas;

	PessoasAtendimentoControlador(ListaEspera listaEspera, Pessoas pessoas) {
		this.listaEspera = listaEspera;
		this.pessoas = pessoas;
	}

	@GetMapping
	List<PessoaAtendida> listar() {
		return listaEspera.listarCandidatosAtivos()
			.stream()
			.map(c -> new PessoaAtendida(c.pessoaId(), c.nomeCompleto()))
			.toList();
	}

	@GetMapping("/{id}")
	PessoaAtendida obter(@PathVariable UUID id) {
		String nome = pessoas.nomes(List.of(id)).get(id);
		if (nome == null) {
			throw new RecursoNaoEncontradoException("Pessoa não encontrada.");
		}
		return new PessoaAtendida(id, nome);
	}

}
