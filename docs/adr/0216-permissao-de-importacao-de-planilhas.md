# ADR 0216 — Permissão de importação de planilhas só para o Coordenador

**Status:** Provisório (PENDENTE-CESJO P17) (2026-11-23) · Issue #33

## Contexto

A matriz da seção 6 diz, para "Importação de planilhas": Coordenador sim, Equipe Técnica não,
Administrativo "sim (?)" e Consulta não. A pergunta P17 não foi respondida, e o ADR 0015 decidiu que
o perfil em dúvida fica sem acesso.

Alternativas consideradas:

- **Uma permissão por importação** (lista de espera, usuários atendidos): permite separar depois, mas
  a matriz trata a importação como uma área só.
- **Uma permissão para toda importação**: segue a matriz. Se uma importação precisar de regra própria,
  ganha permissão própria.

## Decisão

- Permissão nova `PLANILHAS_IMPORTAR`: baixar o modelo, pré-visualizar e confirmar importações.

| Permissão | Coordenador | Equipe Técnica | Administrativo | Consulta |
|---|---|---|---|---|
| `PLANILHAS_IMPORTAR` | sim | não | não (P17) | não |

- O marcador `PENDENTE-CESJO(P17)` fica na linha do Administrativo em `acesso.Perfil`.
- A importação não exige as permissões de cadastro da área: quem importa já tem a permissão mais
  forte, e a rota de importação é separada.
- No frontend, o botão "Importar planilha" da fila e a rota `/lista-espera/importar` só aparecem para
  quem tem a permissão.

## Consequências

- Se a equipe responder que o Administrativo importa, basta somar `PLANILHAS_IMPORTAR` ao perfil em
  `acesso.Perfil`.
