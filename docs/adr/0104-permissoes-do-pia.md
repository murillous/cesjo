# ADR 0104 — Permissões de leitura e edição do PIA

**Status:** Aceito (2026-11-21) · Issue #37

## Contexto

Na matriz da seção 6, "Escuta e PIA" é "total e aprovar PIA" para o Coordenador, "elaborar e
editar" para a Equipe Técnica e "não" para Administrativo e Consulta.

## Decisão

Duas permissões novas em `acesso.Perfil`:

| Permissão | Coordenador | Equipe Técnica | Administrativo | Consulta |
|---|---|---|---|---|
| `PIA_LER` (versões e leitura auditada) | sim | sim | não | não |
| `PIA_EDITAR` (elaborar e editar a versão em elaboração) | sim | sim | não | não |

- A aprovação, só do Coordenador, entra com a issue #38 (`PIA_APROVAR`).
- `GET /api/atendimento/pessoas` passa a aceitar `ESCUTA_LER` ou `PIA_LER`.

## Consequências

- Hoje os mesmos perfis têm escuta e PIA. Se um dia forem separados, a tela da pessoa já mostra só a
  seção que a conta pode ver.
