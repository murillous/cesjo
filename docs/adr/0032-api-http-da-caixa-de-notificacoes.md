# ADR 0032 — API HTTP da caixa de notificações da conta logada

**Status:** Aceito (2026-11-20)

## Contexto

O frontend precisa listar as notificações da conta logada, mostrar a contagem de não lidas e marcar
como lida uma ou todas. Todos os perfis têm `NOTIFICACOES_LER` (seção 6 do planejamento). Uma conta
nunca pode ver nem marcar notificação de outra conta ou de outro perfil.

Alternativas consideradas para o acesso indevido: responder 403 quando a notificação existe mas não é
da conta, ou 404 como se não existisse. Para marcar como lida: `PATCH` com corpo `{ "lida": true }`, ou
um recurso de leitura criado com `POST`.

## Decisão

Endpoints em `/api/notificacoes`, todos com `@PreAuthorize("hasAuthority('NOTIFICACOES_LER')")` e
sempre sobre a conta de `ContaAtual`, nunca sobre uma conta vinda da requisição:

- `GET /api/notificacoes?naoLidas=false&page=0&size=20`: página das notificações visíveis, da mais nova
  para a mais antiga (ordenação pedida pelo cliente é ignorada). Cada item traz `id`, `tipo`,
  `mensagem`, `referencia`, `criadaEm`, `lidaEm` e `lida`. Com `naoLidas=true`, só as não lidas.
- `GET /api/notificacoes/nao-lidas/contagem`: `{ "quantidade": n }`.
- `POST /api/notificacoes/{id}/leitura`: marca uma como lida, 204. Marcar de novo também responde 204.
  Notificação inexistente, de outra conta ou de outro perfil responde 404, sem revelar que existe.
- `POST /api/notificacoes/leitura`: marca todas as visíveis como lidas, `{ "marcadas": n }`.

As requisições que alteram dados exigem o token CSRF, como o resto da API.

## Consequências

- O filtro de visibilidade fica nas consultas do repositório, num único trecho, e os testes de
  integração provam o isolamento entre contas e perfis.
- 404 em vez de 403 evita que alguém descubra identificadores de notificações alheias.
- Marcar como lida é idempotente; cliques repetidos ou duas abas não geram erro nem auditoria duplicada.
