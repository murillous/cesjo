package br.org.cesjo.sgi;

import java.io.IOException;
import java.io.UncheckedIOException;

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
 * Postgres real para os testes de integração. Com Docker disponível (CI e máquinas com Docker), sobe
 * o {@code postgres:18-alpine} pelo Testcontainers. Sem Docker, sobe um Postgres 18 nativo pelo
 * embedded-postgres, compartilhado por todos os contextos da JVM (ADR 0008).
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	@Conditional(ComDocker.class)
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));
	}

	@Bean
	@Conditional(SemDocker.class)
	JdbcConnectionDetails postgresNativo() {
		EmbeddedPostgres postgres = PostgresNativo.INSTANCIA;
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

	/** Um único Postgres nativo por JVM, parado quando a JVM termina. */
	private static final class PostgresNativo {

		static final EmbeddedPostgres INSTANCIA = iniciar();

		private static EmbeddedPostgres iniciar() {
			try {
				EmbeddedPostgres postgres = EmbeddedPostgres.builder().start();
				Runtime.getRuntime().addShutdownHook(new Thread(() -> {
					try {
						postgres.close();
					}
					catch (IOException ignorado) {
						// A JVM já está terminando.
					}
				}));
				return postgres;
			}
			catch (IOException erro) {
				throw new UncheckedIOException("Não foi possível iniciar o Postgres nativo dos testes", erro);
			}
		}

	}

}
