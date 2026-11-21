package br.org.cesjo.sgi.listaespera.interno;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.org.cesjo.sgi.listaespera.OrigemEncaminhamento;
import br.org.cesjo.sgi.listaespera.SituacaoInscricao;
import br.org.cesjo.sgi.pessoas.TipoDeficiencia;
import br.org.cesjo.sgi.relatorios.FormatoExportacao;

/** Exportação da fila em PDF e Excel com os filtros da tela (ADR 0042 e ADR 0130). */
@RestController
class ExportacaoControlador {

	private final ExportacaoListaEspera exportacao;

	ExportacaoControlador(ExportacaoListaEspera exportacao) {
		this.exportacao = exportacao;
	}

	@GetMapping("/api/lista-espera/exportacao")
	@PreAuthorize("hasAuthority('LISTA_ESPERA_EXPORTAR')")
	ResponseEntity<byte[]> exportar(@RequestParam FormatoExportacao formato,
			@RequestParam(required = false) String nome,
			@RequestParam(required = false) TipoDeficiencia tipoDeficiencia,
			@RequestParam(required = false) OrigemEncaminhamento origem,
			@RequestParam(required = false) SituacaoInscricao situacao) {
		return exportacao.exportar(new FiltroFila(nome, tipoDeficiencia, origem, situacao), formato).comoDownload();
	}

}
