package br.org.cesjo.sgi.atividades;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

/** Atividades com usuários e famílias (issue #45): registro, correção, permissões e contagem. */
@Import({ TestcontainersConfiguration.class, DadosDeAtividade.class })
@SpringBootTest
@AutoConfigureMockMvc
class AtividadesTests {

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
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "atividade-tecnica-registra")
	void registraAtividadeComDescricaoCifrada() throws Exception {
		UUID ana = dados.usuario("Ana Atividade Fictícia", "ATIVA");
		UUID bruno = dados.usuario("Bruno Atividade Fictício", "ATIVA");
		UUID profissional = dados.colaborador("Terapeuta Atividade", true);

		String corpo = mvc
			.perform(post(URL).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(atividade("COM_USUARIOS_E_FAMILIAS", HOJE, List.of(bruno, ana), List.of(profissional)))))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", startsWith(URL + "/")))
			.andExpect(jsonPath("$.tipo").value("COM_USUARIOS_E_FAMILIAS"))
			.andExpect(jsonPath("$.titulo").value("Oficina de música"))
			.andExpect(jsonPath("$.descricao").value("Roda de música com as famílias (fictício)."))
			.andExpect(jsonPath("$.participantes[0].nomeCompleto").value("Ana Atividade Fictícia"))
			.andExpect(jsonPath("$.participantes[1].nomeCompleto").value("Bruno Atividade Fictício"))
			.andExpect(jsonPath("$.profissionais[0].nome").value("Terapeuta Atividade"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String id = JsonPath.read(corpo, "$.id");

		assertThat(dados.descricaoNoBanco(id)).startsWith("v1:").doesNotContain("música");
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.CRIAR && e.entidade().equals("atividade")));

		mvc.perform(get(URL).param("size", "100"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].id").value(hasItem(id)))
			.andExpect(jsonPath("$.content[0].descricao").doesNotExist());
		mvc.perform(get(URL + "/{id}", id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.descricao").value("Roda de música com as famílias (fictício)."));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "atividade-coord-corrige")
	void corrigeEAuditaSoOQueMudou() throws Exception {
		UUID usuario = dados.usuario("Usuário Corrige Atividade", "ATIVA");
		UUID outro = dados.usuario("Outro Corrige Atividade", "ATIVA");
		UUID profissional = dados.colaborador("Profissional Corrige", true);
		String id = registrar(atividade("COM_USUARIOS", HOJE, List.of(usuario), List.of(profissional)));
		clearInvocations(auditoria);

		Map<String, Object> corrigida = atividade("COM_USUARIOS", HOJE, List.of(usuario, outro), List.of(profissional));
		corrigida.put("titulo", "Passeio ao parque");
		mvc.perform(put(URL + "/{id}", id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(corrigida)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.titulo").value("Passeio ao parque"))
			.andExpect(jsonPath("$.participantes.length()").value(2));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR
				&& e.camposAlterados().equals(Set.of("titulo", "participantes"))));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "atividade-coord-desligado")
	void mantemParticipanteDesligadoNaCorrecaoMasNaoAceitaNovo() throws Exception {
		UUID usuario = dados.usuario("Usuário Antes Ativo", "ATIVA");
		UUID profissional = dados.colaborador("Profissional Desligado", true);
		String id = registrar(atividade("COM_FAMILIAS", HOJE, List.of(usuario), List.of(profissional)));
		UUID desligado = dados.usuario("Usuário Desligado Atividade", "DESLIGADA");

		mvc.perform(put(URL + "/{id}", id).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(atividade("COM_FAMILIAS", HOJE, List.of(usuario, desligado), List.of(profissional)))))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("Escolha somente usuários atendidos ativos."));
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "atividade-tecnica-regras")
	void recusaAtividadeFuturaVaziaOuComEscolhaInvalida() throws Exception {
		UUID usuario = dados.usuario("Usuário Regras Atividade", "ATIVA");
		UUID desligado = dados.usuario("Desligado Regras Atividade", "DESLIGADA");
		UUID ativo = dados.colaborador("Ativo Regras Atividade", true);
		UUID inativo = dados.colaborador("Inativo Regras Atividade", false);

		mvc.perform(post(URL).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(atividade("COM_USUARIOS", HOJE.plusDays(1), List.of(), List.of()))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("data")))
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("participantes")))
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("profissionais")));
		mvc.perform(post(URL).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(atividade("COM_USUARIOS", HOJE, List.of(desligado), List.of(ativo)))))
			.andExpect(status().isUnprocessableContent());
		mvc.perform(post(URL).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(atividade("COM_USUARIOS", HOJE, List.of(UUID.randomUUID()), List.of(ativo)))))
			.andExpect(status().isUnprocessableContent());
		mvc.perform(post(URL).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(atividade("COM_USUARIOS", HOJE, List.of(usuario), List.of(inativo)))))
			.andExpect(status().isUnprocessableContent());
		verify(auditoria, never()).registrar(any(EventoAuditoria.class));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "atividade-coord-opcoes")
	void ofereceSoUsuariosEProfissionaisAtivos() throws Exception {
		UUID ativo = dados.usuario("Usuário Opção Ativo", "ATIVA");
		UUID desligado = dados.usuario("Usuário Opção Desligado", "DESLIGADA");
		UUID profissional = dados.colaborador("Profissional Opção", true);
		UUID inativo = dados.colaborador("Profissional Opção Inativo", false);

		mvc.perform(get(URL + "/opcoes"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.usuarios[*].pessoaId").value(hasItem(ativo.toString())))
			.andExpect(jsonPath("$.usuarios[*].pessoaId").value(not(hasItem(desligado.toString()))))
			.andExpect(jsonPath("$.profissionais[*].id").value(hasItem(profissional.toString())))
			.andExpect(jsonPath("$.profissionais[*].id").value(not(hasItem(inativo.toString()))));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "atividade-coord-contagem")
	void contaPorTipoNoPeriodo() throws Exception {
		LocalDate dia = LocalDate.of(2003, 5, 14);
		UUID usuario = dados.usuario("Usuário Contagem", "ATIVA");
		UUID profissional = dados.colaborador("Profissional Contagem", true);
		Map<TipoAtividade, Long> antes = atividades.contarPorTipo(dia, dia);
		registrar(atividade("COM_FAMILIAS", dia, List.of(usuario), List.of(profissional)));
		registrar(atividade("COM_FAMILIAS", dia, List.of(usuario), List.of(profissional)));
		registrar(atividade("COM_USUARIOS", dia.minusDays(1), List.of(usuario), List.of(profissional)));

		Map<TipoAtividade, Long> depois = atividades.contarPorTipo(dia, dia);
		assertThat(depois).containsOnlyKeys(TipoAtividade.values());
		assertThat(depois.get(TipoAtividade.COM_FAMILIAS)).isEqualTo(antes.get(TipoAtividade.COM_FAMILIAS) + 2);
		assertThat(depois.get(TipoAtividade.COM_USUARIOS)).isEqualTo(antes.get(TipoAtividade.COM_USUARIOS));
		assertThatThrownBy(() -> atividades.contarPorTipo(dia, dia.minusDays(1)))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "atividade-admin")
	void administrativoNaoTemAcesso() throws Exception {
		semAcesso();
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "atividade-consulta")
	void consultaNaoTemAcesso() throws Exception {
		semAcesso();
	}

	private void semAcesso() throws Exception {
		mvc.perform(get(URL)).andExpect(status().isForbidden());
		mvc.perform(get(URL + "/{id}", UUID.randomUUID())).andExpect(status().isForbidden());
		mvc.perform(get(URL + "/opcoes")).andExpect(status().isForbidden());
		mvc.perform(post(URL).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(atividade("COM_USUARIOS", HOJE, List.of(UUID.randomUUID()), List.of(UUID.randomUUID())))))
			.andExpect(status().isForbidden());
		mvc.perform(put(URL + "/{id}", UUID.randomUUID()).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(atividade("COM_USUARIOS", HOJE, List.of(UUID.randomUUID()), List.of(UUID.randomUUID())))))
			.andExpect(status().isForbidden());
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

	private static Map<String, Object> atividade(String tipo, LocalDate data, List<UUID> participantes,
			List<UUID> profissionais) {
		Map<String, Object> mapa = new LinkedHashMap<>();
		mapa.put("tipo", tipo);
		mapa.put("data", data.toString());
		mapa.put("titulo", " Oficina de música ");
		mapa.put("descricao", "Roda de música com as famílias (fictício).");
		mapa.put("participantes", participantes);
		mapa.put("profissionais", profissionais);
		return mapa;
	}

	private String json(Object valor) {
		return json.writeValueAsString(valor);
	}

}
