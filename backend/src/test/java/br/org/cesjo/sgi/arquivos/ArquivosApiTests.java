package br.org.cesjo.sgi.arquivos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import br.org.cesjo.sgi.TestcontainersConfiguration;
import br.org.cesjo.sgi.acesso.ComConta;
import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.arquivos.interno.PropriedadesArquivos;
import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.criptografia.Cifrador;
import tools.jackson.databind.json.JsonMapper;

/** Fluxo da API de arquivos: envio, download, validação, permissões e auditoria (issue #18). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ArquivosApiTests {

	private static final int DEZ_MB = 10 * 1024 * 1024;

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	Cifrador cifrador;

	@Autowired
	PropriedadesArquivos propriedades;

	@Autowired
	MultipartProperties multipart;

	@MockitoSpyBean
	Auditoria auditoria;

	private final JsonMapper json = JsonMapper.builder().build();

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "arquivos-envio")
	void enviaPdfCifradoComMetadadosESha256NoBanco() throws Exception {
		byte[] pdf = ExemplosDeArquivo.pdf();

		MvcResult resultado = mvc
			.perform(multipart("/api/arquivos").file(arquivo("laudo-ficticio.pdf", "application/pdf", pdf))
				.with(csrf()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.nome").value("laudo-ficticio.pdf"))
			.andExpect(jsonPath("$.tipoMime").value("application/pdf"))
			.andExpect(jsonPath("$.tamanho").value(pdf.length))
			.andExpect(jsonPath("$.sha256").value(sha256(pdf)))
			.andReturn();
		UUID id = idDe(resultado);
		assertThat(resultado.getResponse().getHeader("Location")).isEqualTo("/api/arquivos/" + id);

		Map<String, Object> linha = jdbc.queryForMap("select * from arquivo where id = ?", id);
		assertThat(linha.get("tipo_mime")).isEqualTo("application/pdf");
		assertThat(linha.get("tamanho")).isEqualTo((long) pdf.length);
		assertThat(linha.get("sha256")).isEqualTo(sha256(pdf));
		assertThat(linha.get("chave_id")).isEqualTo(cifrador.chaveAtiva());
		assertThat(linha.get("enviado_por")).isNotNull();
		assertThat((String) linha.get("nome_original")).startsWith("v1:").doesNotContain("laudo");

		Path noDisco = propriedades.diretorio().resolve((String) linha.get("caminho_armazenamento"));
		byte[] gravado = Files.readAllBytes(noDisco);
		assertThat(noDisco.getFileName().toString()).isEqualTo(id.toString());
		assertThat(new String(gravado, StandardCharsets.ISO_8859_1)).doesNotContain("%PDF").doesNotContain("fictício");
		assertThat(cifrador.decifrar(gravado)).isEqualTo(pdf);

		verify(auditoria).registrar(EventoAuditoria.criacao("arquivo", id));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "arquivos-download")
	void baixaOConteudoOriginalERegistraLeituraSensivel() throws Exception {
		byte[] png = ExemplosDeArquivo.png();
		UUID id = enviar("foto ação.png", png);

		mvc.perform(get("/api/arquivos/{id}", id))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.IMAGE_PNG))
			.andExpect(content().bytes(png))
			.andExpect(header().string("Content-Disposition",
					"attachment; filename=\"foto acao.png\"; filename*=UTF-8''foto%20a%C3%A7%C3%A3o.png"))
			.andExpect(header().string("Cache-Control", "no-store"))
			.andExpect(header().string("X-Content-Type-Options", "nosniff"));

		verify(auditoria).registrar(EventoAuditoria.leituraSensivel("arquivo", id));
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "arquivos-jpeg")
	void aceitaJpegENormalizaExtensaoEnganosa() throws Exception {
		mvc.perform(multipart("/api/arquivos").file(arquivo("imagem.exe", "application/octet-stream",
				ExemplosDeArquivo.jpeg()))
			.with(csrf()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.tipoMime").value("image/jpeg"))
			.andExpect(jsonPath("$.nome").value("imagem.exe.jpg"));
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "arquivos-tipo")
	void recusaConteudoQueNaoEPdfMesmoComExtensaoEContentTypeDePdf() throws Exception {
		mvc.perform(multipart("/api/arquivos")
			.file(arquivo("falso.pdf", "application/pdf", "<html>não é pdf</html>".getBytes(StandardCharsets.UTF_8)))
			.with(csrf()))
			.andExpect(status().isUnprocessableContent())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("Tipo de arquivo não aceito. Envie um arquivo PDF, JPG ou PNG."));
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "arquivos-limite")
	void aceitaExatamenteDezMbERecusaUmByteAMais() throws Exception {
		mvc.perform(multipart("/api/arquivos")
			.file(arquivo("limite.pdf", "application/pdf", ExemplosDeArquivo.pdfComTamanho(DEZ_MB)))
			.with(csrf())).andExpect(status().isCreated());

		mvc.perform(multipart("/api/arquivos")
			.file(arquivo("grande.pdf", "application/pdf", ExemplosDeArquivo.pdfComTamanho(DEZ_MB + 1)))
			.with(csrf()))
			.andExpect(status().isUnprocessableContent())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("O arquivo ultrapassa o limite de 10 MB."));
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "arquivos-vazio")
	void recusaArquivoVazio() throws Exception {
		mvc.perform(multipart("/api/arquivos").file(arquivo("vazio.pdf", "application/pdf", new byte[0])).with(csrf()))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("O arquivo está vazio."));
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "arquivos-sem-parte")
	void recusaRequisicaoSemOArquivo() throws Exception {
		mvc.perform(multipart("/api/arquivos").with(csrf()))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Parte obrigatória ausente"))
			.andExpect(jsonPath("$.detail").value("A parte arquivo da requisição é obrigatória."));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "arquivos-404")
	void arquivoInexistenteResponde404() throws Exception {
		mvc.perform(get("/api/arquivos/{id}", UUID.randomUUID()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Arquivo não encontrado."));
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "arquivos-consulta-envio")
	void consultaNaoEnvia() throws Exception {
		mvc.perform(multipart("/api/arquivos").file(arquivo("x.pdf", "application/pdf", ExemplosDeArquivo.pdf()))
			.with(csrf())).andExpect(status().isForbidden());
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "arquivos-consulta-download")
	void consultaNaoBaixa() throws Exception {
		mvc.perform(get("/api/arquivos/{id}", UUID.randomUUID())).andExpect(status().isForbidden());
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "arquivos-equipe")
	void equipeTecnicaLeMasNaoEnvia() throws Exception {
		mvc.perform(multipart("/api/arquivos").file(arquivo("x.pdf", "application/pdf", ExemplosDeArquivo.pdf()))
			.with(csrf())).andExpect(status().isForbidden());
		mvc.perform(get("/api/arquivos/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
	}

	@Test
	void limiteDoMultipartAcompanhaOLimiteDosArquivos() {
		assertThat(multipart.getMaxFileSize()).isEqualTo(propriedades.tamanhoMaximo());
		assertThat(multipart.getMaxRequestSize().toBytes()).isGreaterThan(propriedades.tamanhoMaximo().toBytes());
	}

	private UUID enviar(String nome, byte[] conteudo) throws Exception {
		MvcResult resultado = mvc
			.perform(multipart("/api/arquivos").file(arquivo(nome, "application/octet-stream", conteudo)).with(csrf()))
			.andExpect(status().isCreated())
			.andReturn();
		return idDe(resultado);
	}

	private UUID idDe(MvcResult resultado) throws Exception {
		return UUID.fromString(json.readTree(resultado.getResponse().getContentAsString()).get("id").asString());
	}

	private static MockMultipartFile arquivo(String nome, String tipo, byte[] conteudo) {
		return new MockMultipartFile("arquivo", nome, tipo, conteudo);
	}

	private static String sha256(byte[] conteudo) throws Exception {
		return java.util.HexFormat.of()
			.formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(conteudo));
	}

}
