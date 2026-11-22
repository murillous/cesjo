# ADR 0202 — Conteúdo centralizado ao lado da navegação

**Status:** Aceito (2026-11-22)

## Contexto

No tablet e no desktop, a área de conteúdo (`.conteudo` em `layout/layout.scss`) tem largura máxima
de 1200px, mas fica presa à esquerda, junto do menu lateral. Formulários e fichas também têm largura
máxima própria (de 560px a 1080px) e ficam à esquerda dentro dela. Em telas largas sobra um espaço
vazio grande à direita, e a página parece desalinhada.

Alternativas consideradas:

- **Manter à esquerda**: alinha o conteúdo com o menu, mas deixa o espaço vazio à direita.
- **Centralizar só a área de conteúdo**: só muda alguma coisa em telas acima de cerca de 1500px, e
  formulários e fichas continuam à esquerda.
- **Centralizar a área de conteúdo e o contêiner de cada página**: o conteúdo fica no meio do espaço
  ao lado da navegação em qualquer largura que tenha sobra.

## Decisão

- `.conteudo` recebe `margin-inline: auto` e fica centralizada no espaço ao lado da navegação.
- O contêiner principal de cada página com largura máxima própria também recebe
  `margin-inline: auto`.
- Páginas novas com largura máxima própria seguem a mesma regra. O guia de estilo
  (`docs/frontend/guiaDeEstilo.md`, seção 11) registra isso.
- O texto dentro das páginas continua alinhado à esquerda. Só o bloco é centralizado.

## Consequências

- No celular nada muda: o contêiner já ocupa a largura toda da tela.
- No tablet, formulários mais estreitos que o espaço disponível também ficam centralizados.
- Uma página nova que esquecer o `margin-inline: auto` fica à esquerda, desalinhada das outras.
