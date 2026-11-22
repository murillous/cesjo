package br.org.cesjo.sgi.atendidos;

import java.util.UUID;

/** Usuário atendido ativo como os outros módulos o veem: a pessoa e o nome, para escolher e exibir. */
public record UsuarioAtivo(UUID pessoaId, String nomeCompleto) {
}
