package br.org.cesjo.sgi.pessoas;

import java.util.UUID;

/**
 * Um responsável pela pessoa, como o módulo Pessoas devolve para os outros módulos.
 *
 * @param principal {@code true} para o responsável do cadastro na lista de espera (RF001), o primeiro
 * cadastrado
 */
public record DadosResponsavel(UUID id, UUID pessoaId, String nomeCompleto, boolean principal) {
}
