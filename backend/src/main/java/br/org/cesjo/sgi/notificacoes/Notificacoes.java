package br.org.cesjo.sgi.notificacoes;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import br.org.cesjo.sgi.acesso.Perfil;

/**
 * API pública para outros módulos criarem notificações internas (RF003, ADR 0031).
 *
 * <p>
 * As chamadas entram na transação de quem chama: se a operação de negócio for desfeita, a notificação
 * também é. Cada notificação criada é registrada no log de auditoria (ADR 0033).
 *
 * <p>
 * Exemplo, ao abrir uma vaga na lista de espera:
 *
 * <pre>{@code
 * notificacoes.notificarPerfis(Set.of(Perfil.COORDENADOR, Perfil.ADMINISTRATIVO),
 * 		new NovaNotificacao("VAGA_ABERTA", "Uma vaga foi aberta no serviço.", "/lista-espera"));
 * }</pre>
 */
public interface Notificacoes {

	/**
	 * Notifica todas as contas do perfil. Cada conta marca a sua leitura sem afetar as outras.
	 * @return identificador da notificação criada
	 */
	UUID notificarPerfil(Perfil perfil, NovaNotificacao notificacao);

	/**
	 * Notifica cada perfil do conjunto, com uma notificação por perfil.
	 * @return identificadores das notificações criadas, na ordem dos perfis no enum
	 */
	List<UUID> notificarPerfis(Set<Perfil> perfis, NovaNotificacao notificacao);

	/**
	 * Notifica uma única conta de acesso.
	 * @return identificador da notificação criada
	 */
	UUID notificarConta(UUID contaId, NovaNotificacao notificacao);

}
