# ADR 0124 — Correções de acessibilidade achadas pela auditoria

**Status:** Aceito (2026-11-21)

## Contexto

A primeira rodada da auditoria ([ADR 0123](0123-auditoria-de-acessibilidade-no-ci.md)) apontou três
problemas, e o teste de teclado, mais dois:

1. Na barra inferior, o item "Fila" tinha `aria-label="Lista de espera"`: quem usa comando de voz diz
   o que vê ("Fila") e não acha o link (WCAG 2.5.3, regra `label-content-name-mismatch`).
2. O rótulo dos campos preenchidos (`mat-form-field` sem `appearance="outline"`, como o Período do
   log de auditoria) usava o cinza secundário sobre o fundo do campo, com contraste de 4,1:1 (WCAG
   1.4.3 pede 4,5:1).
3. Os botões do Material tiram o outline nativo e mostram o foco só por uma camada translúcida (WCAG
   2.4.7).
4. Um campo perto da base da tela, ao receber foco pelo Tab, ficava atrás da navegação inferior presa:
   o Chrome não rola um elemento que já está dentro da janela, mesmo coberto (WCAG 2.4.11). Sair de
   um campo obrigatório vazio mostra o erro dele e empurra o resto do formulário depois da rolagem.

## Decisão

1. Cada entrada do menu ganha `nomeAcessivel`: o rótulo, ou "rótulo curto, rótulo" quando os dois
   diferem ("Fila, Lista de espera"). O nome contém o texto visível tanto na barra quanto no trilho.
2. `mat.form-field-overrides` com `filled-label-text-color` no azul petróleo do texto
   (`--sgi-color-text-primary`), 6:1 sobre o fundo do campo.
3. `mat.strong-focus-indicators` com borda de 2px em `--sgi-color-primary-dark` em todos os
   controles do Material focados pelo teclado.
4. `scroll-padding-block` no `html` com a altura das barras presas (só a superior a partir do
   tablet), e o `Layout` escuta `focusin` e, no quadro seguinte, chama
   `scrollIntoView({ block: 'nearest' })` no elemento focado, que respeita esse espaço.

## Consequências

- Todas as telas auditadas passam sem violações em celular e desktop.
- O indicador de foco forte muda o visual do foco pelo teclado em todo o sistema; o foco por toque ou
  clique continua sem borda.
- O `scrollIntoView` roda a cada foco dentro do layout; quando o elemento já está visível ele não
  rola nada.
