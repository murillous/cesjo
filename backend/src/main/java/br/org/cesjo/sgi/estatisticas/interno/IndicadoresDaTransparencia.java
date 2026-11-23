package br.org.cesjo.sgi.estatisticas.interno;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToLongFunction;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.atendidos.UsuariosAtendidos;
import br.org.cesjo.sgi.atividades.Atividades;
import br.org.cesjo.sgi.atividades.TipoAtividade;
import br.org.cesjo.sgi.institucional.ContagensInstitucionais;
import br.org.cesjo.sgi.institucional.DirecaoDoacao;
import br.org.cesjo.sgi.institucional.FuncaoColaborador;
import br.org.cesjo.sgi.institucional.TipoDoacao;
import br.org.cesjo.sgi.institucional.TipoEventoInstitucional;
import br.org.cesjo.sgi.listaespera.ListaEspera;

/**
 * O conteúdo do relatório de transparência (RF016, ADR 0210): a lista de números, na ordem em que saem
 * na tela, no PDF e na planilha. O relatório é público, para prestar contas à sociedade, aos
 * financiadores e aos conselhos: só números agregados, sem nome, descrição, contraparte, motivo ou
 * qualquer dado de pessoa. Mudar o relatório é mudar esta lista.
 */
@Component
class IndicadoresDaTransparencia {

	static final String USUARIOS = "Usuários atendidos";

	static final String LISTA_ESPERA = "Lista de espera";

	static final String ATIVIDADES = "Atendimentos e atividades";

	static final String PARCERIAS = "Parcerias";

	static final String EVENTOS = "Reuniões e eventos institucionais";

	static final String DOACOES_RECEBIDAS = "Doações recebidas";

	static final String DOACOES_REALIZADAS = "Doações feitas pelo Centro-Dia";

	static final String EQUIPE = "Equipe no último dia";

	/** Um item da lista: uma ou mais linhas para o período. */
	@FunctionalInterface
	private interface Item {

		List<LinhaTransparencia> calcular(Periodo periodo);

	}

	private final List<Item> itens;

	IndicadoresDaTransparencia(UsuariosAtendidos atendidos, ListaEspera listaEspera, Atividades atividades,
			ContagensInstitucionais institucional) {
		// PENDENTE-CESJO(P9): conteúdo provisório do relatório de transparência, sem modelo da equipe (ADR 0210).
		this.itens = List.of(
				um(USUARIOS, "Usuários atendidos no período", p -> atendidos.contarAtendidosNoPeriodo(p.inicio(), p.fim())),
				um(USUARIOS, "Inserções no serviço", p -> atendidos.contarInsercoes(p.inicio(), p.fim())),
				um(USUARIOS, "Usuários ativos no último dia", p -> atendidos.contarAtivosEm(p.fim())),

				um(LISTA_ESPERA, "Novas inscrições", p -> listaEspera.contarInscricoes(p.inicio(), p.fim())),
				um(LISTA_ESPERA, "Pessoas aguardando vaga no último dia", p -> listaEspera.contarNaFilaEm(p.fim())),

				porCategoria(ATIVIDADES, TipoAtividade.values(), RotulosAtividade::de,
						p -> atividades.contarPorTipo(p.inicio(), p.fim())),

				um(PARCERIAS, "Parcerias vigentes no período", p -> institucional.contarParceriasVigentes(p.inicio(), p.fim())),
				um(PARCERIAS, "Ações de articulação com parceiros",
						p -> institucional.contarArticulacoes(p.inicio(), p.fim())),

				porCategoria(EVENTOS, TipoEventoInstitucional.values(), RotulosInstitucional::de,
						p -> institucional.contarEventosPorTipo(p.inicio(), p.fim())),

				porCategoria(DOACOES_RECEBIDAS, TipoDoacao.values(), RotulosInstitucional::de,
						p -> institucional.contarDoacoesPorTipo(DirecaoDoacao.RECEBIDA, p.inicio(), p.fim())),
				p -> List.of(LinhaTransparencia.reais(DOACOES_RECEBIDAS, "Valor recebido em dinheiro",
						institucional.somarDoacoesEmDinheiro(DirecaoDoacao.RECEBIDA, p.inicio(), p.fim()))),
				um(DOACOES_REALIZADAS, "Doações feitas", p -> institucional
					.contarDoacoesPorTipo(DirecaoDoacao.REALIZADA, p.inicio(), p.fim())
					.values()
					.stream()
					.mapToLong(Long::longValue)
					.sum()),

				porCategoria(EQUIPE, FuncaoColaborador.values(), RotulosInstitucional::de,
						p -> institucional.contarColaboradoresPorFuncaoEm(p.fim())));
	}

	/**
	 * Todas as linhas do relatório no período, na ordem da lista. Uma transação só de leitura para o
	 * relatório todo: as contagens usam a mesma conexão do pool em vez de uma por indicador (ADR 0233).
	 */
	@Transactional(readOnly = true)
	List<LinhaTransparencia> calcular(Periodo periodo) {
		List<LinhaTransparencia> linhas = new ArrayList<>();
		for (Item item : itens) {
			linhas.addAll(item.calcular(periodo));
		}
		return List.copyOf(linhas);
	}

	/** Um número só. */
	private static Item um(String grupo, String nome, ToLongFunction<Periodo> conta) {
		return periodo -> List.of(LinhaTransparencia.quantidade(grupo, nome, conta.applyAsLong(periodo)));
	}

	/** Uma linha por categoria, na ordem dada, com zero para a categoria sem registro. */
	private static <E> Item porCategoria(String grupo, E[] categorias, Function<E, String> rotulo,
			Function<Periodo, Map<E, Long>> conta) {
		return periodo -> {
			Map<E, Long> totais = conta.apply(periodo);
			List<LinhaTransparencia> linhas = new ArrayList<>();
			for (E categoria : categorias) {
				linhas.add(LinhaTransparencia.quantidade(grupo, rotulo.apply(categoria), totais.getOrDefault(categoria, 0L)));
			}
			return linhas;
		};
	}

}
