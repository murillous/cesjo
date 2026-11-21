# ADR 0008 — Postgres nativo nos testes quando não há Docker

**Status:** Aceito (2026-11-20)

## Contexto

Os testes de integração usam Postgres real pelo Testcontainers ([ADR 0002](0002-monolito-modular-spring-angular.md)).
Algumas máquinas de desenvolvimento não rodam Docker: na máquina do mantenedor, o Docker Desktop
não sobe porque a "Plataforma de Máquina Virtual" do Windows está desligada. Sem Postgres, o
`./mvnw verify` falha localmente e o único retorno vem do CI.

Alternativas consideradas: H2 em modo compatível com Postgres (não é o mesmo banco, esconde erro de
SQL e de migração); rodar os testes só no CI; exigir Docker de todo mundo.

## Decisão

`TestcontainersConfiguration` decide na hora: com Docker disponível, sobe o `postgres:18-alpine` pelo
Testcontainers; sem Docker, sobe um Postgres 18 nativo pelo `io.zonky.test:embedded-postgres`, com
os binários da versão definida em `embedded-postgres-binarios.version` no `pom.xml`. O Postgres
nativo é um só por JVM, compartilhado entre os contextos de teste.

## Consequências

- `./mvnw verify` roda em qualquer máquina com JDK 25; o CI continua com Testcontainers.
- O Postgres nativo é compartilhado: cada teste cria os dados de que precisa e não conta com tabela
  vazia (use identificadores únicos ou limpe o que criou).
- A versão dos binários nativos acompanha a imagem do Docker; ao subir uma, sobe a outra.
