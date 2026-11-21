# ADR 0106 — Status do PIA em ordem fixa, com data, e mudança por nova versão

**Status:** Provisório (PENDENTE-CESJO P5) (2026-11-21) · Issue #38

## Contexto

O RF006 lista os status do PIA: elaborado, apresentado à família, apresentado à equipe e aprovado.
O planejamento (item 29) pede a data de cada passo, aprovação só pelo Coordenador e que o PIA
aprovado não seja mais editável, com a mudança gerando nova versão. A pergunta 5 não diz se a
ordem dos passos é sempre a mesma nem se o PIA é revisado periodicamente.

Alternativas consideradas: passos em qualquer ordem (deixa aprovar um plano que a família não viu);
editar o PIA apresentado (a família e a equipe aprovariam um texto que depois muda); voltar o status
para elaborado (perde o registro do que foi apresentado).

## Decisão

- Migração `V32`: `apresentado_familia_em`, `apresentado_equipe_em` e `aprovado_em` (datas) e
  `aprovado_por` (conta). `CHECK`s garantem a data de cada status já alcançado.
- Provisório (pergunta 5): os passos seguem sempre a ordem do RF006, um de cada vez:
  `POST /api/pias/{id}/apresentar-a-familia`, `.../apresentar-a-equipe` e `.../aprovar`, cada um
  com `data`. A data não pode ser futura nem anterior à do passo de antes.
- Só a versão mais nova muda de status. Só a versão em elaboração aceita edição; depois de
  apresentado, o PIA não muda mais.
- `POST /api/pias/{id}/nova-versao` cria, a partir da versão mais nova já apresentada ou aprovada,
  uma versão nova em elaboração com o mesmo conteúdo. A versão de origem fica como estava.
- A resposta da versão traz `maisNova` e as três datas.
- Auditoria: alteração com `status` e a coluna da data; na aprovação, também `aprovado_por`.
  Criação na nova versão.

## Consequências

- O histórico do PIA de uma pessoa é a lista de versões, cada uma com as datas em que foi
  apresentada e aprovada.
- Se a equipe quiser pular passos (por exemplo aprovar sem apresentar à equipe), a regra muda em
  `Pia.avancar`.
