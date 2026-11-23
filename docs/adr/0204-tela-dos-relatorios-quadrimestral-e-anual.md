# ADR 0204 — Tela dos relatórios quadrimestral e anual

**Status:** Aceito (2026-11-23) · Issue #59

## Contexto

Os relatórios quadrimestral e anual (ADR 0203) têm o mesmo conteúdo do mensal e precisam de uma
prévia e do download. A tela do mensal (ADR 0201) já resolve a prévia, mas tudo estava num
componente só, junto com a escolha do mês.

Alternativas consideradas:

- **Duas abas novas, uma por recorte**: a barra de abas da área Relatórios já tem quatro itens e
  fica longa no celular.
- **Pôr quadrimestre e ano dentro da aba do mensal**: mistura três escolhas diferentes numa tela
  que hoje é simples.
- **Uma aba "Quadrimestral e anual" e um componente de prévia compartilhado**.

## Decisão

- A prévia (título, período, botão "Baixar relatório" e tabelas por grupo) vira o componente
  `app-quadro-relatorio`, usado pelo mensal e pela tela nova. Quem usa passa o endpoint e os
  parâmetros da exportação, os mesmos da prévia.
- A escolha do período e o aviso de erro dividem o estilo `escolha-relatorio.scss`, mobile first.
- Aba "Quadrimestral e anual" em `/relatorios/quadrimestral-e-anual`: escolha do relatório
  (quadrimestral ou anual), do quadrimestre (só no quadrimestral, com os meses no rótulo) e do ano.
  Quadrimestres futuros do ano corrente ficam desabilitados. A tela abre no quadrimestre anterior,
  que já fechou.

## Consequências

- Um recorte novo é uma tela de escolha que reusa `app-quadro-relatorio`.
- Mudanças na prévia valem para os três relatórios de uma vez.
