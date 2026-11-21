package br.org.cesjo.sgi.listaespera;

/**
 * Quantas vagas do serviço estão ocupadas, para o cálculo de vagas disponíveis (RF003, ADR 0093).
 *
 * <p>
 * A conta certa são as inserções ativas, que são do módulo de usuários atendidos (RF007, RF008). Até
 * ele entrar, a implementação padrão conta as inscrições {@link SituacaoInscricao#INSERIDO}, que não
 * descontam desligamentos. Quando o módulo de atendidos entrar, ele fornece a implementação desta
 * interface e a padrão sai.
 */
public interface OcupacaoDeVagas {

	long vagasOcupadas();

}
