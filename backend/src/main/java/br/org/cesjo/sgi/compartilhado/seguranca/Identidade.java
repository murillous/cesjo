package br.org.cesjo.sgi.compartilhado.seguranca;

import java.util.UUID;

/**
 * Identidade mínima de quem faz a requisição: id e login da conta. Existe fora do módulo
 * {@code acesso} para que módulos usados pelo próprio acesso, como a auditoria, saibam quem está
 * logado sem depender dele (ADR 0009).
 */
public interface Identidade {

	UUID id();

	String login();

}
