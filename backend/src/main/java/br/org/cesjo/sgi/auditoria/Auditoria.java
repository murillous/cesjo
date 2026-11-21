package br.org.cesjo.sgi.auditoria;

/**
 * API pública do log de auditoria. Todo módulo registra suas operações por aqui (ADR 0007).
 */
public interface Auditoria {

	void registrar(EventoAuditoria evento);

}
