package br.org.cesjo.sgi.demonstracao;

import java.time.LocalDate;
import java.util.Map;

import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.arquivos.Arquivos;
import br.org.cesjo.sgi.compartilhado.criptografia.Cifrador;

/**
 * Na subida com o perfil {@code demo}, grava a massa de demonstração quando o banco ainda não tem
 * nenhuma pessoa nem conta (ADR 0237). Cria uma conta por perfil, todas com a mesma senha
 * ({@code sgi.demonstracao.senha}, padrão {@value #SENHA_PADRAO}) e sem troca obrigatória no primeiro
 * acesso. Roda antes da criação da primeira conta (ADR 0014), que então encontra contas e não faz nada.
 * Recusa subir junto com o perfil {@code prod}.
 */
@Component
@Profile("demo")
@Order(Ordered.HIGHEST_PRECEDENCE)
class CargaDemonstracao implements ApplicationRunner {

	static final String SENHA_PADRAO = "demonstracao";

	private static final Logger log = LoggerFactory.getLogger(CargaDemonstracao.class);

	private final JdbcTemplate jdbc;

	private final Cifrador cifrador;

	private final Arquivos arquivos;

	private final EntityManager entidades;

	private final PasswordEncoder codificador;

	private final Environment ambiente;

	private final String senha;

	CargaDemonstracao(JdbcTemplate jdbc, Cifrador cifrador, Arquivos arquivos, EntityManager entidades,
			PasswordEncoder codificador, Environment ambiente, @Value("${sgi.demonstracao.senha:" + SENHA_PADRAO + "}") String senha) {
		this.jdbc = jdbc;
		this.cifrador = cifrador;
		this.arquivos = arquivos;
		this.entidades = entidades;
		this.codificador = codificador;
		this.ambiente = ambiente;
		this.senha = senha;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments argumentos) {
		if (ambiente.acceptsProfiles(Profiles.of("prod"))) {
			throw new IllegalStateException("O perfil demo grava dados fictícios e não pode rodar com o perfil prod.");
		}
		long pessoas = jdbc.queryForObject("select count(*) from pessoa", Long.class);
		long contas = jdbc.queryForObject("select count(*) from conta_acesso", Long.class);
		if (pessoas > 0 || contas > 0) {
			log.info("Carga de demonstração ignorada: o banco já tem dados ({} pessoas, {} contas).", pessoas, contas);
			return;
		}
		long inicio = System.nanoTime();
		Map<String, Long> totais = new GeradorDemonstracao(jdbc, cifrador, arquivos, entidades, codificador,
				LocalDate.now(GeradorDemonstracao.FUSO))
			.gravar(senha);
		log.info("Carga de demonstração gravada em {} ms: {}", (System.nanoTime() - inicio) / 1_000_000, totais);
		log.info("Contas de demonstração: coordenador, equipe.tecnica, administrativo e consulta, com a senha {}.",
				senha.equals(SENHA_PADRAO) ? "\"" + SENHA_PADRAO + "\"" : "de sgi.demonstracao.senha");
	}

}
