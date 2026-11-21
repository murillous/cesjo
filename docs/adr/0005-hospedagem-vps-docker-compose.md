# ADR 0005 — Hospedagem numa VPS com Docker Compose

**Status:** Provisório (PENDENTE-CESJO P15) (2026-11-20) · Registra a linha "Hospedagem" da seção 1 do [planejamento](../planejamento.md)

## Contexto

O volume é pequeno e o orçamento também. Ainda não se sabe quem paga e administra a VPS nem se já
existe domínio (pergunta 15 do planejamento).

Alternativas consideradas: PaaS gerenciado; Kubernetes.

## Decisão

Uma VPS com Docker Compose: Caddy (TLS automático, arquivos estáticos do Angular e proxy de `/api`),
a aplicação Spring Boot e o Postgres. Backup diário cifrado enviado para fora da VPS.

## Consequências

- Custo baixo e implantação simples, com um único `docker compose up`.
- Backup, monitoramento e recuperação de desastres são responsabilidade do projeto (épico #2).
- Quando a equipe responder a pergunta 15, um ADR novo confirma ou troca o provedor.
