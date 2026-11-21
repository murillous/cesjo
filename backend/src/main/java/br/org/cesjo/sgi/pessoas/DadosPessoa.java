package br.org.cesjo.sgi.pessoas;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Dados de cadastro de uma pessoa, como o módulo Pessoas devolve para os outros módulos.
 *
 * @param telefone só os dígitos, com DDD
 */
public record DadosPessoa(UUID id, String nomeCompleto, LocalDate dataNascimento, TipoDeficiencia tipoDeficiencia,
		String telefone, String nomeResponsavel) {
}
