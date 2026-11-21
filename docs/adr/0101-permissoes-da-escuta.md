# ADR 0101 — Permissões da escuta ativa e qualificada

**Status:** Aceito (2026-11-21) · Issue #36

## Contexto

Na matriz da seção 6, "Escuta e PIA" é "total e aprovar PIA" para o Coordenador, "elaborar e
editar" para a Equipe Técnica e "não" para o Administrativo e o Consulta, sem ponto com (?). O
critério de aceite da issue #36 repete que Administrativo e Consulta não têm acesso.

## Decisão

Duas permissões novas em `acesso.Perfil`:

| Permissão | Coordenador | Equipe Técnica | Administrativo | Consulta |
|---|---|---|---|---|
| `ESCUTA_LER` (lista e leitura auditada) | sim | sim | não | não |
| `ESCUTA_REGISTRAR` (registrar e corrigir) | sim | sim | não | não |

- `GET /api/atendimento/pessoas`, que alimenta a escolha da pessoa nas telas de escuta e PIA, também
  exige `ESCUTA_LER`.

## Consequências

- O Administrativo agenda visitas (ADR 0071), mas não vê nada da escuta, nem que ela existe.
