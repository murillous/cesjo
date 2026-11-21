package br.org.cesjo.sgi.listaespera;

/**
 * Ponto de entrada para avisar que vagas foram liberadas fora da lista de espera, por exemplo pelo
 * desligamento de um usuário atendido (RF003, RF008, ADR 0095).
 *
 * <p>
 * O módulo de atendidos depende da lista de espera, e não o contrário (ADR 0081). Por isso a lista de
 * espera não escuta o evento {@code UsuarioDesligado} direto: o ouvinte fica no módulo de atendidos e
 * chama {@link #vagasLiberadas(int)} na mesma transação do desligamento, depois de o desligamento já
 * estar aplicado (a {@link OcupacaoDeVagas} precisa enxergar a vaga livre).
 */
public interface AberturaDeVaga {

	/**
	 * Avisa Coordenador e Administrativo se as vagas disponíveis passaram de zero para mais de zero com a
	 * liberação de {@code quantidade} vagas. Não faz nada nos outros casos.
	 */
	void vagasLiberadas(int quantidade);

}
