# ADR 0219 — Tela de importação de planilha reutilizável

**Status:** Aceito (2026-11-23) · Issue #33

## Contexto

A importação da lista de espera (#33) e a dos usuários atendidos (#62) têm o mesmo fluxo: baixar o
modelo, enviar a planilha, conferir os erros por linha e confirmar. A planilha tem até 1000 linhas e
sete ou mais colunas, o que não cabe numa tabela no celular.

Alternativas consideradas:

- **Uma tela por importação**: duplica o fluxo e as mensagens.
- **Componente compartilhado parametrizado pela URL base do backend**: cada importação tem só uma
  página pequena com o título e o texto de introdução.
- **Tabela larga com rolagem horizontal na pré-visualização**: esconde os erros no celular.

## Decisão

- Componente `app-importacao-planilha` em `compartilhado/importacao`, com `ImportacaoApi`. Recebe a
  URL base (`/api/importacao/lista-espera`) e os nomes do registro no singular e no plural. As colunas
  vêm do backend (`GET .../colunas`), então o modelo muda num lugar só (ADR 0215).
- Três passos numerados, porque são uma sequência: baixar o modelo (com "O que preencher em cada
  coluna" num `details`), enviar a planilha e conferir e confirmar.
- A pré-visualização é uma lista, uma linha da planilha por bloco, com o número da linha no Excel, o
  nome, a situação ("Pronta" ou "Com erro"), os erros em lista e os demais valores em uma, duas ou três
  colunas conforme a largura. A linha com erro tem a borda lateral na cor de erro. Uma caixa "Mostrar
  só as linhas com erro" filtra a lista.
- O botão de confirmação diz quantas linhas grava ("Importar 12 linhas") e fica desabilitado sem linha
  válida. O resultado diz quantos registros entraram e quantas linhas ficaram de fora, e mostra o link
  projetado pela página (na lista de espera, "Ver a fila").
- Rota `/lista-espera/importar`, com `PLANILHAS_IMPORTAR` (ADR 0216), e botão "Importar planilha" na
  fila.

## Consequências

- A importação dos usuários atendidos (#62) é uma página nova com outra URL base.
- O arquivo fica no navegador entre a pré-visualização e a confirmação; trocar de página descarta a
  importação, sem nada gravado.
