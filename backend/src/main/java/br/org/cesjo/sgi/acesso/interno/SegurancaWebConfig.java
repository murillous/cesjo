package br.org.cesjo.sgi.acesso.interno;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import tools.jackson.databind.json.JsonMapper;

import br.org.cesjo.sgi.acesso.ContaAutenticada;
import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;

/**
 * Segurança web da API (ADR 0003 e ADR 0010): sessão no servidor com cookie HttpOnly, CSRF no padrão
 * de SPA (cookie {@code XSRF-TOKEN} legível pelo Angular, cabeçalho {@code X-XSRF-TOKEN}), 401 e 403 em
 * {@code ProblemDetail} e nenhuma página de login do Spring.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PropriedadesAcesso.class)
class SegurancaWebConfig {

	/** Argon2id com os parâmetros recomendados pelo Spring Security 5.8 em diante (ADR 0003). */
	@Bean
	PasswordEncoder codificadorSenha() {
		return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
	}

	@Bean
	CookieCsrfTokenRepository repositorioCsrf() {
		CookieCsrfTokenRepository repositorio = CookieCsrfTokenRepository.withHttpOnlyFalse();
		repositorio.setCookieCustomizer(cookie -> cookie.path("/").sameSite("Lax"));
		return repositorio;
	}

	@Bean
	SecurityContextRepository repositorioContexto() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	SecurityFilterChain cadeiaSeguranca(HttpSecurity http, CookieCsrfTokenRepository repositorioCsrf,
			SecurityContextRepository repositorioContexto, ServicoAutenticacao autenticacao, Auditoria auditoria,
			JsonMapper json) throws Exception {
		RespostasSeguranca respostas = new RespostasSeguranca(json);
		http.authorizeHttpRequests(pedidos -> pedidos
			.requestMatchers(HttpMethod.POST, "/api/sessao/entrar").permitAll()
			.requestMatchers(HttpMethod.GET, "/api/sessao/csrf").permitAll()
			.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
			// springdoc, que só existe fora de produção.
			.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
			.requestMatchers("/error").permitAll()
			.anyRequest().authenticated())
			.csrf(csrf -> csrf.csrfTokenRepository(repositorioCsrf).csrfTokenRequestHandler(new TratadorCsrfSpa()))
			.securityContext(contexto -> contexto.securityContextRepository(repositorioContexto))
			.exceptionHandling(erros -> erros.authenticationEntryPoint(respostas).accessDeniedHandler(respostas))
			.logout(saida -> saida.logoutUrl("/api/sessao/sair")
				.addLogoutHandler(auditarSaida(auditoria))
				.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.requestCache(AbstractHttpConfigurer::disable)
			.addFilterBefore(new FiltroContaVigente(autenticacao, repositorioContexto, respostas),
					AuthorizationFilter.class);
		return http.build();
	}

	private static LogoutHandler auditarSaida(Auditoria auditoria) {
		return (requisicao, resposta, autenticacao) -> {
			if (autenticado(autenticacao) && autenticacao.getPrincipal() instanceof ContaAutenticada conta) {
				auditoria.registrar(EventoAuditoria.logout(conta.id(), conta.login()));
			}
		};
	}

	private static boolean autenticado(Authentication autenticacao) {
		return autenticacao != null && autenticacao.isAuthenticated();
	}

}
