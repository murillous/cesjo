package br.org.cesjo.sgi.notificacoes.interno;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.org.cesjo.sgi.acesso.ContaAtual;
import br.org.cesjo.sgi.acesso.ContaAutenticada;

/**
 * Caixa de notificações da conta logada (ADR 0032). Toda operação vale só para a própria conta: não há
 * como ver ou marcar notificação de outra conta.
 */
@RestController
@RequestMapping("/api/notificacoes")
@PreAuthorize("hasAuthority('NOTIFICACOES_LER')")
class NotificacoesControlador {

	private final ServicoNotificacoes servico;

	private final ContaAtual contaAtual;

	NotificacoesControlador(ServicoNotificacoes servico, ContaAtual contaAtual) {
		this.servico = servico;
		this.contaAtual = contaAtual;
	}

	@GetMapping
	Page<NotificacaoResposta> listar(@RequestParam(defaultValue = "false") boolean naoLidas, Pageable pagina) {
		return servico.listar(conta(), naoLidas, pagina);
	}

	@GetMapping("/nao-lidas/contagem")
	ContagemNaoLidas contarNaoLidas() {
		return new ContagemNaoLidas(servico.contarNaoLidas(conta()));
	}

	@PostMapping("/{id}/leitura")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void marcarComoLida(@PathVariable UUID id) {
		servico.marcarComoLida(conta(), id);
	}

	@PostMapping("/leitura")
	MarcadasComoLidas marcarTodasComoLidas() {
		return new MarcadasComoLidas(servico.marcarTodasComoLidas(conta()));
	}

	private ContaAutenticada conta() {
		return contaAtual.obter().orElseThrow(() -> new AccessDeniedException("Conta não identificada."));
	}

	record ContagemNaoLidas(long quantidade) {
	}

	record MarcadasComoLidas(int marcadas) {
	}

}
