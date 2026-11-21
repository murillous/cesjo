# ADR 0107 — Aprovação do PIA só pelo Coordenador

**Status:** Aceito (2026-11-21) · Issue #38

## Contexto

A seção 4.1 do documento de requisitos e a matriz da seção 6 dão ao Coordenador "total e aprovar
PIA" e à Equipe Técnica "elaborar e editar".

## Decisão

Uma permissão nova em `acesso.Perfil`:

| Permissão | Coordenador | Equipe Técnica | Administrativo | Consulta |
|---|---|---|---|---|
| `PIA_APROVAR` | sim | não | não | não |

- Registrar as apresentações à família e à equipe e criar nova versão usam `PIA_EDITAR` (ADR
  0104), que a Equipe Técnica tem: fazem parte de elaborar o plano.
- A aprovação guarda a conta que aprovou (`aprovado_por`).

## Consequências

- A Equipe Técnica vê "Aguardando a aprovação da coordenação" no PIA apresentado à equipe.
