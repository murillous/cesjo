# ADR 0199 — Gráfico de violações só para quem lê violações de direitos

**Status:** Provisório (PENDENTE-CESJO P33) (2026-11-22) · Issue #57

## Contexto

A issue #57 pede o dashboard "visível só para perfis autorizados". A matriz da seção 6 dá
relatórios e dashboards ao Coordenador e ao Consulta (ADR 0196), mas deixa as violações de direitos
(RF009) só com Coordenador e Equipe Técnica. Violação é o dado mais sensível do sistema. Numa
instituição com dezenas de usuários, uma contagem pequena por categoria num mês pode bastar para
alguém de fora da equipe técnica deduzir de quem se trata.

Alternativas consideradas:

- **Mostrar tudo a quem tem `RELATORIOS_LER`**: segue a linha "relatórios" da matriz ao pé da letra,
  mas abre ao Consulta um recorte de dado que a linha "violações" nega a ele.
- **Esconder o dashboard inteiro de quem não lê violações**: tira do Consulta também atividades e
  desligamentos, que ele pode ver.
- **Esconder só o gráfico de violações**: cada parte segue a linha da matriz do seu dado.

## Decisão

- O backend só manda `violacoesPorCategoria` para quem tem `RELATORIOS_LER` e `VIOLACOES_LER`. Para
  os outros, o campo vem `null`, e a tela não mostra o gráfico. Hoje só o Coordenador tem as duas.
- O marcador `PENDENTE-CESJO(P33)` fica em `DashboardsControlador.atendimento`.

## Consequências

- Se a equipe liberar as violações agregadas ao Consulta, basta tirar a condição do controlador.
- O relatório mensal (issue #58) segue a mesma regra para a linha de violações.
