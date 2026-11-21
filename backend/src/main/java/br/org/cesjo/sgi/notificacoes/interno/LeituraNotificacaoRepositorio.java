package br.org.cesjo.sgi.notificacoes.interno;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface LeituraNotificacaoRepositorio extends JpaRepository<LeituraNotificacao, LeituraNotificacao.Chave> {

	/**
	 * Marca a notificação como lida pela conta. Não faz nada se já estava lida, inclusive quando duas
	 * requisições chegam juntas.
	 * @return 1 se a leitura foi registrada agora, 0 se já existia
	 */
	@Modifying
	@Query(value = """
			insert into notificacao_leitura (notificacao_id, conta_id, lida_em)
			values (:notificacao, :conta, :lidaEm)
			on conflict do nothing
			""", nativeQuery = true)
	int registrar(@Param("notificacao") UUID notificacao, @Param("conta") UUID conta, @Param("lidaEm") Instant lidaEm);

}
