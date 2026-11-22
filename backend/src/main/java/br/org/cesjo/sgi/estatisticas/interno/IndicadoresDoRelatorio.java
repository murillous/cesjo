package br.org.cesjo.sgi.estatisticas.interno;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToLongFunction;

import org.springframework.stereotype.Component;

import br.org.cesjo.sgi.atendidos.MotivoDesligamento;
import br.org.cesjo.sgi.atendidos.UsuariosAtendidos;
import br.org.cesjo.sgi.atividades.Atividades;
import br.org.cesjo.sgi.atividades.Comunicacoes;
import br.org.cesjo.sgi.atividades.DirecaoComunicacao;
import br.org.cesjo.sgi.atividades.TipoAtividade;
import br.org.cesjo.sgi.atividades.TipoComunicacao;
import br.org.cesjo.sgi.listaespera.ListaEspera;
import br.org.cesjo.sgi.listaespera.MotivoSaida;

/**
 * O conteúdo do relatório estatístico (RF016, ADR 0200): a lista de indicadores, na ordem em que saem
 * na tela, no PDF e na planilha. Mudar o relatório é mudar esta lista: uma linha por indicador. Cada
 * indicador lê o módulo dono do dado pela API Java pública dele.
 *
 * <p>
 * Vale para qualquer período: o mensal (issue #58) e, depois, o quadrimestral e o anual (issue #59)
 * usam a mesma lista.
 */
@Component
class IndicadoresDoRelatorio {

	static final String LISTA_ESPERA = "Lista de espera";

	static final String USUARIOS = "Usuários atendidos";

	static final String ATENDIMENTOS = "Atendimentos";

	static final String ATIVIDADES = "Atividades";

	static final String COMUNICACOES = "Contatos e comunicações";

	static final String DESLIGAMENTOS = "Desligamentos";

	private final List<Indicador> indicadores;

	IndicadoresDoRelatorio(ListaEspera listaEspera, UsuariosAtendidos atendidos, Atividades atividades,
			Comunicacoes comunicacoes) {
		// PENDENTE-CESJO(P9): conteúdo provisório do relatório estatístico, sem modelo da equipe (ADR 0200).
		this.indicadores = List.of(
				um(LISTA_ESPERA, "Novas inscrições", p -> listaEspera.contarInscricoes(p.inicio(), p.fim())),
				um(LISTA_ESPERA, "Candidatos na fila no último dia", p -> listaEspera.contarNaFilaEm(p.fim())),
				porCategoria(LISTA_ESPERA, "Saídas da lista: ", MotivoSaida.values(), MotivoSaida::rotulo,
						p -> listaEspera.contarSaidasPorMotivo(p.inicio(), p.fim())),

				um(USUARIOS, "Inserções no serviço", p -> atendidos.contarInsercoes(p.inicio(), p.fim())),
				um(USUARIOS, "Usuários atendidos no período", p -> atendidos.contarAtendidosNoPeriodo(p.inicio(), p.fim())),
				um(USUARIOS, "Usuários ativos no último dia", p -> atendidos.contarAtivosEm(p.fim())),

				porCategoria(ATENDIMENTOS, "", tiposDeAtividade(true), RotulosAtividade::de,
						p -> atividades.contarPorTipo(p.inicio(), p.fim())),

				porCategoria(ATIVIDADES, "", tiposDeAtividade(false), RotulosAtividade::de,
						p -> atividades.contarPorTipo(p.inicio(), p.fim())),

				comunicacoes(comunicacoes),

				porCategoria(DESLIGAMENTOS, "", MotivoDesligamento.values(), MotivoDesligamento::rotulo,
						p -> atendidos.contarDesligamentosPorMotivo(p.inicio(), p.fim())));
	}

	/** Todas as linhas do relatório no período, na ordem da lista. */
	List<LinhaRelatorio> calcular(Periodo periodo) {
		List<LinhaRelatorio> linhas = new ArrayList<>();
		for (Indicador indicador : indicadores) {
			linhas.addAll(indicador.calcular(periodo));
		}
		return List.copyOf(linhas);
	}

	/** Um número só. */
	private static Indicador um(String grupo, String nome, ToLongFunction<Periodo> conta) {
		return periodo -> List.of(new LinhaRelatorio(grupo, nome, conta.applyAsLong(periodo)));
	}

	/** Uma linha por categoria, na ordem dada, com zero para a categoria sem registro. */
	private static <E> Indicador porCategoria(String grupo, String prefixo, E[] categorias,
			Function<E, String> rotulo, Function<Periodo, Map<E, Long>> conta) {
		return periodo -> {
			Map<E, Long> totais = conta.apply(periodo);
			List<LinhaRelatorio> linhas = new ArrayList<>();
			for (E categoria : categorias) {
				linhas.add(new LinhaRelatorio(grupo, prefixo + rotulo.apply(categoria), totais.getOrDefault(categoria, 0L)));
			}
			return linhas;
		};
	}

	/** Atendimentos e estudos de caso ({@code individual}) ou as atividades em grupo. */
	private static TipoAtividade[] tiposDeAtividade(boolean individual) {
		return Arrays.stream(TipoAtividade.values())
			.filter(tipo -> tipo.individual() == individual)
			.toArray(TipoAtividade[]::new);
	}

	/** Contatos por tipo; ofícios, declarações e encaminhamentos separados em expedidos e recebidos. */
	private static Indicador comunicacoes(Comunicacoes comunicacoes) {
		return periodo -> {
			Map<TipoComunicacao, Long> porTipo = comunicacoes.contarPorTipo(periodo.inicio(), periodo.fim());
			List<LinhaRelatorio> linhas = new ArrayList<>();
			for (TipoComunicacao tipo : TipoComunicacao.values()) {
				if (!tipo.temDirecao()) {
					linhas.add(new LinhaRelatorio(COMUNICACOES, RotulosComunicacao.de(tipo), porTipo.getOrDefault(tipo, 0L)));
					continue;
				}
				Map<DirecaoComunicacao, Long> porDirecao = comunicacoes.contarPorDirecao(tipo, periodo.inicio(),
						periodo.fim());
				for (DirecaoComunicacao direcao : DirecaoComunicacao.values()) {
					linhas.add(new LinhaRelatorio(COMUNICACOES, RotulosComunicacao.de(tipo, direcao),
							porDirecao.getOrDefault(direcao, 0L)));
				}
			}
			return linhas;
		};
	}

}
