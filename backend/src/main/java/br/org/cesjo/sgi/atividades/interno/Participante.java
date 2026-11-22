package br.org.cesjo.sgi.atividades.interno;

import java.util.UUID;

/** Usuário atendido que participou da atividade, ativo ou já desligado. */
record Participante(UUID pessoaId, String nomeCompleto) {
}
