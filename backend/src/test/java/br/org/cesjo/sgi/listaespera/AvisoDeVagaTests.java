package br.org.cesjo.sgi.listaespera;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import br.org.cesjo.sgi.TestcontainersConfiguration;
import br.org.cesjo.sgi.acesso.ComConta;
import br.org.cesjo.sgi.acesso.Perfil;

/**
 * Aviso de vaga aberta (issue #31, ADR 0095). O banco é compartilhado com outros testes, então cada
 * teste parte da ocupação que o painel informa e conta só as notificações que ele mesmo gerou.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AvisoDeVagaTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	AberturaDeVaga aberturaDeVaga;

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "aviso-capacidade")
	void aumentoDeCapacidadeQueAbreVagaNotificaCoordenadorEAdministrativo() throws Exception {
		inserirInscricao("AGUARDANDO", "2000-01-01");
		long ocupadas = ocupadas();
		definir(ocupadas);
		long antes = avisos();

		definir(ocupadas + 2);
		assertThat(avisos()).isEqualTo(antes + 2);
		List<String> perfis = jdbc.queryForList(
				"select perfil_destino from notificacao where tipo = 'VAGA_ABERTA' order by criada_em desc limit 2",
				String.class);
		assertThat(perfis).containsExactlyInAnyOrder("COORDENADOR", "ADMINISTRATIVO");
		String referencia = jdbc.queryForObject(
				"select referencia from notificacao where tipo = 'VAGA_ABERTA' order by criada_em desc limit 1",
				String.class);
		assertThat(referencia).isEqualTo("/lista-espera/" + primeiroDaFila());
		assertThat(jdbc.queryForObject(
				"select mensagem from notificacao where tipo = 'VAGA_ABERTA' order by criada_em desc limit 1",
				String.class))
			.isEqualTo("Uma vaga foi aberta no serviço (2 vagas disponíveis). Veja o próximo candidato da fila.");

		definir(ocupadas + 3);
		assertThat(avisos()).as("já havia vaga: não avisa de novo").isEqualTo(antes + 2);
		definir(ocupadas);
		assertThat(avisos()).as("reduzir a capacidade não avisa").isEqualTo(antes + 2);
	}

	@Test
	@ComConta(perfil = Perfil.COORDENADOR, login = "aviso-desligamento")
	void vagaLiberadaPorOutroModuloNotificaQuandoSaiDeZero() throws Exception {
		UUID inserida = inserirInscricao("INSERIDO", "2001-01-01");
		UUID outra = inserirInscricao("INSERIDO", "2001-01-02");
		long ocupadas = ocupadas();
		definir(ocupadas);
		long antes = avisos();

		// Simula o desligamento: a vaga fica livre e o módulo de atendidos avisa.
		liberar(inserida);
		aberturaDeVaga.vagasLiberadas(1);
		assertThat(avisos()).isEqualTo(antes + 2);

		liberar(outra);
		aberturaDeVaga.vagasLiberadas(1);
		assertThat(avisos()).as("a vaga já estava aberta").isEqualTo(antes + 2);
	}

	private void liberar(UUID inscricao) {
		jdbc.update("update inscricao_lista_espera set situacao = 'SAIU', motivo_saida = 'OUTRO',"
				+ " data_saida = date '2001-02-01' where id = ?", inscricao);
	}

	private long avisos() {
		return jdbc.queryForObject("select count(*) from notificacao where tipo = 'VAGA_ABERTA'", Long.class);
	}

	private String primeiroDaFila() {
		return jdbc.queryForObject("select id::text from inscricao_lista_espera"
				+ " where situacao in ('AGUARDANDO', 'EM_AVALIACAO') order by data_cadastro, criado_em, id limit 1",
				String.class);
	}

	private long ocupadas() throws Exception {
		String corpo = mvc.perform(get("/api/lista-espera/vagas")).andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(corpo, "$.ocupadas")).longValue();
	}

	private void definir(long vagas) throws Exception {
		mvc.perform(put("/api/lista-espera/capacidade").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"vagas\": " + vagas + "}")).andExpect(status().isOk());
	}

	private UUID inserirInscricao(String situacao, String dataCadastro) {
		UUID pessoa = UUID.randomUUID();
		UUID inscricao = UUID.randomUUID();
		jdbc.update("insert into pessoa (id, nome_completo, tipo_deficiencia, telefone, criado_em, atualizado_em)"
				+ " values (?, 'Pessoa Fictícia do Aviso', 'TEA', '3132345678', now(), now())", pessoa);
		jdbc.update("insert into inscricao_lista_espera (id, pessoa_id, data_cadastro, origem_encaminhamento, situacao,"
				+ " criado_em, atualizado_em) values (?, ?, ?::date, 'CRAS', ?, now(), now())", inscricao, pessoa,
				dataCadastro, situacao);
		return inscricao;
	}

}
