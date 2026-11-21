# ADR 0082 — Permissões dos usuários atendidos

**Status:** Provisório (PENDENTE-CESJO P8, P17) (2026-11-21) · Issue #39

## Contexto

A matriz da seção 6 diz, para "Usuários atendidos e desligamentos (RF007, RF008)": Coordenador
"total", Equipe Técnica "ler e editar", Administrativo e Consulta "dados básicos". O ADR 0015 assumiu
que os dados básicos são só nome, situação, turno e datas (P8). Não está dito se "ler e editar"
inclui inserir no serviço, que é uma decisão sobre vaga.

## Decisão

Três permissões novas em `acesso.Perfil`:

| Permissão | Coordenador | Equipe Técnica | Administrativo | Consulta |
|---|---|---|---|---|
| `ATENDIDOS_INSERIR` | sim | não (P17) | não | não |
| `ATENDIDOS_LER` (ficha completa) | sim | sim | não | não |
| `ATENDIDOS_LER_BASICO` (nome, situação, turno e datas) | sim | sim | sim | sim (P8) |

- "Ler e editar" da Equipe Técnica foi lido como não incluir inserir no serviço, pelo critério
  conservador do ADR 0006, como no cadastro da lista de espera (ADR 0039). A edição do cadastro entra
  com a #40, com permissão própria.
- Os dados básicos (P8) são aplicados na listagem e na ficha (#41).
- O item de menu "Usuários atendidos" aparece para quem tem `ATENDIDOS_LER_BASICO`; a tela de
  inserção exige `ATENDIDOS_INSERIR`.

## Consequências

- Se a Equipe Técnica precisar inserir, basta acrescentar `ATENDIDOS_INSERIR` ao perfil, no ponto
  marcado com `PENDENTE-CESJO(P17)`. Se os dados básicos mudarem, muda a visão básica da #41.
