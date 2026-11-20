# CESJO SGI

<p align="center">
  <img src="docs/frontend/assets/logo-pobres-servos-cesjo.png" alt="Logo Pobres Servos CESJO" width="500">
</p>

Sistema de gestão de informações do Centro-Dia, que atende pessoas com deficiência (física, sensorial,
intelectual, múltipla, TEA e T21) de 18 a 59 anos.

O projeto recomeçou do zero em 2026-11-20. O MVP anterior (Spring Boot 3.5, SQLite, React 18) está
preservado na tag `mvp-legado`.

## Requisitos

A única fonte de requisitos é o
[Documento de Requisitos do Sistema Centro-Dia](docs/requisitos/documentoRequisitosCentroDia.pdf)
(RF001 a RF017, requisitos não funcionais, perfis de usuário e integrações).

## Stack

| Camada | Escolha |
|---|---|
| Backend | Java 25, Spring Boot 4.2 (Web MVC, Data JPA, Validation, Actuator) |
| Banco | PostgreSQL 18, migrações com Flyway |
| Testes | JUnit, Testcontainers com Postgres real |
| Frontend | Angular 22, PrimeNG 22 com tema institucional |

O planejamento completo (arquitetura, modelo de dados, permissões, ordem de entrega e backlog) está em
[docs/planejamento.md](docs/planejamento.md).

## Rodar o backend

Pré-requisitos: JDK 25 e Docker em execução.

```bash
cd backend
./mvnw spring-boot:run   # sobe o Postgres pelo compose.yaml (Spring Boot Docker Compose)
./mvnw verify            # testes de integração sobem o Postgres via Testcontainers
```
