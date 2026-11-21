# ADR 0071 — Permissões da agenda e do agendamento de visitas

**Status:** Aceito (2026-11-21) · Issue #34

## Contexto

Na matriz da seção 6, as visitas domiciliares (RF004) são "total" para Coordenador e Equipe Técnica,
"agendar e alterar" para o Administrativo e "não" para o Consulta. Nenhum ponto tem (?).

## Decisão

Duas permissões novas em `acesso.Perfil`:

| Permissão | Coordenador | Equipe Técnica | Administrativo | Consulta |
|---|---|---|---|---|
| `VISITAS_LER` (agenda e ficha da visita) | sim | sim | sim | não |
| `VISITAS_AGENDAR` (agendar, listas de candidatos e profissionais) | sim | sim | sim | não |

- O Administrativo lê a agenda, porque agendar e alterar sem ver a agenda não faz sentido.
- A escolha do profissional usa `GET /api/visitas/profissionais`, sob `VISITAS_AGENDAR`. Assim a
  Equipe Técnica e o Administrativo escolhem colaboradores sem acesso ao cadastro deles (ADR 0047).
- O registro da realização e do cancelamento entra com a issue #35, com permissões próprias.

## Consequências

- O Consulta não vê nada de visitas, nem na página inicial.
