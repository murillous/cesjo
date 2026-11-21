# ADR 0108 — PIA impresso ou salvo em PDF pelo navegador

**Status:** Aceito (2026-11-21) · Issue #38

## Contexto

O PIA precisa ser imprimível em PDF para ser apresentado à família e à equipe. A base de exportação
em PDF no servidor (Thymeleaf e openhtmltopdf, no módulo `relatorios`) está em outro PR (#80),
ainda não mergeado e fora da pilha desta issue. O conteúdo do PIA já é lido por uma leitura
registrada no log de auditoria.

Alternativas consideradas: esperar o #80 (trava a issue); gerar o PDF no servidor com outra
biblioteca (duplica a base de exportação e cria conflito de dependências).

## Decisão

- A ficha do PIA tem "Imprimir ou salvar em PDF", que abre a impressão do navegador. Todo navegador
  atual oferece "Salvar como PDF" nessa janela.
- A folha de estilo de impressão mostra só o plano: nome da pessoa, versão, status, andamento com
  as datas, diagnóstico, objetivos e observações. A barra superior, a navegação, os botões e os
  avisos de tela ficam fora (`.nao-imprimir` e o bloco `@media print` de `styles.scss`).
- A impressão não gera registro próprio no log: o conteúdo impresso é o que a leitura auditada já
  trouxe para a tela.

## Consequências

- O PDF sai com a aparência do navegador, sem cabeçalho institucional próprio.
- Quando a base de exportação do #80 estiver na `main`, o PIA pode ganhar um PDF gerado no servidor,
  com logo e registro `EXPORTAR`. Essa troca fica para uma issue nova.
