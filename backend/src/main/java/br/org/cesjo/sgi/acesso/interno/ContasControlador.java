package br.org.cesjo.sgi.acesso.interno;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Gestão das contas de acesso, só para quem tem {@code CONTAS_GERENCIAR} (seção 4.1). */
@RestController
@RequestMapping("/api/contas")
@PreAuthorize("hasAuthority('CONTAS_GERENCIAR')")
class ContasControlador {

	private final ServicoContas servico;

	ContasControlador(ServicoContas servico) {
		this.servico = servico;
	}

	@GetMapping
	Page<ContaResposta> listar(@PageableDefault(sort = "nome", direction = Sort.Direction.ASC) Pageable paginacao) {
		return servico.listar(paginacao);
	}

	@GetMapping("/{id}")
	ContaResposta obter(@PathVariable UUID id) {
		return servico.obter(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ContaCriada criar(@Valid @RequestBody NovaConta nova) {
		return servico.criar(nova);
	}

	@PutMapping("/{id}")
	ContaResposta editar(@PathVariable UUID id, @Valid @RequestBody EdicaoConta edicao) {
		return servico.editar(id, edicao);
	}

	@PostMapping("/{id}/desativar")
	ContaResposta desativar(@PathVariable UUID id) {
		return servico.desativar(id);
	}

	@PostMapping("/{id}/reativar")
	ContaResposta reativar(@PathVariable UUID id) {
		return servico.reativar(id);
	}

	@PostMapping("/{id}/redefinir-senha")
	SenhaTemporaria redefinirSenha(@PathVariable UUID id) {
		return servico.redefinirSenha(id);
	}

}
