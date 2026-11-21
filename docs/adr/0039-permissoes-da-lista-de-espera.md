# ADR 0039 — Permissões do cadastro e da leitura da lista de espera

**Status:** Provisório (PENDENTE-CESJO P17) (2026-11-21) · Issue #26

## Contexto

A matriz da seção 6 diz, para a lista de espera: Coordenador "total", Equipe Técnica "ler e editar",
Administrativo "total" e Consulta "dados básicos". Não diz se "ler e editar" inclui cadastrar
candidato novo.

## Decisão

Duas permissões novas em `acesso.Perfil`:

| Permissão | Coordenador | Equipe Técnica | Administrativo | Consulta |
|---|---|---|---|---|
| `LISTA_ESPERA_LER` (ficha completa) | sim | sim | sim | não |
| `LISTA_ESPERA_CADASTRAR` | sim | não (P17) | sim | não |

- "Ler e editar" foi lido como não incluir cadastrar, pelo critério conservador do ADR 0006. A
  edição entra com a issue #29, com permissão própria.
- O Consulta não lê a ficha completa. A visão de "dados básicos" (P8) entra com a listagem (#28).

## Consequências

- Se a Equipe Técnica precisar cadastrar, basta acrescentar `LISTA_ESPERA_CADASTRAR` ao perfil, no
  ponto marcado com `PENDENTE-CESJO(P17)`.
