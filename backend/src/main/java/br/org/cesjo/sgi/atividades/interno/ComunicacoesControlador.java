package br.org.cesjo.sgi.atividades.interno;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.org.cesjo.sgi.arquivos.ConteudoArquivo;
import br.org.cesjo.sgi.arquivos.NovoArquivo;
import br.org.cesjo.sgi.atividades.DirecaoComunicacao;
import br.org.cesjo.sgi.atividades.TipoComunicacao;

/** API dos contatos e comunicações (RF011, ADRs 0160 e 0164, permissões no ADR 0162). */
@RestController
@RequestMapping("/api/comunicacoes")
class ComunicacoesControlador {

	private final ServicoComunicacoes servico;

	ComunicacoesControlador(ServicoComunicacoes servico) {
		this.servico = servico;
	}

	/**
	 * Lista paginada, da mais recente para a mais antiga, sem a descrição. Filtros opcionais; {@code contraparte}
	 * é um trecho do nome da pessoa ou instituição.
	 */
	@GetMapping
	@PreAuthorize("hasAuthority('COMUNICACOES_LER')")
	Page<ResumoComunicacao> listar(@RequestParam(required = false) TipoComunicacao tipo,
			@RequestParam(required = false) DirecaoComunicacao direcao,
			@RequestParam(required = false) LocalDate inicio, @RequestParam(required = false) LocalDate fim,
			@RequestParam(required = false) String contraparte,
			@PageableDefault(size = 20) @SortDefault.SortDefaults({
					@SortDefault(sort = "data", direction = Sort.Direction.DESC),
					@SortDefault(sort = "criadoEm", direction = Sort.Direction.DESC) }) Pageable pagina) {
		return servico.listar(new FiltroComunicacoes(tipo, direcao, inicio, fim, contraparte), pagina);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('COMUNICACOES_LER')")
	ComunicacaoResposta ler(@PathVariable UUID id) {
		return servico.ler(id);
	}

	@PostMapping
	@PreAuthorize("hasAuthority('COMUNICACOES_REGISTRAR')")
	ResponseEntity<ComunicacaoResposta> registrar(@Valid @RequestBody DadosComunicacao dados) {
		ComunicacaoResposta comunicacao = servico.registrar(dados);
		return ResponseEntity.created(URI.create("/api/comunicacoes/" + comunicacao.id())).body(comunicacao);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('COMUNICACOES_REGISTRAR')")
	ComunicacaoResposta corrigir(@PathVariable UUID id, @Valid @RequestBody DadosComunicacao dados) {
		return servico.corrigir(id, dados);
	}

	/** Anexa um PDF, JPG ou PNG ao registro, no lugar do anexo anterior. */
	@PostMapping(path = "/{id}/anexo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAuthority('COMUNICACOES_REGISTRAR')")
	ComunicacaoResposta anexar(@PathVariable UUID id, @RequestPart("arquivo") MultipartFile arquivo) {
		return servico.anexar(id, new NovoArquivo(arquivo.getOriginalFilename(), bytes(arquivo)));
	}

	@DeleteMapping("/{id}/anexo")
	@PreAuthorize("hasAuthority('COMUNICACOES_REGISTRAR')")
	ComunicacaoResposta removerAnexo(@PathVariable UUID id) {
		return servico.removerAnexo(id);
	}

	/** Baixa o anexo. A leitura é auditada pelo módulo Arquivos. */
	@GetMapping("/{id}/anexo")
	@PreAuthorize("hasAuthority('COMUNICACOES_LER')")
	ResponseEntity<byte[]> baixarAnexo(@PathVariable UUID id) {
		ConteudoArquivo arquivo = servico.lerAnexo(id);
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(arquivo.metadados().tipo().tipoMime()))
			.contentLength(arquivo.conteudo().length)
			.header("Content-Disposition",
					ContentDisposition.attachment()
						.filename(arquivo.metadados().nomeOriginal(), StandardCharsets.UTF_8)
						.build()
						.toString())
			.cacheControl(CacheControl.noStore())
			.body(arquivo.conteudo());
	}

	private static byte[] bytes(MultipartFile arquivo) {
		try {
			return arquivo.getBytes();
		}
		catch (IOException erro) {
			throw new UncheckedIOException("Falha ao ler o arquivo enviado", erro);
		}
	}

}
