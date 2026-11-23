# ADR 0221 — Tela de importação dos usuários atendidos

**Status:** Aceito (2026-11-23) · Issue #62

## Contexto

A tela de importação da lista de espera já é um componente reutilizável (ADR 0219). A importação dos
usuários atendidos precisa do mesmo fluxo, a partir da área de usuários atendidos.

## Decisão

- Página `/atendidos/importar` (`importar-atendidos.ts`), com `PLANILHAS_IMPORTAR`, que usa
  `app-importacao-planilha` com a URL base `/api/importacao/atendidos`. As colunas e as instruções vêm do
  backend.
- O texto de introdução diz que cada linha vira usuário ativo, com o responsável como cuidador, e que
  quem ainda espera vaga entra pela importação da lista de espera.
- Botão "Importar planilha" no cabeçalho da lista de usuários atendidos, só para quem tem a permissão.
  Depois da importação, o link "Ver os usuários atendidos" volta para a lista.

## Consequências

- Nenhum componente novo: uma mudança no fluxo vale para as duas importações.
