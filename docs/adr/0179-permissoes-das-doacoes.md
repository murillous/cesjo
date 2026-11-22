# ADR 0179 — Permissões das doações

**Status:** Provisório (PENDENTE-CESJO P17) (2026-11-22) · Issue #54

## Contexto

Doações são gestão institucional (RF015). Na matriz da seção 6 a área é "total" para o Coordenador,
"ler (?)" para a Equipe Técnica, "registrar (?)" para o Administrativo e "não" para o Consulta. Pelo
critério do ADR 0015, seguido nos ADRs 0047, 0173 e 0176, os perfis com (?) ficam sem acesso. Das
quatro áreas, esta é a que o Administrativo mais provavelmente vai registrar; uma permissão própria
permite liberar só ela.

## Decisão

Duas permissões novas em `acesso.Perfil`, só para o Coordenador:

| Permissão | Coordenador | Equipe Técnica | Administrativo | Consulta |
|---|---|---|---|---|
| `DOACOES_LER` (lista e ficha) | sim | não (P17) | não (P17) | não |
| `DOACOES_REGISTRAR` (registrar e editar) | sim | não | não (P17) | não |

## Consequências

- Se a equipe confirmar o acesso, basta acrescentar a permissão ao perfil, no ponto marcado com
  `PENDENTE-CESJO(P17)` em `acesso.Perfil`.
