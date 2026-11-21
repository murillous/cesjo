package br.org.cesjo.sgi.pessoas;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Dados para cadastrar uma pessoa com o seu responsável (RF001). Quem chama já validou os campos; aqui
 * só se normaliza espaço e telefone.
 *
 * @param nomeCompleto nome da pessoa com deficiência
 * @param dataNascimento data de nascimento, quando informada
 * @param tipoDeficiencia tipo de deficiência
 * @param telefone telefone de contato, com ou sem máscara; guardado só com os dígitos
 * @param nomeResponsavel nome do responsável pela pessoa
 */
public record NovaPessoa(String nomeCompleto, LocalDate dataNascimento, TipoDeficiencia tipoDeficiencia,
		String telefone, String nomeResponsavel) {

	public NovaPessoa {
		nomeCompleto = Objects.requireNonNull(nomeCompleto, "nomeCompleto").strip();
		Objects.requireNonNull(tipoDeficiencia, "tipoDeficiencia");
		telefone = Telefone.normalizar(Objects.requireNonNull(telefone, "telefone"));
		nomeResponsavel = Objects.requireNonNull(nomeResponsavel, "nomeResponsavel").strip();
	}

}
