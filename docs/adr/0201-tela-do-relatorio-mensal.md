# ADR 0201 — Tela do relatório mensal

**Status:** Aceito (2026-11-22) · Issue #58

## Contexto

O relatório mensal (ADR 0200) precisa de escolha do mês, de uma prévia dos números e do download em
PDF e Excel. O Angular Material não tem seletor só de mês e ano, e o `<input type="month">` não
funciona no Firefox de computador nem é igual entre navegadores.

## Decisão

- Aba "Relatório mensal" em `/relatorios/mensal`, na área Relatórios (ADR 0197).
- Escolha do mês com dois `mat-select`, mês e ano (de 2020 até o ano corrente). Os meses futuros do
  ano corrente ficam desabilitados. A tela abre no mês anterior, que já fechou.
- A prévia mostra o título, o período e uma tabela por grupo, na ordem que vem do servidor. A
  quantidade zero aparece, mais apagada, para mostrar que a área foi contada.
- O botão "Baixar relatório" é o `app-botao-exportar` da base (ADR 0044), com o mês da prévia na
  tela: o arquivo baixado é sempre o mesmo relatório que a pessoa está vendo.

## Consequências

- O quadrimestral e o anual (issue #59) podem reusar o componente trocando a escolha do período.
