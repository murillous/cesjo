package br.org.cesjo.sgi.importacao.interno;

import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * API da importação da lista de espera atual (seção 5.2, issue #33, ADR 0215 e ADR 0216). O arquivo vai
 * na parte {@code arquivo} de um {@code multipart/form-data}, nas duas etapas: pré-visualizar e confirmar.
 */
@RestController
@RequestMapping("/api/importacao/lista-espera")
@PreAuthorize("hasAuthority('PLANILHAS_IMPORTAR')")
class ImportacaoListaEsperaControlador {

	private final ServicoImportacaoListaEspera servico;

	private final ModeloListaEspera modelo;

	private final GeradorModelo gerador;

	ImportacaoListaEsperaControlador(ServicoImportacaoListaEspera servico, ModeloListaEspera modelo,
			GeradorModelo gerador) {
		this.servico = servico;
		this.modelo = modelo;
		this.gerador = gerador;
	}

	/** Colunas do modelo, para a tela explicar o que preencher. */
	@GetMapping("/colunas")
	List<Coluna> colunas() {
		return modelo.colunas();
	}

	/** Modelo de planilha vazio, com as instruções, para download. */
	@GetMapping("/modelo")
	ResponseEntity<byte[]> modelo() {
		return ArquivosHttp.planilha(modelo.nomeArquivo(), gerador.gerar(modelo));
	}

	/** Lê a planilha e devolve as linhas com os erros, sem gravar nada. */
	@PostMapping(path = "/previa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	ResponseEntity<Previa> previa(@RequestPart("arquivo") MultipartFile arquivo) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servico.previa(ArquivosHttp.conteudo(arquivo)));
	}

	/** Lê a planilha de novo e grava só as linhas válidas. */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	ResultadoImportacao importar(@RequestPart("arquivo") MultipartFile arquivo) {
		return servico.importar(ArquivosHttp.conteudo(arquivo));
	}

}
