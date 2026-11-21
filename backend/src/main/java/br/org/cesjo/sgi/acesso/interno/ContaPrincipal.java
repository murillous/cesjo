package br.org.cesjo.sgi.acesso.interno;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import br.org.cesjo.sgi.acesso.ContaAutenticada;
import br.org.cesjo.sgi.acesso.Perfil;

/**
 * Principal do Spring Security guardado na sessão. É um retrato da conta no momento do login, renovado
 * a cada requisição pelo {@link FiltroContaVigente} (ADR 0010).
 */
record ContaPrincipal(UUID id, String login, String nome, Perfil perfil, boolean trocaSenhaObrigatoria)
		implements ContaAutenticada, Serializable {

	static ContaPrincipal de(ContaAcesso conta) {
		return new ContaPrincipal(conta.getId(), conta.getLogin(), conta.getNome(), conta.getPerfil(),
				conta.isTrocaSenhaObrigatoria());
	}

	/** As authorities são os nomes das permissões do perfil (ADR 0007). */
	List<GrantedAuthority> authorities() {
		return perfil.permissoes()
			.stream()
			.sorted()
			.<GrantedAuthority>map(permissao -> new SimpleGrantedAuthority(permissao.name()))
			.toList();
	}

	Authentication autenticacao() {
		return UsernamePasswordAuthenticationToken.authenticated(this, null, authorities());
	}

}
