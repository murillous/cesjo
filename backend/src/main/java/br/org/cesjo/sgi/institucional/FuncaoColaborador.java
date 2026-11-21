package br.org.cesjo.sgi.institucional;

/**
 * Função do colaborador na equipe do Centro-Dia (seção 5 do planejamento, ADR 0045).
 *
 * <p>
 * PENDENTE-CESJO(P18): lista provisória (ADR 0045), tirada do planejamento; o documento de requisitos não lista
 * as funções. Mudar aqui exige mudar também o {@code ck_colaborador_funcao} numa migração nova.
 */
public enum FuncaoColaborador {

	ASSISTENTE_SOCIAL,

	PSICOLOGO,

	TERAPEUTA_OCUPACIONAL,

	/** Qualquer função fora das três acima. */
	OUTRA

}
