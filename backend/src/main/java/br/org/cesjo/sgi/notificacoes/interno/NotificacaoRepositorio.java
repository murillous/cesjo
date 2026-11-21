package br.org.cesjo.sgi.notificacoes.interno;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.org.cesjo.sgi.acesso.Perfil;

/**
 * Consultas da caixa de notificações de uma conta. Uma conta vê as notificações dirigidas a ela e as
 * dirigidas ao seu perfil atual (ADR 0030); toda consulta aqui aplica esse filtro.
 */
interface NotificacaoRepositorio extends JpaRepository<Notificacao, UUID> {

	String VISIVEL_PARA_CONTA = "(n.contaDestino = :conta or n.perfilDestino = :perfil)";

	String NAO_LIDA_PELA_CONTA = "not exists (select 1 from LeituraNotificacao l where l.notificacaoId = n.id and l.contaId = :conta)";

	@Query(value = """
			select new br.org.cesjo.sgi.notificacoes.interno.NotificacaoResposta(
				n.id, n.tipo, n.mensagem, n.referencia, n.criadaEm, l.lidaEm)
			from Notificacao n
			left join LeituraNotificacao l on l.notificacaoId = n.id and l.contaId = :conta
			where """ + VISIVEL_PARA_CONTA + """
			 and (:apenasNaoLidas = false or l.lidaEm is null)
			order by n.criadaEm desc, n.id desc
			""", countQuery = """
			select count(n)
			from Notificacao n
			left join LeituraNotificacao l on l.notificacaoId = n.id and l.contaId = :conta
			where """ + VISIVEL_PARA_CONTA + """
			 and (:apenasNaoLidas = false or l.lidaEm is null)
			""")
	Page<NotificacaoResposta> listarDaConta(@Param("conta") UUID conta, @Param("perfil") Perfil perfil,
			@Param("apenasNaoLidas") boolean apenasNaoLidas, Pageable pagina);

	@Query("select count(n) from Notificacao n where " + VISIVEL_PARA_CONTA + " and " + NAO_LIDA_PELA_CONTA)
	long contarNaoLidas(@Param("conta") UUID conta, @Param("perfil") Perfil perfil);

	@Query("select n.id from Notificacao n where " + VISIVEL_PARA_CONTA + " and " + NAO_LIDA_PELA_CONTA)
	List<UUID> idsNaoLidas(@Param("conta") UUID conta, @Param("perfil") Perfil perfil);

	@Query("select count(n) > 0 from Notificacao n where n.id = :id and " + VISIVEL_PARA_CONTA)
	boolean visivelParaConta(@Param("id") UUID id, @Param("conta") UUID conta, @Param("perfil") Perfil perfil);

}
