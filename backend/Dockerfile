# syntax=docker/dockerfile:1
# Imagem da aplicação Spring Boot para produção (issue #20, ADR 0050).
# Construir a partir de backend/: docker build -t cesjo-sgi-backend backend

# --- Etapa 1: compila o jar (os testes rodam no CI, não aqui) ---
FROM eclipse-temurin:25-jdk AS compilacao
WORKDIR /fonte
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests \
	&& cp target/sgi-backend-*.jar aplicacao.jar \
	&& java -Djarmode=tools -jar aplicacao.jar extract --layers --destination extraido

# --- Etapa 2: só o JRE e as camadas do jar ---
FROM eclipse-temurin:25-jre
# curl só para o healthcheck do Actuator.
RUN apt-get update \
	&& apt-get install -y --no-install-recommends curl \
	&& rm -rf /var/lib/apt/lists/*
# Usuário sem privilégio. O diretório dos arquivos cifrados já nasce com o dono certo, para o volume
# nomeado herdar a permissão (SGI_ARQUIVOS_DIRETORIO, ADR 0026).
RUN groupadd --system --gid 10001 sgi \
	&& useradd --system --uid 10001 --gid sgi --home-dir /app --shell /usr/sbin/nologin sgi \
	&& mkdir -p /var/lib/sgi/arquivos \
	&& chown -R sgi:sgi /var/lib/sgi
WORKDIR /app
# Uma camada por grupo, das que mudam menos para as que mudam mais.
COPY --from=compilacao /fonte/extraido/dependencies/ ./
COPY --from=compilacao /fonte/extraido/spring-boot-loader/ ./
COPY --from=compilacao /fonte/extraido/snapshot-dependencies/ ./
COPY --from=compilacao /fonte/extraido/application/ ./
USER sgi
ENV SPRING_PROFILES_ACTIVE=prod \
	SGI_ARQUIVOS_DIRETORIO=/var/lib/sgi/arquivos \
	JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
	CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "aplicacao.jar"]
