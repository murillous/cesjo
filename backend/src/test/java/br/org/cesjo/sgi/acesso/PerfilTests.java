package br.org.cesjo.sgi.acesso;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/** Matriz de perfis da Fundação (ADR 0015). */
class PerfilTests {

	@Test
	void soCoordenadorGerenciaContasELeAuditoria() {
		assertThat(Arrays.stream(Perfil.values()).filter(perfil -> perfil.tem(Permissao.CONTAS_GERENCIAR)))
			.containsExactly(Perfil.COORDENADOR);
		assertThat(Arrays.stream(Perfil.values()).filter(perfil -> perfil.tem(Permissao.AUDITORIA_LER)))
			.containsExactly(Perfil.COORDENADOR);
	}

	@Test
	void documentosConformeAMatriz() {
		assertThat(Arrays.stream(Perfil.values()).filter(perfil -> perfil.tem(Permissao.DOCUMENTOS_LER)))
			.containsExactlyInAnyOrder(Perfil.COORDENADOR, Perfil.EQUIPE_TECNICA, Perfil.ADMINISTRATIVO);
		assertThat(Arrays.stream(Perfil.values()).filter(perfil -> perfil.tem(Permissao.DOCUMENTOS_ENVIAR)))
			.containsExactlyInAnyOrder(Perfil.COORDENADOR, Perfil.ADMINISTRATIVO);
	}

	@Test
	void listaDeEsperaConformeAMatriz() {
		assertThat(Arrays.stream(Perfil.values()).filter(perfil -> perfil.tem(Permissao.LISTA_ESPERA_LER)))
			.containsExactlyInAnyOrder(Perfil.COORDENADOR, Perfil.EQUIPE_TECNICA, Perfil.ADMINISTRATIVO);
		assertThat(Arrays.stream(Perfil.values()).filter(perfil -> perfil.tem(Permissao.LISTA_ESPERA_CADASTRAR)))
			.containsExactlyInAnyOrder(Perfil.COORDENADOR, Perfil.ADMINISTRATIVO);
	}

	@Test
	void soCoordenadorVeEGerenciaColaboradores() {
		assertThat(Arrays.stream(Perfil.values()).filter(perfil -> perfil.tem(Permissao.COLABORADORES_LER)))
			.containsExactly(Perfil.COORDENADOR);
		assertThat(Arrays.stream(Perfil.values()).filter(perfil -> perfil.tem(Permissao.COLABORADORES_GERENCIAR)))
			.containsExactly(Perfil.COORDENADOR);
	}

	@Test
	void usuariosAtendidosConformeAMatriz() {
		assertThat(Arrays.stream(Perfil.values()).filter(perfil -> perfil.tem(Permissao.ATENDIDOS_INSERIR)))
			.containsExactly(Perfil.COORDENADOR);
		assertThat(Arrays.stream(Perfil.values()).filter(perfil -> perfil.tem(Permissao.ATENDIDOS_LER)))
			.containsExactlyInAnyOrder(Perfil.COORDENADOR, Perfil.EQUIPE_TECNICA);
		assertThat(Perfil.values()).allMatch(perfil -> perfil.tem(Permissao.ATENDIDOS_LER_BASICO));
	}

	@Test
	void todosVeemAsPropriasNotificacoes() {
		assertThat(Perfil.values()).allMatch(perfil -> perfil.tem(Permissao.NOTIFICACOES_LER));
	}

	@Test
	void consultaSoVeNotificacoesEDadosBasicosDosAtendidos() {
		assertThat(Perfil.CONSULTA.permissoes()).containsExactlyInAnyOrder(Permissao.NOTIFICACOES_LER,
				Permissao.ATENDIDOS_LER_BASICO);
	}

}
