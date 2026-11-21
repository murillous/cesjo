package br.org.cesjo.sgi.acesso;

import static br.org.cesjo.sgi.acesso.Permissao.AUDITORIA_LER;
import static br.org.cesjo.sgi.acesso.Permissao.COLABORADORES_GERENCIAR;
import static br.org.cesjo.sgi.acesso.Permissao.COLABORADORES_LER;
import static br.org.cesjo.sgi.acesso.Permissao.CONTAS_GERENCIAR;
import static br.org.cesjo.sgi.acesso.Permissao.DOCUMENTOS_ENVIAR;
import static br.org.cesjo.sgi.acesso.Permissao.DOCUMENTOS_LER;
import static br.org.cesjo.sgi.acesso.Permissao.LISTA_ESPERA_CADASTRAR;
import static br.org.cesjo.sgi.acesso.Permissao.LISTA_ESPERA_LER;
import static br.org.cesjo.sgi.acesso.Permissao.NOTIFICACOES_LER;
import static br.org.cesjo.sgi.acesso.Permissao.VISITAS_AGENDAR;
import static br.org.cesjo.sgi.acesso.Permissao.VISITAS_LER;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Perfis de acesso da seção 4 do documento de requisitos. Uma conta tem um perfil, e cada perfil é um
 * conjunto fixo de {@link Permissao}, conforme a matriz da seção 6 do planejamento (ADR 0007). A
 * matriz da Fundação e as suposições provisórias estão no ADR 0015.
 *
 * <p>
 * Este enum é o único lugar onde a matriz de permissões vive no backend.
 */
public enum Perfil {

	// PENDENTE-CESJO(P17): as áreas com (?) na matriz que ainda não têm permissões entram sem acesso para
	// os perfis em dúvida, conforme a tabela de suposições do ADR 0015.

	COORDENADOR("Coordenador",
			EnumSet.of(CONTAS_GERENCIAR, AUDITORIA_LER, DOCUMENTOS_LER, DOCUMENTOS_ENVIAR, NOTIFICACOES_LER,
					LISTA_ESPERA_LER, LISTA_ESPERA_CADASTRAR, COLABORADORES_LER, COLABORADORES_GERENCIAR, VISITAS_LER,
					VISITAS_AGENDAR)),

	// PENDENTE-CESJO(P17): a Equipe Técnica não vê o cadastro de colaboradores (gestão institucional, ADR 0047).
	// PENDENTE-CESJO(P17): a Equipe Técnica lê a lista de espera mas não cadastra candidatos (ADR 0039).
	EQUIPE_TECNICA("Equipe Técnica",
			EnumSet.of(DOCUMENTOS_LER, NOTIFICACOES_LER, LISTA_ESPERA_LER, VISITAS_LER, VISITAS_AGENDAR)),

	// PENDENTE-CESJO(P17): o Administrativo não vê o cadastro de colaboradores (gestão institucional, ADR 0047).
	// PENDENTE-CESJO(P8): o Administrativo envia e lê laudos e comprovantes até a equipe responder (ADR 0015).
	ADMINISTRATIVO("Administrativo",
			EnumSet.of(DOCUMENTOS_LER, DOCUMENTOS_ENVIAR, NOTIFICACOES_LER, LISTA_ESPERA_LER, LISTA_ESPERA_CADASTRAR,
					VISITAS_LER, VISITAS_AGENDAR)),

	CONSULTA("Consulta", EnumSet.of(NOTIFICACOES_LER));

	private final String rotulo;

	private final Set<Permissao> permissoes;

	Perfil(String rotulo, EnumSet<Permissao> permissoes) {
		this.rotulo = rotulo;
		this.permissoes = Collections.unmodifiableSet(permissoes);
	}

	/** Nome exibido nas telas. */
	public String rotulo() {
		return rotulo;
	}

	public Set<Permissao> permissoes() {
		return permissoes;
	}

	public boolean tem(Permissao permissao) {
		return permissoes.contains(permissao);
	}

}
