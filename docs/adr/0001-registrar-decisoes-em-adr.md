# ADR 0001 — Registrar decisões em ADR

**Status:** Aceito (2026-11-20)

## Contexto

O projeto recomeçou do zero e várias decisões ainda dependem de respostas da equipe CESJO. As
decisões estavam só na tabela da seção 1 do [planejamento](../planejamento.md), sem o porquê de cada
escolha e sem histórico quando uma delas muda. Com várias frentes de trabalho em paralelo, o risco é
cada uma decidir a mesma coisa de um jeito.

Alternativas consideradas: manter tudo no planejamento; registrar decisões só nas descrições de PR.

## Decisão

Toda decisão do projeto vira um ADR em `docs/adr/`, no formato descrito no [README](README.md), o
mesmo usado no repositório do Bridger. O ADR entra no mesmo PR que implementa a decisão. ADR aceito
não é reescrito: uma mudança vira um ADR novo que o substitui.

## Consequências

- O porquê de cada escolha fica no repositório, ao lado do código.
- Quem revisa um PR confere se a decisão nova tem ADR.
- O planejamento continua como visão geral; quando ele e um ADR divergirem, vale o ADR mais recente.
