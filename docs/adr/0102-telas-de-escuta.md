# ADR 0102 — Telas de escuta organizadas pela pessoa atendida

**Status:** Aceito (2026-11-21) · Issue #36

## Contexto

Escuta e PIA são da mesma pessoa e a equipe consulta um junto com o outro. A barra inferior do
celular tem cinco lugares (ADR 0035), então dois itens de menu separados empurrariam outros módulos
para "Mais". Ainda não existem os usuários atendidos (marco M3).

## Decisão

- Módulo `modulos/atendimento`, rota `/atendimento`, um item de menu "Escuta e PIA" (curto
  "Escuta"), com `ESCUTA_LER`.
- `/atendimento` lista as pessoas com busca pelo nome que ignora acentos. Por enquanto são os
  candidatos ativos da lista de espera; os usuários atendidos entram nessa lista no M3.
- `/atendimento/pessoas/:pessoaId` é o atendimento da pessoa: a seção de escutas (data e
  profissional, sem os textos) e, com a issue #37, a seção do PIA.
- `/atendimento/pessoas/:pessoaId/escutas/nova` registra; a data começa em hoje e o profissional
  começa no colaborador ligado à conta logada, quando houver.
- `/atendimento/escutas/:id` abre a escuta (leitura auditada), com um aviso de sigilo. Cada texto é
  uma seção com título, para ler no celular como um relatório. "Corrigir escuta" troca a leitura pelo
  formulário.

## Consequências

- Quando o M3 entrar, a ficha do usuário atendido pode apontar para `/atendimento/pessoas/:id`.
