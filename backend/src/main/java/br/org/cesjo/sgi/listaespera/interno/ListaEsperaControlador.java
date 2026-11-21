package br.org.cesjo.sgi.listaespera.interno;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.org.cesjo.sgi.arquivos.ConteudoArquivo;
import br.org.cesjo.sgi.listaespera.OrigemEncaminhamento;
import br.org.cesjo.sgi.listaespera.SituacaoInscricao;
import br.org.cesjo.sgi.pessoas.TipoDeficiencia;
import br.org.cesjo.sgi.pessoas.TipoDocumento;

/** API das inscrições na lista de espera (RF001, RF002, ADR 0037, ADR 0056 e ADR 0058). */
@RestController
@RequestMapping("/api/lista-espera/inscricoes")
class ListaEsperaControlador {

	private static final String DOCUMENTOS_ENVIAR = "DOCUMENTOS_ENVIAR";

	private static final String LISTA_ESPERA_LER = "LISTA_ESPERA_LER";

	private final ServicoListaEspera servico;

	private final FilaListaEspera fila;

	ListaEsperaControlador(ServicoListaEspera servico, FilaListaEspera fila) {
		this.servico = servico;
		this.fila = fila;
	}

	/**
	 * Fila paginada, na ordem de {@link OrdemDaFila}, com a posição. Quem não tem {@code LISTA_ESPERA_LER}
	 * vê só os dados básicos e não filtra por tipo de deficiência nem por origem (ADR 0058).
	 */
	@GetMapping
	@PreAuthorize("hasAnyAuthority('LISTA_ESPERA_LER', 'LISTA_ESPERA_FILA_LER')")
	Page<ItemFila> listar(@RequestParam(required = false) String nome,
			@RequestParam(required = false) TipoDeficiencia tipoDeficiencia,
			@RequestParam(required = false) OrigemEncaminhamento origem,
			@RequestParam(required = false) SituacaoInscricao situacao, Pageable pageable,
			Authentication autenticacao) {
		FiltroFila filtro = new FiltroFila(nome, tipoDeficiencia, origem, situacao);
		boolean completo = tem(autenticacao, LISTA_ESPERA_LER);
		if (!completo && filtro.usaDadoCompleto()) {
			throw new AccessDeniedException("Sem permissão para filtrar por tipo de deficiência ou origem.");
		}
		Page<ItemFila> itens = fila.listar(filtro, pageable);
		return completo ? itens : itens.map(ItemFila::basico);
	}

	@PostMapping
	@PreAuthorize("hasAuthority('LISTA_ESPERA_CADASTRAR')")
	ResponseEntity<InscricaoResposta> inscrever(@Valid @RequestBody NovaInscricao nova, Authentication autenticacao) {
		if (nova.temAnexo() && !tem(autenticacao, DOCUMENTOS_ENVIAR)) {
			throw new AccessDeniedException("Sem permissão para enviar documentos.");
		}
		InscricaoResposta inscricao = servico.inscrever(nova);
		return ResponseEntity.created(URI.create("/api/lista-espera/inscricoes/" + inscricao.id())).body(inscricao);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('LISTA_ESPERA_LER')")
	InscricaoResposta obter(@PathVariable UUID id) {
		return servico.obter(id);
	}

	/** Anexa ou substitui o documento do tipo informado (ADR 0056). */
	@PutMapping("/{id}/documentos/{tipo}")
	@PreAuthorize("hasAuthority('LISTA_ESPERA_LER') and hasAuthority('DOCUMENTOS_ENVIAR')")
	InscricaoResposta anexar(@PathVariable UUID id, @PathVariable TipoDocumento tipo,
			@Valid @RequestBody AnexoDocumento anexo) {
		return servico.anexar(id, tipo, anexo.arquivoId());
	}

	/** Baixa o documento do tipo informado. A leitura é auditada como dado sensível. */
	@GetMapping("/{id}/documentos/{tipo}")
	@PreAuthorize("hasAuthority('LISTA_ESPERA_LER') and hasAuthority('DOCUMENTOS_LER')")
	ResponseEntity<byte[]> baixar(@PathVariable UUID id, @PathVariable TipoDocumento tipo) {
		ConteudoArquivo arquivo = servico.documento(id, tipo);
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

	private static boolean tem(Authentication autenticacao, String permissao) {
		return autenticacao != null && autenticacao.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.anyMatch(permissao::equals);
	}

}
