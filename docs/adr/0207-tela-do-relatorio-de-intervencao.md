# ADR 0207 — Tela do relatório de intervenção

**Status:** Aceito (2026-11-23) · Issue #60

## Contexto

O relatório de intervenção (ADR 0205) tem um período livre, uma lista de casos com nome e um resumo.
Cada geração é auditada como leitura sensível (ADR 0206), e a área de relatórios mostra as mesmas abas
para todos que têm `RELATORIOS_LER` (ADR 0197).

Alternativas consideradas para abrir a aba:

- **Gerar ao abrir, como o mensal**: a pessoa vê os números logo, mas cada clique na aba deixa um
  evento de leitura sensível no log, mesmo sem querer o relatório.
- **Preencher o período e gerar só quando a pessoa pedir**: um clique a mais, e o log só registra
  quem quis ver.

Alternativas para mostrar os casos:

- **Tabela larga**: igual ao arquivo, mas no celular precisa de rolagem lateral.
- **Um bloco por caso**: nome, violações e intervenções empilhados no celular e em duas colunas no
  desktop.

## Decisão

- Aba "Relatório de intervenção" em `/relatorios/intervencao`. A aba e a rota só existem para quem
  também tem `VIOLACOES_LER`: `abas.ts` ganhou o campo opcional `permissao`, e a página de relatórios
  esconde a aba de quem não a tem.
- O período vem preenchido com o mês anterior, num `mat-date-range-input` que não aceita datas
  futuras. O relatório só é gerado com "Gerar relatório".
- Um aviso curto, sempre visível, diz que o relatório mostra nomes e que gerar e baixar ficam na
  auditoria.
- Cada caso é um bloco com o nome, as violações (categoria, âmbito e data) e três dados: atendimentos
  no período, último atendimento e situação no fim do período. Sem caso no período, a tela diz isso.
- O resumo usa tabelas por grupo, como no mensal (ADR 0201).
- "Baixar relatório" é o `app-botao-exportar` com o período da prévia.

## Consequências

- Outra aba com permissão própria é só uma linha com `permissao` em `abas.ts` e um `canMatch` na
  rota.
