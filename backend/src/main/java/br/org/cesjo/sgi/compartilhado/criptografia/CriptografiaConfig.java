package br.org.cesjo.sgi.compartilhado.criptografia;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Liga as propriedades {@code sgi.criptografia.*}. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PropriedadesCriptografia.class)
class CriptografiaConfig {

}
