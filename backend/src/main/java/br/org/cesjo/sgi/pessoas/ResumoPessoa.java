package br.org.cesjo.sgi.pessoas;

import java.util.Set;
import java.util.UUID;

/**
 * Dados de uma pessoa para listagens, sem contato (ADR 0059).
 *
 * @param documentos tipos de documento já anexados
 */
public record ResumoPessoa(UUID id, String nomeCompleto, TipoDeficiencia tipoDeficiencia,
		Set<TipoDocumento> documentos) {

	public ResumoPessoa {
		documentos = Set.copyOf(documentos);
	}

}
