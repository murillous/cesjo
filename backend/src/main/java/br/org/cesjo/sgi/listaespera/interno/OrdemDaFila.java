package br.org.cesjo.sgi.listaespera.interno;

import org.springframework.data.domain.Sort;

/**
 * Ordem da fila da lista de espera (RF002, ADR 0057). Único lugar que decide quem vem antes: a posição
 * de cada inscrição aguardando e a ordem da listagem saem daqui.
 */
final class OrdemDaFila {

	// PENDENTE-CESJO(P1): ordem só cronológica, por data de cadastro, com o criado_em e o id como
	// desempate. A prioridade para BPC e CadÚnico, se a equipe definir, entra aqui (ADR 0057).
	static final Sort ORDEM = Sort.by(Sort.Order.asc("dataCadastro"), Sort.Order.asc("criadoEm"),
			Sort.Order.asc("id"));

	private OrdemDaFila() {
	}

}
