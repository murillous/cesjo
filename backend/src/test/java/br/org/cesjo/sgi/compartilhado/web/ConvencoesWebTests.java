package br.org.cesjo.sgi.compartilhado.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Prova as convenções web da API: erros em {@code ProblemDetail} e paginação padrão. Os filtros de
 * segurança ficam de fora porque o assunto aqui é só o formato das respostas.
 */
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ConvencoesWebTests.ControladorDeTeste.class)
@Import(ConvencoesWebTests.ControladorDeTeste.class)
class ConvencoesWebTests {

	@Autowired
	MockMvc mvc;

	@Test
	void recursoNaoEncontradoRespondeProblemDetail404() throws Exception {
		mvc.perform(get("/api/teste/inexistente"))
			.andExpect(status().isNotFound())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.title").value("Recurso não encontrado"))
			.andExpect(jsonPath("$.detail").value("Pessoa não encontrada."));
	}

	@Test
	void regraNegocioRespondeProblemDetail422() throws Exception {
		mvc.perform(get("/api/teste/regra"))
			.andExpect(status().isUnprocessableContent())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(422))
			.andExpect(jsonPath("$.title").value("Regra de negócio violada"))
			.andExpect(jsonPath("$.detail").value("Não há vagas disponíveis."));
	}

	@Test
	void erroDeValidacaoRespondeProblemDetailComCamposInvalidos() throws Exception {
		mvc.perform(post("/api/teste/validacao").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"nome": " ", "email": "invalido"}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.title").value("Dados inválidos"))
			.andExpect(jsonPath("$.erros", hasSize(2)))
			.andExpect(jsonPath("$.erros[0].campo").value("email"))
			.andExpect(jsonPath("$.erros[0].mensagem").value("deve ser um endereço de e-mail bem formado"))
			.andExpect(jsonPath("$.erros[1].campo").value("nome"))
			.andExpect(jsonPath("$.erros[1].mensagem").value("não deve estar em branco"));
	}

	@Test
	void excecaoPadraoDoSpringRespondeProblemDetailEmPortugues() throws Exception {
		mvc.perform(post("/api/teste/regra"))
			.andExpect(status().isMethodNotAllowed())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Método não permitido"))
			.andExpect(jsonPath("$.detail").value("O método POST não é aceito neste endereço."));
	}

	@Test
	void erroInesperadoRespondeProblemDetail500SemExporDetalhes() throws Exception {
		mvc.perform(get("/api/teste/falha"))
			.andExpect(status().isInternalServerError())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Erro interno"))
			.andExpect(jsonPath("$.detail").value("Ocorreu um erro inesperado. Tente novamente mais tarde."));
	}

	@Test
	void paginacaoUsaTamanhoPadraoESerializacaoEstavel() throws Exception {
		mvc.perform(get("/api/teste/paginado"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.page.size").value(20))
			.andExpect(jsonPath("$.page.number").value(0))
			.andExpect(jsonPath("$.page.totalElements").value(1))
			.andExpect(jsonPath("$.page.totalPages").value(1));
	}

	@Test
	void paginacaoLimitaTamanhoMaximo() throws Exception {
		mvc.perform(get("/api/teste/paginado").param("size", "500"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.page.size").value(100));
	}

	record Cadastro(@NotBlank String nome, @Email String email) {
	}

	@RestController
	static class ControladorDeTeste {

		@GetMapping("/api/teste/inexistente")
		void inexistente() {
			throw new RecursoNaoEncontradoException("Pessoa não encontrada.");
		}

		@GetMapping("/api/teste/regra")
		void regra() {
			throw new RegraNegocioException("Não há vagas disponíveis.");
		}

		@GetMapping("/api/teste/falha")
		void falha() {
			throw new IllegalStateException("detalhe interno que não pode vazar");
		}

		@PostMapping("/api/teste/validacao")
		void validacao(@Valid @RequestBody Cadastro cadastro) {
		}

		@GetMapping("/api/teste/paginado")
		Page<String> paginado(Pageable pageable) {
			return new PageImpl<>(List.of("item"), pageable, 1);
		}

	}

}
