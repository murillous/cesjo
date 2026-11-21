package br.org.cesjo.sgi.compartilhado.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Garante que o tratador global de erros não engole as falhas de permissão como 500 (ADR 0009).
 */
@WebMvcTest(ErrosDeSegurancaTests.ControladorProtegido.class)
@Import({ ErrosDeSegurancaTests.ControladorProtegido.class, ErrosDeSegurancaTests.SegurancaDeTeste.class })
class ErrosDeSegurancaTests {

	@Autowired
	MockMvc mvc;

	@Test
	@WithMockUser(authorities = "OUTRA")
	void semPermissaoResponde403() throws Exception {
		mvc.perform(get("/api/teste/protegido")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(authorities = "PERMITIDO")
	void comPermissaoResponde200() throws Exception {
		mvc.perform(get("/api/teste/protegido")).andExpect(status().isOk());
	}

	@Test
	void semAutenticacaoResponde401() throws Exception {
		mvc.perform(get("/api/teste/protegido")).andExpect(status().isUnauthorized());
	}

	@Configuration(proxyBeanMethods = false)
	@EnableMethodSecurity
	static class SegurancaDeTeste {

	}

	@RestController
	static class ControladorProtegido {

		@GetMapping("/api/teste/protegido")
		@PreAuthorize("hasAuthority('PERMITIDO')")
		String protegido() {
			return "ok";
		}

	}

}
