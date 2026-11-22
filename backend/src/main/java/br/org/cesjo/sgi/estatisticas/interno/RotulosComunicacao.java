package br.org.cesjo.sgi.estatisticas.interno;

import br.org.cesjo.sgi.atividades.DirecaoComunicacao;
import br.org.cesjo.sgi.atividades.TipoComunicacao;

/**
 * Nome de cada tipo de contato e comunicação no relatório, no plural, como no frontend
 * ({@code modulos/comunicacoes/rotulos.ts}). O enum do módulo Atividades não tem rótulo.
 */
final class RotulosComunicacao {

	private RotulosComunicacao() {
	}

	static String de(TipoComunicacao tipo) {
		return switch (tipo) {
			case CONTATO_CUIDADOR_FAMILIAR -> "Contatos telefônicos com cuidador familiar";
			case CONTATO_INSTITUCIONAL -> "Contatos telefônicos institucionais";
			case ATENDIMENTO_PUBLICO_NAO_ACOMPANHADO -> "Atendimentos a público não acompanhado";
			case OFICIO -> "Ofícios";
			case DECLARACAO -> "Declarações";
			case ENCAMINHAMENTO -> "Encaminhamentos";
		};
	}

	/** Por exemplo "Ofícios expedidos" ou "Declarações recebidas". */
	static String de(TipoComunicacao tipo, DirecaoComunicacao direcao) {
		boolean feminino = tipo == TipoComunicacao.DECLARACAO;
		String verbo = switch (direcao) {
			case ENVIADA -> tipo == TipoComunicacao.ENCAMINHAMENTO ? "enviados" : feminino ? "expedidas" : "expedidos";
			case RECEBIDA -> feminino ? "recebidas" : "recebidos";
		};
		return de(tipo) + " " + verbo;
	}

}
