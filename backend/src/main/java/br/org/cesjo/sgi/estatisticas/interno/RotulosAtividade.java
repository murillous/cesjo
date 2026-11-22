package br.org.cesjo.sgi.estatisticas.interno;

import br.org.cesjo.sgi.atividades.TipoAtividade;

/**
 * Nome de cada tipo de atividade nos dashboards e relatórios, igual ao do frontend
 * ({@code modulos/atividades/rotulos.ts}). O enum do módulo Atividades não tem rótulo.
 */
final class RotulosAtividade {

	private RotulosAtividade() {
	}

	static String de(TipoAtividade tipo) {
		return switch (tipo) {
			case COM_FAMILIAS -> "Atividade com famílias";
			case COM_USUARIOS -> "Atividade com usuários";
			case COM_USUARIOS_E_FAMILIAS -> "Atividade com usuários e famílias";
			case ATENDIMENTO_MULTIPROFISSIONAL -> "Atendimento multiprofissional";
			case ATENDIMENTO_TECNICO_INDIVIDUALIZADO -> "Atendimento técnico individualizado";
			case ESTUDO_DE_CASO -> "Estudo de caso";
		};
	}

}
