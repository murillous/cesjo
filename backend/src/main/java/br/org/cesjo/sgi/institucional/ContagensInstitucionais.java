package br.org.cesjo.sgi.institucional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Contagens agregadas do módulo Institucional para os relatórios (RF016, ADR 0211). Só números: não
 * devolve nome, descrição, contraparte nem qualquer outro dado de pessoa ou instituição. Datas são
 * inclusivas.
 *
 * <p>
 * A permissão é de quem chama: este serviço não checa perfil. Só leitura, então não audita.
 */
public interface ContagensInstitucionais {

	/**
	 * Colaboradores na equipe em {@code dia}, por função, com zero para a função sem ninguém. Conta quem
	 * tem vínculo aberto naquele dia (ADR 0170) e, entre os que não têm nenhum vínculo registrado, quem
	 * está ativo hoje.
	 */
	Map<FuncaoColaborador, Long> contarColaboradoresPorFuncaoEm(LocalDate dia);

	/**
	 * Parceiros ativos cuja vigência alcança algum dia do período. Vigência sem início ou sem fim fica
	 * aberta daquele lado.
	 */
	long contarParceriasVigentes(LocalDate inicio, LocalDate fim);

	/** Ações de articulação com parceiros no período. */
	long contarArticulacoes(LocalDate inicio, LocalDate fim);

	/** Reuniões e eventos institucionais no período, por tipo, com zero para o tipo sem registro. */
	Map<TipoEventoInstitucional, Long> contarEventosPorTipo(LocalDate inicio, LocalDate fim);

	/** Doações da direção dada no período, por tipo, com zero para o tipo sem registro. */
	Map<TipoDoacao, Long> contarDoacoesPorTipo(DirecaoDoacao direcao, LocalDate inicio, LocalDate fim);

	/** Soma dos valores das doações em dinheiro da direção dada no período, com duas casas; zero sem nenhuma. */
	BigDecimal somarDoacoesEmDinheiro(DirecaoDoacao direcao, LocalDate inicio, LocalDate fim);

}
