package br.org.cesjo.sgi.acesso;

import java.util.UUID;

/** Dados de uma conta de acesso que outros módulos podem ver. Nunca leva senha nem perfil. */
public record ResumoConta(UUID id, String login, String nome, boolean ativa) {
}
