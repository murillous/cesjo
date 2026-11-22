package br.org.cesjo.sgi.acesso;

/**
 * Permissões finas do sistema. Cada uma vira uma authority do Spring Security com o mesmo nome, usada
 * em {@code @PreAuthorize("hasAuthority('NOME')")}. Os perfis são conjuntos fixos destas permissões,
 * definidos em {@link Perfil} (ADR 0007).
 *
 * <p>
 * Cada módulo acrescenta as suas permissões aqui quando entrar. As da Fundação são as abaixo.
 */
public enum Permissao {

	/** Criar, editar, desativar e redefinir senha de contas de acesso (seção 4.1). */
	CONTAS_GERENCIAR,

	/** Consultar o log de auditoria (RNF 3.3). */
	AUDITORIA_LER,

	/** Ler e baixar documentos anexados, como laudos e comprovantes (RF001). */
	DOCUMENTOS_LER,

	/** Enviar documentos anexados (RF001). */
	DOCUMENTOS_ENVIAR,

	/** Ver e marcar como lidas as próprias notificações internas (RF003). */
	NOTIFICACOES_LER,

	/** Ler a ficha completa dos candidatos da lista de espera (RF001, RF002). */
	LISTA_ESPERA_LER,

	/** Cadastrar candidato na lista de espera (RF001, ADR 0039). */
	LISTA_ESPERA_CADASTRAR,

	/**
	 * Ver a fila da lista de espera só com os dados básicos: nome, situação, datas e posição, sem contato
	 * nem documentos (RF002, ADR 0058).
	 */
	LISTA_ESPERA_FILA_LER,

	/** Editar os dados do candidato e mudar a situação da inscrição (RF002, ADR 0090). */
	LISTA_ESPERA_EDITAR,

	/** Exportar a fila da lista de espera em PDF e Excel (seção 5.2, ADR 0130). */
	LISTA_ESPERA_EXPORTAR,
	/** Ver o cadastro de colaboradores (RF014, ADR 0047). */
	COLABORADORES_LER,

	/** Cadastrar e editar colaboradores, inclusive a situação e a conta ligada (RF014, ADR 0047). */
	COLABORADORES_GERENCIAR,

	/** Inserir no serviço um candidato da lista de espera (RF007, ADR 0082). */
	ATENDIDOS_INSERIR,

	/** Ler a ficha completa dos usuários atendidos (RF007, ADR 0082). */
	ATENDIDOS_LER,

	/** Ler só os dados básicos dos usuários atendidos: nome, situação, turno e datas (ADR 0082). */
	ATENDIDOS_LER_BASICO,

	/** Completar e corrigir o cadastro do usuário atendido, sem crença nem saúde (RF007, ADR 0142). */
	ATENDIDOS_EDITAR,

	/** Ler e editar a crença religiosa e os dados de saúde do usuário atendido (RF007, ADR 0142). */
	ATENDIDOS_DADOS_SENSIVEIS,

	/** Ver a agenda e a ficha das visitas domiciliares (RF004, ADR 0071). */
	VISITAS_LER,

	/** Agendar visita domiciliar (RF004, ADR 0071). */
	VISITAS_AGENDAR,

	/** Registrar a realização da visita e ler as observações, que são sensíveis (RF004, ADR 0074). */
	VISITAS_REGISTRAR,

	/** Ler as escutas ativas e qualificadas, com os textos sensíveis (RF005, ADR 0101). */
	ESCUTA_LER,

	/** Registrar e corrigir escutas (RF005, ADR 0101). */
	ESCUTA_REGISTRAR,

	/** Ler o PIA e as versões anteriores, com o conteúdo sensível (RF006, ADR 0104). */
	PIA_LER,

	/** Elaborar o PIA e editar a versão em elaboração (RF006, ADR 0104). */
	PIA_EDITAR,

	/** Aprovar o PIA apresentado à família e à equipe (RF006, seção 4.1, ADR 0107). */
	PIA_APROVAR,

	/** Ver a capacidade do serviço, as vagas disponíveis e o tamanho da fila (RF003, ADR 0092). */
	CAPACIDADE_LER,

	/** Definir a capacidade do serviço (RF003, ADR 0092). */
	CAPACIDADE_EDITAR,

	/** Registrar o desligamento de um usuário atendido (RF008, ADR 0110). */
	ATENDIDOS_DESLIGAR,

	/** Registrar violação de direitos (RF009, ADR 0112). */
	VIOLACOES_REGISTRAR,

	/** Consultar violações de direitos e ler a descrição, com leitura auditada (RF009, ADR 0113). */
	VIOLACOES_LER,

	/** Listar as atividades e abrir a ficha com a descrição (RF010, ADR 0152). */
	ATIVIDADES_LER,

	/** Registrar e corrigir atividades (RF010, ADR 0152). */
	ATIVIDADES_REGISTRAR,

	/** Listar os contatos e comunicações e abrir a ficha com a descrição (RF011, ADR 0162). */
	COMUNICACOES_LER,

	/** Registrar e corrigir contatos e comunicações (RF011, ADR 0162). */
	COMUNICACOES_REGISTRAR

}
