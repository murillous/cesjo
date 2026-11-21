package br.org.cesjo.sgi.atendimento;

import static org.assertj.core.api.Assertions.assertThat;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

/** Agendamento e agenda de visitas domiciliares (issue #34): regras, permissões e auditoria. */
@Import({ TestcontainersConfiguration.class, DadosDeVisita.class })
@SpringBootTest
@AutoConfigureMockMvc
class AgendamentoVisitasTests {

	private static final String URL = "/api/visitas";

	/** Ano bem à frente, para a agenda dos testes não misturar com outras visitas do banco compartilhado. */
	private static final LocalDate DIA = LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusYears(5).withDayOfYear(10);

	@Autowired
	MockMvc mvc;

	@Autowired
	DadosDeVisita dados;

	@MockitoSpyBean
	Auditoria auditoria;

	private final JsonMapper json = JsonMapper.builder().build();

	@BeforeEach
	void limparChamadasDaAuditoria() {
		clearInvocations(auditoria);
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "visita-admin-agenda")
	void agendaVisitaComProfissionaisEColocaCandidatoEmAvaliacao() throws Exception {
		UUID pessoa = dados.candidato("Candidata Fictícia", "AGUARDANDO");
		UUID assistente = dados.colaborador("Bruna Fictícia", true);
		UUID psicologo = dados.colaborador("Abel Fictício", true);

		String corpo = mvc
			.perform(post(URL).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("pessoaId", pessoa, "dataHora", DIA.atTime(14, 30).toString(), "profissionais",
						List.of(assistente, psicologo)))))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", startsWith(URL + "/")))
			.andExpect(jsonPath("$.status").value("AGENDADA"))
			.andExpect(jsonPath("$.nomePessoa").value("Candidata Fictícia"))
			.andExpect(jsonPath("$.dataHora").value(DIA + "T14:30:00"))
			.andExpect(jsonPath("$.profissionais[0].nome").value("Abel Fictício"))
			.andExpect(jsonPath("$.profissionais[1].nome").value("Bruna Fictícia"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String id = JsonPath.read(corpo, "$.id");

		assertThat(dados.situacao(pessoa)).isEqualTo("EM_AVALIACAO");
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.CRIAR
				&& e.entidade().equals("visita_domiciliar") && e.entidadeId().equals(id)));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR
				&& e.entidade().equals("inscricao_lista_espera") && e.camposAlterados().equals(Set.of("situacao"))));

		mvc.perform(get(URL + "/{id}", id)).andExpect(status().isOk()).andExpect(jsonPath("$.pessoaId").value(pessoa.toString()));
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "visita-tecnica-segunda")
	void segundaVisitaDeCandidatoEmAvaliacaoNaoMudaSituacao() throws Exception {
		UUID pessoa = dados.candidato("Candidato Em Avaliação", "EM_AVALIACAO");
		UUID profissional = dados.colaborador("Profissional Fictício", true);

		agendar(pessoa, DIA.atTime(9, 0), List.of(profissional));

		assertThat(dados.situacao(pessoa)).isEqualTo("EM_AVALIACAO");
		verify(auditoria, never()).registrar(argThat(e -> e.entidade().equals("inscricao_lista_espera")));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "visita-coord-regras")
	void recusaPessoaForaDaListaProfissionalInativoEDiaPassado() throws Exception {
		UUID pessoa = dados.candidato("Candidato Regras", "AGUARDANDO");
		UUID ativo = dados.colaborador("Ativo Fictício", true);
		UUID inativo = dados.colaborador("Inativo Fictício", false);

		recusar(Map.of("pessoaId", dados.pessoaSemInscricao(), "dataHora", DIA.atTime(10, 0).toString(),
				"profissionais", List.of(ativo)), "A pessoa escolhida não está na lista de espera.");
		recusar(Map.of("pessoaId", pessoa, "dataHora", DIA.atTime(10, 0).toString(), "profissionais",
				List.of(ativo, inativo)), "Escolha apenas colaboradores ativos.");
		recusar(Map.of("pessoaId", pessoa, "dataHora", LocalDate.now().minusDays(3).atTime(10, 0).toString(),
				"profissionais", List.of(ativo)), "A visita não pode ser agendada para um dia que já passou.");

		mvc.perform(post(URL).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(Map.of("pessoaId", pessoa, "dataHora", DIA.atTime(10, 0).toString(), "profissionais",
					List.of()))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("profissionais")));
		assertThat(dados.situacao(pessoa)).isEqualTo("AGUARDANDO");
		verify(auditoria, never()).registrar(any(EventoAuditoria.class));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "visita-coord-agenda")
	void agendaPorPeriodoEmOrdemDeHorario() throws Exception {
		// Dia sorteado: o banco dos testes é compartilhado entre execuções, e a agenda não pode ter visitas antigas.
		LocalDate dia = DIA.plusDays(40 + java.util.concurrent.ThreadLocalRandom.current().nextInt(30_000));
		UUID profissional = dados.colaborador("Agenda Fictícia", true);
		String tarde = agendar(dados.candidato("Visita Tarde", "AGUARDANDO"), dia.atTime(15, 0), List.of(profissional));
		String manha = agendar(dados.candidato("Visita Manhã", "AGUARDANDO"), dia.atTime(8, 0), List.of(profissional));
		String depois = agendar(dados.candidato("Visita Depois", "AGUARDANDO"), dia.plusDays(1).atTime(8, 0),
				List.of(profissional));

		mvc.perform(get(URL).param("de", dia.toString()).param("ate", dia.toString()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(manha))
			.andExpect(jsonPath("$[1].id").value(tarde))
			.andExpect(jsonPath("$[*].id").value(not(hasItem(depois))));
		mvc.perform(get(URL).param("de", dia.toString()).param("ate", dia.plusDays(1).toString()))
			.andExpect(jsonPath("$[2].id").value(depois));
		mvc.perform(get(URL).param("de", dia.toString()).param("ate", dia.minusDays(1).toString()))
			.andExpect(status().isUnprocessableContent());
		mvc.perform(get(URL).param("de", dia.toString()).param("ate", dia.plusDays(62).toString()))
			.andExpect(status().isUnprocessableContent());
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "visita-admin-escolhas")
	void ofereceCandidatosAtivosEProfissionaisAtivos() throws Exception {
		UUID pessoa = dados.candidato("Candidato Escolha", "AGUARDANDO");
		UUID ativo = dados.colaborador("Escolha Ativa", true);
		UUID inativo = dados.colaborador("Escolha Inativa", false);

		mvc.perform(get(URL + "/candidatos"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].pessoaId").value(hasItem(pessoa.toString())));
		mvc.perform(get(URL + "/profissionais"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].id").value(hasItem(ativo.toString())))
			.andExpect(jsonPath("$[*].id").value(not(hasItem(inativo.toString()))));
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "visita-consulta")
	void consultaNaoVeNemAgenda() throws Exception {
		mvc.perform(get(URL).param("de", DIA.toString()).param("ate", DIA.toString())).andExpect(status().isForbidden());
		mvc.perform(get(URL + "/{id}", UUID.randomUUID())).andExpect(status().isForbidden());
		mvc.perform(get(URL + "/candidatos")).andExpect(status().isForbidden());
		mvc.perform(get(URL + "/profissionais")).andExpect(status().isForbidden());
		mvc.perform(post(URL).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(Map.of("pessoaId", UUID.randomUUID(), "dataHora", DIA.atTime(9, 0).toString(),
					"profissionais", List.of(UUID.randomUUID())))))
			.andExpect(status().isForbidden());
	}

	@Test
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "visita-tecnica-404")
	void visitaInexistenteResponde404() throws Exception {
		mvc.perform(get(URL + "/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
	}

	private String agendar(UUID pessoa, LocalDateTime dataHora, List<UUID> profissionais) throws Exception {
		String corpo = mvc
			.perform(post(URL).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("pessoaId", pessoa, "dataHora", dataHora.toString(), "profissionais",
						profissionais))))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(corpo, "$.id");
	}

	private void recusar(Map<String, Object> corpo, String mensagem) throws Exception {
		mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(corpo)))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value(mensagem));
	}

	private String json(Object valor) {
		return json.writeValueAsString(valor);
	}

}
