# ADR 0180 — Telas de doações

**Status:** Aceito (2026-11-22) · Issue #54

## Contexto

O registro do RF015 (ADR 0178) precisa de listagem por período e de formulário. Valor e quantidade
dependem do tipo, e quem digita valor em reais usa vírgula.

## Decisão

- Módulo novo `doacoes` no frontend, com item de menu "Doações" (ícone `volunteer_activism`) para
  quem tem `DOACOES_LER`.
- `/doacoes`: lista da mais recente à mais antiga, com filtros de período (de, até) e direção num
  formulário com "Filtrar" e "Limpar", empilhados no celular e numa linha a partir do tablet. Cada
  doação mostra data, direção (por texto e fundo, não só por cor), contraparte, tipo e o valor ou a
  quantidade.
- `/doacoes/nova` (só com `DOACOES_REGISTRAR`) e `/doacoes/:id`: a direção é um par de botões de
  opção; o rótulo da contraparte muda para "Quem doou" ou "Quem recebeu". Para dinheiro aparece o
  campo "Valor" com prefixo R$, teclado numérico (`inputmode="decimal"`) e vírgula ou ponto; para os
  outros tipos aparece "Quantidade". Só o campo visível é enviado.

## Consequências

- O formulário não deixa enviar combinação que o backend recusaria.
- Totais por período e exportação ficam para os relatórios do RF016.
