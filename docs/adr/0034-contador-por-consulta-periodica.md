# ADR 0034 — Contador de notificações por consulta periódica de 60 segundos

**Status:** Aceito (2026-11-20)

## Contexto

O sino da barra superior mostra quantas notificações estão sem leitura. A contagem precisa se
atualizar sem recarregar a página. Alternativas consideradas:

- WebSocket ou Server-Sent Events: atualização imediata, mas exige conexão aberta por aba, cuidado
  com a sessão e com o proxy (Caddy), e mais código no backend, para avisos que não são urgentes.
- Consulta periódica (polling) do endpoint de contagem: simples, usa a sessão e a API que já existem.
  O custo é o atraso até o próximo ciclo e uma requisição leve por aba a cada intervalo.

## Decisão

- O frontend consulta `GET /api/notificacoes/nao-lidas/contagem` ao aparecer o sino e depois a cada
  60 segundos. O valor fica no token `INTERVALO_CONSULTA_NOTIFICACOES`, em
  `frontend/src/app/notificacoes/sino-notificacoes.ts`.
- A consulta pausa enquanto a aba está escondida (`document.visibilityState`) e roda na hora em que ela
  volta a ficar visível. Abrir o painel também recarrega a contagem.
- Falha na consulta mantém o último valor, sem mensagem; a próxima tentativa vem no ciclo seguinte.
- O sino só existe para conta autenticada com `NOTIFICACOES_LER`; sem sessão não há consulta.

## Consequências

- Um aviso de vaga aberta pode levar até um minuto para aparecer, o que basta para o uso do Centro-Dia.
- Com poucas dezenas de contas, a carga é desprezível (uma contagem indexada por minuto por aba).
- Se surgir aviso que precise ser imediato, este ADR é substituído por um com SSE.
