# ADR 0196 — Permissão dos relatórios e dashboards

**Status:** Provisório (PENDENTE-CESJO P17) (2026-11-22) · Issues #56, #57 e #58

## Contexto

A matriz da seção 6 diz, para "Relatórios e dashboards (RF016, RF017)": Coordenador total, Equipe
Técnica "ler (?)", Administrativo "não (?)" e Consulta "ler". A pergunta P17 ainda não foi
respondida, e o ADR 0015 decidiu que o perfil em dúvida fica sem acesso.

Alternativas consideradas:

- **Uma permissão por painel**: permite dar o perfil dos atendidos a um perfil e as violações a
  outro, mas a matriz trata relatórios e dashboards como uma área só.
- **Uma permissão para a área toda**: segue a matriz, e um painel com regra própria ganha permissão
  própria quando a equipe pedir.

## Decisão

- Permissão nova `RELATORIOS_LER`: ver os dashboards e gerar os relatórios estatísticos.

| Permissão | Coordenador | Equipe Técnica | Administrativo | Consulta |
|---|---|---|---|---|
| `RELATORIOS_LER` | sim | não (P17) | não (P17) | sim |

- Os marcadores `PENDENTE-CESJO(P17)` ficam nas linhas da Equipe Técnica e do Administrativo em
  `acesso.Perfil`, e no `relatorios.modulo.ts` do frontend.
- No frontend, a área "Relatórios" (`/relatorios`) só aparece no menu e só abre para quem tem a
  permissão.

## Consequências

- Se a equipe responder que a Equipe Técnica lê relatórios, basta somar `RELATORIOS_LER` ao perfil
  em `acesso.Perfil`.
- O Consulta vê contagens por tipo de deficiência e gênero, que não vê na lista de espera (ADR 0058),
  porque o painel não identifica ninguém.
