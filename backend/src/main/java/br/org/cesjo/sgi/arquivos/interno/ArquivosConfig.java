package br.org.cesjo.sgi.arquivos.interno;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PropriedadesArquivos.class)
class ArquivosConfig {

}
