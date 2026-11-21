package br.org.cesjo.sgi.acesso;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

class ComContaFactory implements WithSecurityContextFactory<ComConta> {

	@Override
	public SecurityContext createSecurityContext(ComConta anotacao) {
		UUID id = anotacao.id().isEmpty()
				? UUID.nameUUIDFromBytes(anotacao.login().getBytes(StandardCharsets.UTF_8))
				: UUID.fromString(anotacao.id());
		ContaDeTeste conta = new ContaDeTeste(id, anotacao.login(), "Conta " + anotacao.login(), anotacao.perfil());
		List<SimpleGrantedAuthority> authorities = anotacao.perfil()
			.permissoes()
			.stream()
			.map(permissao -> new SimpleGrantedAuthority(permissao.name()))
			.toList();
		SecurityContext contexto = SecurityContextHolder.createEmptyContext();
		contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(conta, null, authorities));
		return contexto;
	}

	record ContaDeTeste(UUID id, String login, String nome, Perfil perfil) implements ContaAutenticada {
	}

}
