# ADR 0047 — Permissões do cadastro de colaboradores

**Status:** Provisório (PENDENTE-CESJO P17) (2026-11-21) · Issue #50

## Contexto

Colaboradores fazem parte da gestão institucional (RF014). Na matriz da seção 6 essa área é "total"
para o Coordenador, "ler (?)" para a Equipe Técnica, "registrar (?)" para o Administrativo e "não"
para o Consulta. O ADR 0015 já supôs, pelo critério conservador, que os perfis com (?) ficam sem
acesso à gestão institucional.

Visitas, escutas e atividades precisam mostrar a lista de colaboradores ativos para a Equipe Técnica
e o Administrativo escolherem o profissional. Isso não exige ver o cadastro.

## Decisão

Duas permissões novas em `acesso.Perfil`:

| Permissão | Coordenador | Equipe Técnica | Administrativo | Consulta |
|---|---|---|---|---|
| `COLABORADORES_LER` (listagem e ficha) | sim | não (P17) | não (P17) | não |
| `COLABORADORES_GERENCIAR` (cadastrar, editar, ativar e ligar conta) | sim | não | não (P17) | não |

- A escolha do profissional nas outras telas não usa estas permissões: cada módulo expõe os
  colaboradores ativos pela API Java `Colaboradores` (ADR 0045), sob a permissão da própria tela
  (por exemplo, agendar visita).

## Consequências

- Se a equipe CESJO confirmar que a Equipe Técnica lê a gestão institucional, basta acrescentar
  `COLABORADORES_LER` ao perfil, no ponto marcado com `PENDENTE-CESJO(P17)`. O mesmo vale para o
  Administrativo e `COLABORADORES_GERENCIAR`.
