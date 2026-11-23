package br.org.cesjo.sgi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Verifica as fronteiras entre os módulos do Spring Modulith. Não sobe contexto nem banco.
 */
class ModularidadeTests {

	private final ApplicationModules modulos = ApplicationModules.of(SgiBackendApplication.class);

	@Test
	void modulosRespeitamAsFronteiras() {
		modulos.verify();
	}

	@Test
	void modulosDaArquiteturaEstaoDeclarados() {
		assertThat(modulos.stream().map(ApplicationModule::getIdentifier).map(Object::toString))
			.containsExactlyInAnyOrder("acesso", "auditoria", "arquivos", "notificacoes", "pessoas", "listaespera",
					"atendimento", "atendidos", "atividades", "institucional", "relatorios", "estatisticas", "importacao",
					"demonstracao", "compartilhado");
	}

}
