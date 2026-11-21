package br.org.cesjo.sgi.listaespera.interno;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import br.org.cesjo.sgi.listaespera.OrigemEncaminhamento;
import br.org.cesjo.sgi.listaespera.SituacaoInscricao;
import br.org.cesjo.sgi.pessoas.TipoDeficiencia;

/**
 * Uma linha da fila. Para quem só vê os dados básicos (ADR 0058), {@code tipoDeficiencia},
 * {@code origemEncaminhamento} e {@code cadastroCompleto} vêm nulos e ficam fora do JSON.
 *
 * @param posicao posição na fila; nula quando a inscrição não está aguardando
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record ItemFila(UUID id, Integer posicao, String nomeCompleto, SituacaoInscricao situacao, LocalDate dataCadastro,
		TipoDeficiencia tipoDeficiencia, OrigemEncaminhamento origemEncaminhamento, Boolean cadastroCompleto) {

	/** Só nome, situação e datas (perfil Consulta). */
	ItemFila basico() {
		return new ItemFila(id, posicao, nomeCompleto, situacao, dataCadastro, null, null, null);
	}

}
