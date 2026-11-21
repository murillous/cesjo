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

	/** Ver o cadastro de colaboradores (RF014, ADR 0047). */
	COLABORADORES_LER,

	/** Cadastrar e editar colaboradores, inclusive a situação e a conta ligada (RF014, ADR 0047). */
	COLABORADORES_GERENCIAR

}
