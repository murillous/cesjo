package br.org.cesjo.sgi.listaespera.interno;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import br.org.cesjo.sgi.pessoas.TipoDocumento;

/**
 * Documentos que o RF001 exige no cadastro da lista de espera (ADR 0056). Sem eles a inscrição continua
 * na fila, mas aparece como cadastro incompleto.
 */
final class DocumentosObrigatorios {

	static final Set<TipoDocumento> TIPOS = EnumSet.of(TipoDocumento.LAUDO_MEDICO,
			TipoDocumento.COMPROVANTE_RESIDENCIA);

	private DocumentosObrigatorios() {
	}

	/** Os tipos obrigatórios que faltam, na ordem do enum. Vazio quando o cadastro está completo. */
	static List<TipoDocumento> pendentes(Collection<TipoDocumento> anexados) {
		return TIPOS.stream().filter(tipo -> !anexados.contains(tipo)).toList();
	}

}
