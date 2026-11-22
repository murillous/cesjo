package br.org.cesjo.sgi.listaespera;

/**
 * Quantas vagas do serviço estão ocupadas, para o cálculo de vagas disponíveis (RF003, ADR 0093).
 *
 * <p>
 * A conta certa são as inserções ativas, que são do módulo de usuários atendidos (RF007, RF008). Esse
 * módulo depende da lista de espera, e não o contrário (ADR 0081), então é ele quem implementa esta
 * interface com {@code UsuariosAtendidos.contarAtivos()} (ADR 0135).
 */
public interface OcupacaoDeVagas {

	long vagasOcupadas();

}
