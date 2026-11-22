package br.org.cesjo.sgi.atendidos.interno;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Edição do cadastro completo do usuário atendido (RF007, ADR 0140 e ADR 0142). A crença e a saúde só
 * vão junto para quem tem {@code ATENDIDOS_DADOS_SENSIVEIS}.
 */
@RestController
class CadastroControlador {

	private final ServicoCadastroAtendido servico;

	CadastroControlador(ServicoCadastroAtendido servico) {
		this.servico = servico;
	}

	@PutMapping("/api/atendidos/{id}/cadastro")
	@PreAuthorize("hasAuthority('ATENDIDOS_EDITAR')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void atualizar(@PathVariable UUID id, @Valid @RequestBody EdicaoCadastro edicao, Authentication autenticacao) {
		if (edicao.dadosSensiveis() != null
				&& !AtendidosControlador.tem(autenticacao, AtendidosControlador.DADOS_SENSIVEIS)) {
			throw new AccessDeniedException("Crença e dados de saúde só para quem tem permissão.");
		}
		servico.atualizar(id, edicao);
	}

}
