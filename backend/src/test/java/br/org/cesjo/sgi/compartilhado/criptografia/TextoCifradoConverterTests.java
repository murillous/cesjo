package br.org.cesjo.sgi.compartilhado.criptografia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.hibernate.SpringBeanContainer;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import br.org.cesjo.sgi.TestcontainersConfiguration;
import br.org.cesjo.testes.criptografia.RegistroComCampoCifrado;

/**
 * Confirma que o {@link TextoCifradoConverter} grava texto cifrado no banco e devolve texto claro na
 * entidade. A tabela e a entidade existem só neste teste: a tabela é criada aqui, e a entidade entra
 * numa unidade de persistência própria, com o conversor criado pelo Spring como na aplicação.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TextoCifradoConverterTests {

	@Autowired
	DataSource dataSource;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	ConfigurableListableBeanFactory beanFactory;

	@Autowired
	Cifrador cifrador;

	private LocalContainerEntityManagerFactoryBean fabrica;

	@BeforeEach
	void criarTabelaEUnidadeDePersistencia() {
		jdbc.execute("create table if not exists teste_campo_cifrado (id uuid primary key, relato text)");
		fabrica = new LocalContainerEntityManagerFactoryBean();
		fabrica.setDataSource(dataSource);
		fabrica.setPersistenceUnitName("teste-criptografia");
		fabrica.setPackagesToScan(RegistroComCampoCifrado.class.getPackageName());
		fabrica.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
		fabrica.setJpaPropertyMap(Map.of("hibernate.resource.beans.container", new SpringBeanContainer(beanFactory),
				"hibernate.hbm2ddl.auto", "validate"));
		fabrica.afterPropertiesSet();
	}

	@AfterEach
	void fecharUnidadeDePersistencia() {
		fabrica.destroy();
	}

	@Test
	void gravaCifradoNoBancoELeClaroNaEntidade() {
		UUID id = UUID.randomUUID();
		String relato = "Relato fictício de teste " + id;
		EntityManagerFactory emf = fabrica.getObject();

		EntityManager gravacao = emf.createEntityManager();
		gravacao.getTransaction().begin();
		gravacao.persist(new RegistroComCampoCifrado(id, relato));
		gravacao.getTransaction().commit();
		gravacao.close();

		String noBanco = jdbc.queryForObject("select relato from teste_campo_cifrado where id = ?", String.class, id);
		assertThat(noBanco).startsWith("v1:" + cifrador.chaveAtiva() + ":").doesNotContain("Relato").doesNotContain(
				id.toString());
		assertThat(cifrador.decifrarTexto(noBanco)).isEqualTo(relato);

		EntityManager leitura = emf.createEntityManager();
		RegistroComCampoCifrado lido = leitura.find(RegistroComCampoCifrado.class, id);
		leitura.close();
		assertThat(lido.getRelato()).isEqualTo(relato);
	}

	@Test
	void nuloFicaNuloNoBanco() {
		UUID id = UUID.randomUUID();
		EntityManager gravacao = fabrica.getObject().createEntityManager();
		gravacao.getTransaction().begin();
		gravacao.persist(new RegistroComCampoCifrado(id, null));
		gravacao.getTransaction().commit();
		gravacao.close();

		assertThat(jdbc.queryForObject("select relato from teste_campo_cifrado where id = ?", String.class, id))
			.isNull();
	}

}
