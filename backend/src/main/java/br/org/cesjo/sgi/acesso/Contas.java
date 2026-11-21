package br.org.cesjo.sgi.acesso;

import java.util.Optional;
import java.util.UUID;

/**
 * API pública de leitura das contas de acesso, para outros módulos que guardam o {@code id} de uma
 * conta (por exemplo o colaborador ligado a uma conta, ADR 0046).
 *
 * <p>
 * A permissão é de quem chama: este serviço não checa perfil.
 */
public interface Contas {

	Optional<ResumoConta> obter(UUID id);

}
