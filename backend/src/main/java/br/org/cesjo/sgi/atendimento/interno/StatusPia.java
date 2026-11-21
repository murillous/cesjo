package br.org.cesjo.sgi.atendimento.interno;

/** Status do PIA (RF006), na ordem em que acontecem. Toda versão nasce {@link #ELABORADO}. */
enum StatusPia {

	ELABORADO,

	APRESENTADO_FAMILIA,

	APRESENTADO_EQUIPE,

	APROVADO

}
