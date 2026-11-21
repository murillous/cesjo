package br.org.cesjo.sgi.auditoria.interno;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;

/**
 * Implementação provisória que só escreve no log da aplicação. A issue #16 a substitui pela gravação
 * na tabela {@code auditoria}.
 */
@Component
class AuditoriaEmLog implements Auditoria {

	private static final Logger log = LoggerFactory.getLogger(AuditoriaEmLog.class);

	@Override
	public void registrar(EventoAuditoria evento) {
		log.info("auditoria acao={} entidade={} id={} campos={}", evento.acao(), evento.entidade(),
				evento.entidadeId(), evento.camposAlterados());
	}

}
