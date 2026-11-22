package br.org.cesjo.sgi.atividades;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import br.org.cesjo.sgi.TestcontainersConfiguration;
import br.org.cesjo.sgi.acesso.ComConta;
import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.auditoria.AcaoAuditoria;
import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import tools.jackson.databind.json.JsonMapper;

/** Atendimentos e estudos de caso (issue #46): um usuário, texto cifrado e leitura auditada. */
@Import({ TestcontainersConfiguration.class, DadosDeAtividade.class })
@SpringBootTest
@AutoConfigureMockMvc
class AtendimentosEEstudosDeCasoTests {

	private static final String URL = "/api/atividades";

	private static final LocalDate HOJE = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

	@Autowired
	MockMvc mvc;

	@Autowired
	DadosDeAtividade dados;

	@Autowired
	Atividades atividades;

	@MockitoSpyBean
	Auditoria auditoria;

	private final JsonMapper json = JsonMapper.builder().build();

	@BeforeEach
	void limparChamadasDaAuditoria() {
		clearInvocations(auditoria);
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "atendimento-tecnica-registra")
	void registraAtendimentoCifradoComLeituraAuditada() throws Exception {
		UUID usuario = dados.usuario("Usuário Atendimento Fictício", "ATIVA");
		UUID psicologa = dados.colaborador("Psicóloga Atendimento", true);
		String id = registrar(atividade("ATENDIMENTO_TECNICO_INDIVIDUALIZADO", List.of(usuario), List.of(psicologa)));

		assertThat(dados.descricaoNoBanco(id)).startsWith("v1:").doesNotContain("ansiedade");
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.CRIAR && e.entidade().equals("atividade")));

		mvc.perform(get(URL).param("size", "100")).andExpect(status().isOk());
		verify(auditoria, never()).registrar(argThat(e -> e.acao() == AcaoAuditoria.LER_SENSIVEL));

		mvc.perform(get(URL + "/{id}", id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.descricao").value("Relato de ansiedade (fictício)."))
			.andExpect(jsonPath("$.participantes[0].pessoaId").value(usuario.toString()));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.LER_SENSIVEL
				&& e.entidade().equals("atividade") && e.entidadeId().equals(id)));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "atendimento-coord-coletiva")
	void leituraDeAtividadeColetivaNaoEAuditada() throws Exception {
		UUID usuario = dados.usuario("Usuário Coletiva Leitura", "ATIVA");
		UUID profissional = dados.colaborador("Profissional Coletiva Leitura", true);
		String id = registrar(atividade("COM_USUARIOS", List.of(usuario), List.of(profissional)));

		mvc.perform(get(URL + "/{id}", id)).andExpect(status().isOk());
		verify(auditoria, never()).registrar(argThat(e -> e.acao() == AcaoAuditoria.LER_SENSIVEL));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "atendimento-coord-regras")
	void exigeUmUsuarioEDoisProfissionaisNoMultiprofissional() throws Exception {
		UUID ana = dados.usuario("Ana Estudo Fictícia", "ATIVA");
		UUID bruno = dados.usuario("Bruno Estudo Fictício", "ATIVA");
		UUID psicologa = dados.colaborador("Psicóloga Estudo", true);
		UUID assistente = dados.colaborador("Assistente Estudo", true);

		mvc.perform(post(URL).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(atividade("ESTUDO_DE_CASO", List.of(ana, bruno), List.of(psicologa)))))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("Atendimentos e estudos de caso são de um só usuário atendido."));
		mvc.perform(post(URL).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(atividade("ATENDIMENTO_MULTIPROFISSIONAL", List.of(ana), List.of(psicologa)))))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail")
				.value("O atendimento multiprofissional precisa de pelo menos 2 profissionais."));
		verify(auditoria, never()).registrar(any(EventoAuditoria.class));

		registrar(atividade("ATENDIMENTO_MULTIPROFISSIONAL", List.of(ana), List.of(psicologa, assistente)));
		registrar(atividade("ESTUDO_DE_CASO", List.of(bruno), List.of(psicologa)));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "atendimento-coord-contagem")
	void contaOsTiposIndividuais() throws Exception {
		LocalDate dia = LocalDate.of(2004, 8, 9);
		UUID usuario = dados.usuario("Usuário Contagem Atendimento", "ATIVA");
		UUID profissional = dados.colaborador("Profissional Contagem Atendimento", true);
		long antes = atividades.contarPorTipo(dia, dia).get(TipoAtividade.ESTUDO_DE_CASO);
		Map<String, Object> estudo = atividade("ESTUDO_DE_CASO", List.of(usuario), List.of(profissional));
		estudo.put("data", dia.toString());
		registrar(estudo);

		assertThat(atividades.contarPorTipo(dia, dia).get(TipoAtividade.ESTUDO_DE_CASO)).isEqualTo(antes + 1);
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "atendimento-admin")
	void administrativoNaoLe() throws Exception {
		mvc.perform(get(URL + "/{id}", UUID.randomUUID())).andExpect(status().isForbidden());
		verify(auditoria, never()).registrar(any(EventoAuditoria.class));
	}

	private String registrar(Map<String, Object> corpo) throws Exception {
		String resposta = mvc
			.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(corpo)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(resposta, "$.id");
	}

	private static Map<String, Object> atividade(String tipo, List<UUID> participantes, List<UUID> profissionais) {
		Map<String, Object> mapa = new LinkedHashMap<>();
		mapa.put("tipo", tipo);
		mapa.put("data", HOJE.toString());
		mapa.put("titulo", "Atendimento");
		mapa.put("descricao", "Relato de ansiedade (fictício).");
		mapa.put("participantes", participantes);
		mapa.put("profissionais", profissionais);
		return mapa;
	}

	private String json(Object valor) {
		return json.writeValueAsString(valor);
	}

}
