package br.org.cesjo.sgi.listaespera;

import java.time.LocalDate;
import java.util.Objects;

import br.org.cesjo.sgi.pessoas.TipoDeficiencia;

/**
 * Candidato a inscrever na lista de espera por outro módulo, como a importação de planilha (ADR 0215).
 * Quem chama já validou os campos com as mesmas regras do cadastro pela tela (ADR 0038).
 *
 * @param nomeCompleto nome da pessoa com deficiência
 * @param dataNascimento opcional
 * @param tipoDeficiencia tipo de deficiência
 * @param telefone telefone de contato, com ou sem máscara
 * @param nomeResponsavel nome do responsável
 * @param origemEncaminhamento quem encaminhou
 * @param dataCadastro data em que o candidato entrou na lista; define a ordem da fila (ADR 0057)
 */
public record NovoCandidato(String nomeCompleto, LocalDate dataNascimento, TipoDeficiencia tipoDeficiencia,
		String telefone, String nomeResponsavel, OrigemEncaminhamento origemEncaminhamento, LocalDate dataCadastro) {

	public NovoCandidato {
		Objects.requireNonNull(nomeCompleto, "nomeCompleto");
		Objects.requireNonNull(tipoDeficiencia, "tipoDeficiencia");
		Objects.requireNonNull(telefone, "telefone");
		Objects.requireNonNull(nomeResponsavel, "nomeResponsavel");
		Objects.requireNonNull(origemEncaminhamento, "origemEncaminhamento");
		Objects.requireNonNull(dataCadastro, "dataCadastro");
	}

}
