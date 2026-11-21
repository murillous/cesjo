package br.org.cesjo.sgi.atendimento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
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
import org.springframework.test.web.servlet.ResultActions;

import br.org.cesjo.sgi.TestcontainersConfiguration;
import br.org.cesjo.sgi.acesso.ComConta;
import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.auditoria.AcaoAuditoria;
import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import tools.jackson.databind.json.JsonMapper;

/** Realização, cancelamento e reagendamento de visitas (issue #35): regras, cifra, permissões e auditoria. */
@Import({ TestcontainersConfiguration.class, DadosDeVisita.class })
@SpringBootTest
@AutoConfigureMockMvc
class ResultadoVisitasTests {

	private static final String URL = "/api/visitas";

	private static final LocalDate HOJE = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

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
	@ComConta(perfil = Perfil.EQUIPE_TECNICA, login = "resultado-tecnica-realiza")
	void realizaComObservacoesCifradasELeituraAuditada() throws Exception {
		UUID visita = visitaDeOntem();

		acao(visita, "realizar", Map.of("observacoes", "  Família acolhedora; casa adaptada (fictício). "))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("REALIZADA"))
			.andExpect(jsonPath("$.temObservacoes").value(true))
			.andExpect(jsonPath("$.observacoes").doesNotExist());

		assertThat(dados.observacoesNoBanco(visita)).startsWith("v1:").doesNotContain("Família");
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR
				&& e.entidadeId().equals(visita.toString()) && e.camposAlterados().equals(Set.of("status", "observacoes"))));

		mvc.perform(get(URL + "/{id}/observacoes", visita))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.observacoes").value("Família acolhedora; casa adaptada (fictício)."));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.LER_SENSIVEL
				&& e.entidade().equals("visita_domiciliar") && e.entidadeId().equals(visita.toString())));
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "resultado-coord-regras")
	void soRealizaVisitaAgendadaDeHojeOuAntes() throws Exception {
		UUID futura = dados.visita(dados.candidato("Visita Futura", "EM_AVALIACAO"), HOJE.plusDays(3).atTime(9, 0),
				dados.colaborador("Profissional Regras", true));
		acao(futura, "realizar", Map.of())
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("Uma visita marcada para um dia futuro ainda não pode ser realizada."));

		UUID visita = visitaDeOntem();
		acao(visita, "realizar", Map.of()).andExpect(status().isOk()).andExpect(jsonPath("$.temObservacoes").value(false));
		acao(visita, "cancelar", Map.of("motivo", "Engano"))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("Só uma visita agendada pode ser realizada, cancelada ou reagendada."));
		acao(UUID.randomUUID(), "realizar", Map.of()).andExpect(status().isNotFound());
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "resultado-admin-cancela")
	void administrativoCancelaComMotivoMasNaoRealiza() throws Exception {
		UUID pessoa = dados.candidato("Candidato Cancelado", "EM_AVALIACAO");
		UUID visita = dados.visita(pessoa, HOJE.plusDays(2).atTime(10, 0), dados.colaborador("Prof Cancela", true));

		acao(visita, "realizar", Map.of()).andExpect(status().isForbidden());
		mvc.perform(get(URL + "/{id}/observacoes", visita)).andExpect(status().isForbidden());
		acao(visita, "cancelar", Map.of("motivo", " "))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[*].campo").value(hasItem("motivo")));
		verify(auditoria, never()).registrar(any(EventoAuditoria.class));

		acao(visita, "cancelar", Map.of("motivo", "Família pediu outra data"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CANCELADA"))
			.andExpect(jsonPath("$.motivoCancelamento").value("Família pediu outra data"));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR
				&& e.camposAlterados().equals(Set.of("status", "motivo_cancelamento"))));
		// PENDENTE-CESJO(P4): cancelar não devolve o candidato para aguardando (ADR 0049).
		assertThat(dados.situacao(pessoa)).isEqualTo("EM_AVALIACAO");
	}

	@Test
	@ComConta(perfil = Perfil.ADMINISTRATIVO, login = "resultado-admin-reagenda")
	void reagendarCancelaAVisitaECriaOutraLigada() throws Exception {
		UUID pessoa = dados.candidato("Candidato Reagendado", "EM_AVALIACAO");
		UUID profissional = dados.colaborador("Prof Reagenda", true);
		UUID visita = dados.visita(pessoa, HOJE.plusDays(1).atTime(10, 0), profissional);
		LocalDateTime novaData = HOJE.plusDays(8).atTime(15, 0);

		String corpo = acao(visita, "reagendar",
				Map.of("dataHora", novaData.toString(), "profissionais", List.of(profissional), "motivo", "Chuva forte"))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", startsWith(URL + "/")))
			.andExpect(jsonPath("$.status").value("AGENDADA"))
			.andExpect(jsonPath("$.pessoaId").value(pessoa.toString()))
			.andExpect(jsonPath("$.visitaOrigemId").value(visita.toString()))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String nova = JsonPath.read(corpo, "$.id");

		mvc.perform(get(URL + "/{id}", visita))
			.andExpect(jsonPath("$.status").value("CANCELADA"))
			.andExpect(jsonPath("$.motivoCancelamento").value("Chuva forte"))
			.andExpect(jsonPath("$.reagendamentoId").value(nova));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.CRIAR && e.entidadeId().equals(nova)));
		verify(auditoria).registrar(argThat(e -> e.acao() == AcaoAuditoria.ALTERAR
				&& e.entidadeId().equals(visita.toString())));

		acao(visita, "reagendar",
				Map.of("dataHora", novaData.toString(), "profissionais", List.of(profissional), "motivo", "De novo"))
			.andExpect(status().isUnprocessableContent());
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "resultado-coord-reagenda-invalido")
	void reagendamentoInvalidoNaoCancelaAVisita() throws Exception {
		UUID profissional = dados.colaborador("Prof Válido", true);
		UUID inativo = dados.colaborador("Prof Inativo", false);
		UUID visita = dados.visita(dados.candidato("Candidato Inválido", "EM_AVALIACAO"), HOJE.plusDays(1).atTime(9, 0),
				profissional);

		acao(visita, "reagendar", Map.of("dataHora", HOJE.plusDays(5).atTime(9, 0).toString(), "profissionais",
				List.of(inativo), "motivo", "Troca"))
			.andExpect(status().isUnprocessableContent());
		acao(visita, "reagendar", Map.of("dataHora", HOJE.minusDays(5).atTime(9, 0).toString(), "profissionais",
				List.of(profissional), "motivo", "Troca"))
			.andExpect(status().isUnprocessableContent());
		mvc.perform(get(URL + "/{id}", visita)).andExpect(jsonPath("$.status").value("AGENDADA"));
		verify(auditoria, never()).registrar(any(EventoAuditoria.class));
	}

	@Test
	@ComConta(perfil = Perfil.CONSULTA, login = "resultado-consulta")
	void consultaNaoMexeEmVisitas() throws Exception {
		UUID visita = UUID.randomUUID();
		acao(visita, "realizar", Map.of()).andExpect(status().isForbidden());
		acao(visita, "cancelar", Map.of("motivo", "x")).andExpect(status().isForbidden());
		acao(visita, "reagendar", Map.of("dataHora", HOJE.plusDays(1).atTime(9, 0).toString(), "profissionais",
				List.of(UUID.randomUUID()), "motivo", "x"))
			.andExpect(status().isForbidden());
		mvc.perform(get(URL + "/{id}/observacoes", visita)).andExpect(status().isForbidden());
	}

	private UUID visitaDeOntem() {
		return dados.visita(dados.candidato("Candidato Visitado", "EM_AVALIACAO"), HOJE.minusDays(1).atTime(9, 30),
				dados.colaborador("Profissional Visita", true));
	}

	private ResultActions acao(UUID visita, String acao, Map<String, Object> corpo) throws Exception {
		return mvc.perform(post(URL + "/{id}/" + acao, visita).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(corpo)));
	}

}
