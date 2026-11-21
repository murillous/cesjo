package br.org.cesjo.sgi.acesso;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.test.context.support.WithSecurityContext;

/**
 * Roda o teste autenticado como uma conta com o perfil dado. As authorities são as permissões do
 * perfil, como em produção. Use com {@code csrf()} nas requisições que alteram dados.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Inherited
@WithSecurityContext(factory = ComContaFactory.class)
public @interface ComConta {

	Perfil perfil() default Perfil.COORDENADOR;

	/** UUID da conta. Vazio gera um fixo por login. */
	String id() default "";

	String login() default "teste";

}
