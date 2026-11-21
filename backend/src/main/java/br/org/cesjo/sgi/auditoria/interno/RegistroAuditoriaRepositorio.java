package br.org.cesjo.sgi.auditoria.interno;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Leitura do log de auditoria. Não estende {@code CrudRepository} de propósito: não há método de
 * alterar nem de excluir (ADR 0022). A gravação é feita por {@link AuditoriaEmBanco}.
 */
interface RegistroAuditoriaRepositorio
		extends Repository<RegistroAuditoria, UUID>, JpaSpecificationExecutor<RegistroAuditoria> {

	@Query("select distinct r.entidade from RegistroAuditoria r order by r.entidade")
	List<String> entidades();

}
