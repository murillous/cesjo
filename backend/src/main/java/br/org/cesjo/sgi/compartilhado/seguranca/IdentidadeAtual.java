package br.org.cesjo.sgi.compartilhado.seguranca;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Lê a {@link Identidade} da requisição atual. Fora de uma requisição autenticada (tarefa agendada,
 * login falho), devolve vazio. Quem precisa do perfil usa {@code acesso.ContaAtual}.
 */
@Component
public class IdentidadeAtual {

	public Optional<Identidade> obter() {
		Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacao != null && autenticacao.getPrincipal() instanceof Identidade identidade) {
			return Optional.of(identidade);
		}
		return Optional.empty();
	}

}
