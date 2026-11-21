package br.org.cesjo.sgi.acesso;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Ponto de entrada público para saber qual conta fez a requisição atual. Fora de uma requisição
 * autenticada (tarefa agendada, login falho), devolve vazio.
 */
@Component
public class ContaAtual {

	public Optional<ContaAutenticada> obter() {
		Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacao != null && autenticacao.getPrincipal() instanceof ContaAutenticada conta) {
			return Optional.of(conta);
		}
		return Optional.empty();
	}

	public Optional<UUID> id() {
		return obter().map(ContaAutenticada::id);
	}

}
