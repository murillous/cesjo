# ADR 0074 — Permissões para registrar a realização e ler as observações da visita

**Status:** Provisório (PENDENTE-CESJO P8) (2026-11-21) · Issue #35

## Contexto

Na matriz da seção 6, as visitas são "total" para Coordenador e Equipe Técnica e "agendar e
alterar" para o Administrativo. Não está claro se "alterar" inclui registrar como foi a visita, com
observações sobre a família. A pergunta 8 trata justamente do acesso do Administrativo a dados
sensíveis.

## Decisão

Uma permissão nova em `acesso.Perfil`:

| Permissão | Coordenador | Equipe Técnica | Administrativo | Consulta |
|---|---|---|---|---|
| `VISITAS_REGISTRAR` (realizar e ler observações) | sim | sim | não (P8) | não |

- Cancelar e reagendar contam como "alterar" a agenda e ficam com `VISITAS_AGENDAR` (ADR 0071),
  que o Administrativo tem.
- O Administrativo vê que a visita foi realizada e se há observações, mas não o texto, pelo critério
  conservador do ADR 0006.

## Consequências

- Se a equipe CESJO liberar, basta acrescentar `VISITAS_REGISTRAR` ao Administrativo, no ponto
  marcado com `PENDENTE-CESJO(P8)` em `acesso.Perfil`.
