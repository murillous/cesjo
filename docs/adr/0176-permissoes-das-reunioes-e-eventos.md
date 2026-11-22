# ADR 0176 — Permissões das reuniões e eventos institucionais

**Status:** Provisório (PENDENTE-CESJO P17) (2026-11-22) · Issue #52

## Contexto

Reuniões e eventos são gestão institucional (RF012). Na matriz da seção 6 a área é "total" para o
Coordenador, "ler (?)" para a Equipe Técnica, "registrar (?)" para o Administrativo e "não" para o
Consulta. Pelo critério do ADR 0015, seguido nos ADRs 0047 e 0173, os perfis com (?) ficam sem
acesso. Uma permissão por área deixa responder à P17 de forma diferente para cada uma.

## Decisão

Duas permissões novas em `acesso.Perfil`, só para o Coordenador:

| Permissão | Coordenador | Equipe Técnica | Administrativo | Consulta |
|---|---|---|---|---|
| `EVENTOS_LER` (lista e ficha, com a descrição) | sim | não (P17) | não (P17) | não |
| `EVENTOS_REGISTRAR` (registrar e editar) | sim | não | não (P17) | não |

A escolha do parceiro na tela usa a listagem de parceiros, que exige `PARCERIAS_LER`; sem ela, o
campo some e o registro fica sem parceiro.

## Consequências

- Se a equipe confirmar o acesso, basta acrescentar a permissão ao perfil, no ponto marcado com
  `PENDENTE-CESJO(P17)` em `acesso.Perfil`. Quem ganhar `EVENTOS_REGISTRAR` sem `PARCERIAS_LER`
  não consegue ligar parceiro; nesse caso, dê as duas.
