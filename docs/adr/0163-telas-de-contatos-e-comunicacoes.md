# ADR 0163 — Telas de contatos e comunicações

**Status:** Aceito (2026-11-22) · Issue #48

## Contexto

A issue #48 pede telas responsivas para registrar e consultar os contatos. As atividades (RF010) têm
telas próprias em PR aberto, no módulo de frontend `atividades`. Juntar as comunicações nessas telas
criaria conflito e misturaria dois registros diferentes numa lista só.

## Decisão

- Módulo de frontend próprio, `modulos/comunicacoes`, no caminho `/comunicacoes`, com o item de menu
  "Contatos e comunicações" ("Contatos" na barra do celular) e o ícone `call`.
- Lista (`/comunicacoes`): do mais recente para o mais antigo, cada linha com o ícone do tipo
  (família, prédio ou atendente), a pessoa ou instituição em destaque, o assunto, o tipo e a data.
  Filtros de tipo e período (seletor de intervalo de datas), com "Limpar filtros". Paginada.
- Ficha (`/comunicacoes/nova` e `/comunicacoes/:id`): o tipo se escolhe em três cartões com ícone,
  feitos com rádios nativos dentro de um `fieldset`, que funcionam com teclado e leitor de tela. O
  rótulo do campo de pessoa ou instituição muda com o tipo ("Cuidador familiar", "Instituição",
  "Pessoa atendida"). A data tem máximo hoje (P30), e a descrição é opcional.
- SCSS mobile first: tudo em uma coluna no celular; no tablet, filtros lado a lado, cartões de tipo
  em três colunas e a data à direita de cada linha.

## Consequências

- Os ofícios, declarações e encaminhamentos (issue #49) entram nessas mesmas telas, com mais tipos e
  os campos de direção e anexo.
