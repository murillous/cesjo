package br.org.cesjo.sgi.acesso;

import java.util.UUID;

/**
 * Conta de acesso autenticada na requisição atual. O principal do Spring Security implementa esta
 * interface, e os outros módulos leem a conta só por {@link ContaAtual} (ADR 0007).
 */
public interface ContaAutenticada {

	UUID id();

	String login();

	String nome();

	Perfil perfil();

}
