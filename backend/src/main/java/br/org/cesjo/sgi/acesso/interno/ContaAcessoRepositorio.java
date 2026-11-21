package br.org.cesjo.sgi.acesso.interno;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import br.org.cesjo.sgi.acesso.Perfil;

interface ContaAcessoRepositorio extends JpaRepository<ContaAcesso, UUID> {

	Optional<ContaAcesso> findByLogin(String login);

	boolean existsByLogin(String login);

	/**
	 * Contas ativas do perfil, travadas até o fim da transação. Serve à regra da última conta ativa de
	 * Coordenador: duas desativações simultâneas não passam juntas pela checagem.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	List<ContaAcesso> findByPerfilAndAtivaTrue(Perfil perfil);

}
