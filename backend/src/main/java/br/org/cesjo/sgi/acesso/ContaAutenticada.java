package br.org.cesjo.sgi.acesso;

import br.org.cesjo.sgi.compartilhado.seguranca.Identidade;

/**
 * Conta de acesso autenticada na requisição atual. O principal do Spring Security implementa esta
 * interface, e os outros módulos leem a conta só por {@link ContaAtual} (ADR 0007). Módulos que o
 * acesso usa, como a auditoria, leem só a {@link Identidade} (ADR 0009).
 */
public interface ContaAutenticada extends Identidade {

	String nome();

	Perfil perfil();

}
