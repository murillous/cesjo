package br.org.cesjo.sgi.acesso;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/** Liga {@code @PreAuthorize} nos controladores e serviços de todos os módulos. */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
class SegurancaMetodosConfig {

}
