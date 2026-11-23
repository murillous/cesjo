package br.org.cesjo.sgi.estatisticas.interno;

import br.org.cesjo.sgi.institucional.FuncaoColaborador;
import br.org.cesjo.sgi.institucional.TipoDoacao;
import br.org.cesjo.sgi.institucional.TipoEventoInstitucional;

/**
 * Nomes dos tipos do módulo Institucional no relatório de transparência, iguais aos do frontend
 * ({@code rotulos.ts} dos módulos colaboradores, eventos e doações). Os enums não têm rótulo.
 */
final class RotulosInstitucional {

	private RotulosInstitucional() {
	}

	static String de(FuncaoColaborador funcao) {
		return switch (funcao) {
			case ASSISTENTE_SOCIAL -> "Assistente social";
			case PSICOLOGO -> "Psicólogo(a)";
			case TERAPEUTA_OCUPACIONAL -> "Terapeuta ocupacional";
			case OUTRA -> "Outra função";
		};
	}

	static String de(TipoEventoInstitucional tipo) {
		return switch (tipo) {
			case REUNIAO_COORDENACAO_EQUIPE -> "Reunião entre coordenação e equipe técnica";
			case FORMACAO_MULTIPROFISSIONAL -> "Formação multiprofissional";
			case REUNIAO_DIRECAO_GERAL -> "Reunião com a direção geral";
			case MONITORAMENTO_ORGAO_GESTOR -> "Monitoramento do órgão gestor";
			case VISITA_INSTITUCIONAL -> "Visita institucional";
			case EVENTO -> "Evento";
			case REUNIAO_PARCEIROS -> "Reunião com parceiros";
		};
	}

	static String de(TipoDoacao tipo) {
		return switch (tipo) {
			case DINHEIRO -> "Dinheiro";
			case ALIMENTOS -> "Alimentos";
			case VESTUARIO -> "Roupas e calçados";
			case HIGIENE_LIMPEZA -> "Higiene e limpeza";
			case MATERIAL_PEDAGOGICO -> "Material pedagógico e de oficinas";
			case MOVEIS_EQUIPAMENTOS -> "Móveis e equipamentos";
			case OUTRO -> "Outro tipo";
		};
	}

}
