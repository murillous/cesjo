package br.org.cesjo.sgi.acesso.interno;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Autentica login e senha contra as contas de acesso. Como bean, também impede o Spring Boot de criar
 * o usuário em memória com senha gerada.
 */
@Component
class ProvedorAutenticacaoContas implements AuthenticationProvider {

	private final ServicoAutenticacao servico;

	ProvedorAutenticacaoContas(ServicoAutenticacao servico) {
		this.servico = servico;
	}

	@Override
	public Authentication authenticate(Authentication pedido) {
		String senha = pedido.getCredentials() == null ? "" : pedido.getCredentials().toString();
		return servico.autenticar(pedido.getName(), senha)
			.map(ContaPrincipal::autenticacao)
			.orElseThrow(() -> new BadCredentialsException("Login ou senha incorretos."));
	}

	@Override
	public boolean supports(Class<?> tipo) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(tipo);
	}

}
