# ADR 0002 — Monólito modular com Spring Boot, PostgreSQL e Angular

**Status:** Aceito (2026-11-20) · Registra as decisões da seção 1 do [planejamento](../planejamento.md)

## Contexto

O sistema atende uma só organização, com centenas de pessoas atendidas e dezenas de contas de
acesso. Roda numa VPS pequena e é mantido por poucas pessoas. O MVP anterior (Spring Boot 3.5,
SQLite, React 18) ficou na tag `mvp-legado` e não é reaproveitado.

Alternativas consideradas: microsserviços; SQLite; React; PrimeNG no lugar do Angular Material.

## Decisão

- Backend Java 25 com Spring Boot 4.2 (Web MVC, Data JPA, Security, Validation), monólito modular
  verificado pelo Spring Modulith. Pacote raiz `br.org.cesjo.sgi`, um pacote por módulo.
- PostgreSQL 18, esquema só por migrações Flyway, Hibernate em `validate`.
- Testes com JUnit e Testcontainers com Postgres real.
- Frontend Angular 22 com Angular Material 22 e tema Material 3 a partir do guia de estilo. O
  PrimeNG ficou de fora porque passou a exigir chave de licença a partir da versão 22.
- Monorepo com `backend/` e `frontend/`.

## Consequências

- Um artefato só para implantar e monitorar.
- O Spring Modulith falha o teste quando um módulo usa algo interno de outro.
- Separar um módulo em serviço próprio no futuro exige trocar chamadas diretas por eventos ou HTTP.
