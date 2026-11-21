package br.org.cesjo.sgi.listaespera.interno;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.listaespera.OrigemEncaminhamento;
import br.org.cesjo.sgi.pessoas.TipoDeficiencia;
import br.org.cesjo.sgi.relatorios.ArquivoExportado;
import br.org.cesjo.sgi.relatorios.Exportacao;
import br.org.cesjo.sgi.relatorios.FiltroAplicado;
import br.org.cesjo.sgi.relatorios.FormatoExportacao;
import br.org.cesjo.sgi.relatorios.TabelaExportacao;

/**
 * Exportação da fila em PDF e Excel, com os mesmos filtros e a mesma ordem da tela (seção 5.2,
 * ADR 0130). Leva só as colunas da fila: sem contato, responsável nem documentos. A auditoria é feita
 * pela {@link Exportacao} (ADR 0043).
 */
@Service
class ExportacaoListaEspera {

	static final String ENTIDADE = "lista_espera";

	static final String TITULO = "Lista de espera";

	private static final List<String> COLUNAS = List.of("Posição", "Nome", "Tipo de deficiência",
			"Origem do encaminhamento", "Data de cadastro", "Situação", "Cadastro");

	private final FilaListaEspera fila;

	private final Exportacao exportacao;

	ExportacaoListaEspera(FilaListaEspera fila, Exportacao exportacao) {
		this.fila = fila;
		this.exportacao = exportacao;
	}

	@Transactional(readOnly = true)
	ArquivoExportado exportar(FiltroFila filtro, FormatoExportacao formato) {
		List<List<Object>> linhas = fila.todos(filtro).stream().map(ExportacaoListaEspera::linha).toList();
		return exportacao.exportar(ENTIDADE, new TabelaExportacao(TITULO, COLUNAS, linhas, filtros(filtro)), formato);
	}

	private static List<Object> linha(ItemFila item) {
		return Arrays.asList(item.posicao(), item.nomeCompleto(), rotulo(item.tipoDeficiencia()),
				rotulo(item.origemEncaminhamento()), item.dataCadastro(), item.situacao().rotulo(),
				Boolean.TRUE.equals(item.cadastroCompleto()) ? "Completo" : "Incompleto");
	}

	private static List<FiltroAplicado> filtros(FiltroFila filtro) {
		List<FiltroAplicado> filtros = new ArrayList<>();
		if (filtro.nome() != null && !filtro.nome().isBlank()) {
			filtros.add(new FiltroAplicado("Nome contém", filtro.nome().strip()));
		}
		if (filtro.tipoDeficiencia() != null) {
			filtros.add(new FiltroAplicado("Tipo de deficiência", filtro.tipoDeficiencia().rotulo()));
		}
		if (filtro.origem() != null) {
			filtros.add(new FiltroAplicado("Origem do encaminhamento", filtro.origem().rotulo()));
		}
		if (filtro.situacao() != null) {
			filtros.add(new FiltroAplicado("Situação", filtro.situacao().rotulo()));
		}
		return filtros;
	}

	private static String rotulo(TipoDeficiencia tipo) {
		return tipo == null ? null : tipo.rotulo();
	}

	private static String rotulo(OrigemEncaminhamento origem) {
		return origem == null ? null : origem.rotulo();
	}

}
