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
 * API da importação dos usuários atendidos atuais (seção 5.2, issue #62, ADR 0220). Mesmo contrato da
 * importação da lista de espera (ADR 0215): {@code colunas}, {@code modelo}, {@code previa} e a
 * confirmação no {@code POST} da base, com o arquivo na parte {@code arquivo}.
 */
@RestController
@RequestMapping("/api/importacao/atendidos")
@PreAuthorize("hasAuthority('PLANILHAS_IMPORTAR')")
class ImportacaoAtendidosControlador {

	private final ServicoImportacaoAtendidos servico;

	private final ModeloAtendidos modelo;

	private final GeradorModelo gerador;

	ImportacaoAtendidosControlador(ServicoImportacaoAtendidos servico, ModeloAtendidos modelo, GeradorModelo gerador) {
		this.servico = servico;
		this.modelo = modelo;
		this.gerador = gerador;
	}

	@GetMapping("/colunas")
	List<Coluna> colunas() {
		return modelo.colunas();
	}

	@GetMapping("/modelo")
	ResponseEntity<byte[]> modelo() {
		return ArquivosHttp.planilha(modelo.nomeArquivo(), gerador.gerar(modelo));
	}

	@PostMapping(path = "/previa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	ResponseEntity<Previa> previa(@RequestPart("arquivo") MultipartFile arquivo) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servico.previa(ArquivosHttp.conteudo(arquivo)));
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	ResultadoImportacao importar(@RequestPart("arquivo") MultipartFile arquivo) {
		return servico.importar(ArquivosHttp.conteudo(arquivo));
	}

}
