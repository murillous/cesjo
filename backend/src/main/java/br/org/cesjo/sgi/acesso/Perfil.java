package br.org.cesjo.sgi.acesso;

import static br.org.cesjo.sgi.acesso.Permissao.ATENDIDOS_INSERIR;
import static br.org.cesjo.sgi.acesso.Permissao.ATENDIDOS_LER;
import static br.org.cesjo.sgi.acesso.Permissao.ATENDIDOS_LER_BASICO;
import static br.org.cesjo.sgi.acesso.Permissao.AUDITORIA_LER;
import static br.org.cesjo.sgi.acesso.Permissao.COLABORADORES_GERENCIAR;
import static br.org.cesjo.sgi.acesso.Permissao.COLABORADORES_LER;
import static br.org.cesjo.sgi.acesso.Permissao.CONTAS_GERENCIAR;
import static br.org.cesjo.sgi.acesso.Permissao.DOCUMENTOS_ENVIAR;
import static br.org.cesjo.sgi.acesso.Permissao.DOCUMENTOS_LER;
import static br.org.cesjo.sgi.acesso.Permissao.LISTA_ESPERA_CADASTRAR;
import static br.org.cesjo.sgi.acesso.Permissao.LISTA_ESPERA_EDITAR;
import static br.org.cesjo.sgi.acesso.Permissao.LISTA_ESPERA_FILA_LER;
import static br.org.cesjo.sgi.acesso.Permissao.LISTA_ESPERA_LER;
import static br.org.cesjo.sgi.acesso.Permissao.NOTIFICACOES_LER;
import static br.org.cesjo.sgi.acesso.Permissao.VISITAS_AGENDAR;
import static br.org.cesjo.sgi.acesso.Permissao.VISITAS_LER;
import static br.org.cesjo.sgi.acesso.Permissao.VISITAS_REGISTRAR;

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
					LISTA_ESPERA_LER, LISTA_ESPERA_CADASTRAR, LISTA_ESPERA_FILA_LER, LISTA_ESPERA_EDITAR, COLABORADORES_LER,
					COLABORADORES_GERENCIAR, ATENDIDOS_INSERIR, ATENDIDOS_LER, ATENDIDOS_LER_BASICO, VISITAS_LER,
					VISITAS_AGENDAR, VISITAS_REGISTRAR)),

	// PENDENTE-CESJO(P17): a Equipe Técnica não vê o cadastro de colaboradores (gestão institucional, ADR 0047).
	// PENDENTE-CESJO(P17): a Equipe Técnica lê e edita a lista de espera mas não cadastra candidatos (ADR 0039) e
	// lê os usuários atendidos mas não insere no serviço (ADR 0082).
	EQUIPE_TECNICA("Equipe Técnica",
			EnumSet.of(DOCUMENTOS_LER, NOTIFICACOES_LER, LISTA_ESPERA_LER, LISTA_ESPERA_FILA_LER, LISTA_ESPERA_EDITAR,
					ATENDIDOS_LER, ATENDIDOS_LER_BASICO, VISITAS_LER, VISITAS_AGENDAR, VISITAS_REGISTRAR)),

	// PENDENTE-CESJO(P17): o Administrativo não vê o cadastro de colaboradores (gestão institucional, ADR 0047).
	// PENDENTE-CESJO(P8): o Administrativo agenda, cancela e reagenda visitas, mas não registra a realização
	// nem lê as observações, que são sensíveis (ADR 0074).
	// PENDENTE-CESJO(P8): o Administrativo envia e lê laudos e comprovantes até a equipe responder (ADR 0015).
	ADMINISTRATIVO("Administrativo",
			EnumSet.of(DOCUMENTOS_LER, DOCUMENTOS_ENVIAR, NOTIFICACOES_LER, LISTA_ESPERA_LER, LISTA_ESPERA_CADASTRAR,
					LISTA_ESPERA_FILA_LER, LISTA_ESPERA_EDITAR, ATENDIDOS_LER_BASICO, VISITAS_LER, VISITAS_AGENDAR)),

	// PENDENTE-CESJO(P8): "dados básicos" do Consulta na lista de espera são nome, situação, datas e
	// posição, sem tipo de deficiência, origem, contato nem documentos (ADR 0058).
	// PENDENTE-CESJO(P8): o Consulta vê só os dados básicos dos usuários atendidos (ADR 0015 e ADR 0082).
	CONSULTA("Consulta", EnumSet.of(NOTIFICACOES_LER, LISTA_ESPERA_FILA_LER, ATENDIDOS_LER_BASICO));

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
