package br.org.cesjo.sgi.listaespera.interno;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.org.cesjo.sgi.listaespera.OcupacaoDeVagas;

/**
 * Implementação padrão de {@link OcupacaoDeVagas} (ADR 0093). Quando o módulo de atendidos fornecer a
 * dele (inserções ativas), esta sai de cena sozinha, sem a lista de espera depender de atendidos.
 */
@Configuration(proxyBeanMethods = false)
class VagasConfig {

	@Bean
	@ConditionalOnMissingBean(OcupacaoDeVagas.class)
	OcupacaoDeVagas ocupacaoPelasInscricoesInseridas(InscricaoRepositorio inscricoes) {
		return new OcupacaoPelasInscricoesInseridas(inscricoes);
	}

}
