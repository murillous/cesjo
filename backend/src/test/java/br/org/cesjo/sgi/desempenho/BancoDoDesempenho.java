package br.org.cesjo.sgi.desempenho;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Banco só do teste de desempenho (ADR 0231): um Postgres 18 novo, separado do Postgres compartilhado
 * pelos outros testes, para a massa simulada não se misturar com os dados deles. Com Docker, um
 * contêiner {@code postgres:18-alpine}; sem Docker, uma instância nativa do embedded-postgres (como no
 * ADR 0008), parada quando o contexto fecha. O teste da carga de demonstração (ADR 0237) usa o mesmo
 * banco isolado.
 */
@TestConfiguration(proxyBeanMethods = false)
public class BancoDoDesempenho {

	@Bean
	@ServiceConnection
	@Conditional(ComDocker.class)
	PostgreSQLContainer postgresDoDesempenho() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));
	}

	@Bean(destroyMethod = "close")
	@Conditional(SemDocker.class)
	EmbeddedPostgres postgresNativoDoDesempenho() throws java.io.IOException {
		return EmbeddedPostgres.builder().start();
	}

	@Bean
	@Conditional(SemDocker.class)
	JdbcConnectionDetails conexaoDoDesempenho(EmbeddedPostgres postgres) {
		String url = postgres.getJdbcUrl("postgres", "postgres");
		return new JdbcConnectionDetails() {

			@Override
			public String getUsername() {
				return "postgres";
			}

			@Override
			public String getPassword() {
				return "postgres";
			}

			@Override
			public String getJdbcUrl() {
				return url;
			}

		};
	}

	static final class ComDocker implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return DockerClientFactory.instance().isDockerAvailable();
		}

	}

	static final class SemDocker implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return !DockerClientFactory.instance().isDockerAvailable();
		}

	}

}
