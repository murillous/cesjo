# ADR 0173 — Permissões das parcerias

**Status:** Provisório (PENDENTE-CESJO P17) (2026-11-22) · Issue #53

## Contexto

Parcerias fazem parte da gestão institucional (RF012 a RF015). Na matriz da seção 6 essa área é
"total" para o Coordenador, "ler (?)" para a Equipe Técnica, "registrar (?)" para o Administrativo
e "não" para o Consulta. O ADR 0015 supôs, pelo critério conservador, que os perfis com (?) ficam
sem acesso, e o ADR 0047 seguiu a mesma linha para os colaboradores.

Alternativa: uma permissão única para toda a gestão institucional. Ela impede responder à P17 de
forma diferente por área (por exemplo, o Administrativo registrar doações mas não parcerias).

## Decisão

Duas permissões novas em `acesso.Perfil`, só para o Coordenador:

| Permissão | Coordenador | Equipe Técnica | Administrativo | Consulta |
|---|---|---|---|---|
| `PARCERIAS_LER` (lista, ficha e ações) | sim | não (P17) | não (P17) | não |
| `PARCERIAS_GERENCIAR` (cadastrar, editar e registrar ações) | sim | não | não (P17) | não |

## Consequências

- Se a equipe confirmar o acesso, basta acrescentar a permissão ao perfil, no ponto marcado com
  `PENDENTE-CESJO(P17)` em `acesso.Perfil`.
