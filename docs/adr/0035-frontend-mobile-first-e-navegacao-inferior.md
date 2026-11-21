# ADR 0035 — Frontend mobile first com navegação inferior no celular

**Status:** Aceito (2026-11-21)

## Contexto

A equipe técnica do Centro-Dia usa o sistema no celular durante o trabalho de campo: visitas
domiciliares (#34, #35), escuta qualificada (#36) e contatos telefônicos (#48). O computador fica
para relatórios, dashboards e tarefas administrativas (#55 a #61). O esqueleto do frontend (#11)
partia do desktop. O menu lateral usava `MatSidenav` e, abaixo de 992px, virava uma gaveta aberta
por um botão de hambúrguer no canto superior, longe do polegar. O `layout.scss` também usava media
query `max-width`.

Alternativas consideradas para a navegação no celular:

- **Manter o hambúrguer com gaveta**: não exige mudança, mas esconde os módulos e deixa o botão fora
  do alcance do polegar.
- **Barra de navegação inferior**: módulos sempre visíveis e no alcance do polegar. Cabem só cerca de
  cinco destinos.
- **Abas no topo**: mesma limitação de quantidade e longe do polegar.

Alternativas para os módulos que não cabem na barra: rolagem horizontal na barra (esconde itens sem
aviso) ou um item "Mais" que abre a lista restante.

## Decisão

- Os estilos do frontend são **mobile first**. A base vale para o celular, e telas maiores entram
  com media queries `min-width` nos breakpoints de `styles/_breakpoints.scss` (768px, 992px, 1440px).
  Media query `max-width` não é usada.
- A navegação principal muda de formato só com CSS, sem `BreakpointObserver`:
  - **celular**: barra inferior. Com até cinco módulos visíveis para a conta, todos ficam na barra.
    Acima disso, os quatro primeiros ficam na barra e o resto vai para um item "Mais", que abre um
    `MatBottomSheet` (`layout/menu-mais/`). O "Mais" fica marcado quando a página atual é de um
    módulo que está dentro dele;
  - **tablet (768px ou mais)**: trilho lateral com todos os módulos, ícone sobre o rótulo;
  - **desktop (992px ou mais)**: menu lateral com ícone e rótulo na mesma linha.
- O `MatSidenav` e o botão de hambúrguer saem do layout. O layout passa a ser um grid CSS com barra
  superior e barra inferior presas por `position: sticky`, respeitando `env(safe-area-inset-*)`.
- `ItemMenu` ganha `rotuloCurto`, opcional, usado na barra inferior, onde cada item tem um quinto da
  largura. O rótulo completo continua sendo o nome acessível do link (`aria-label`).
- O item ativo é marcado por um disco na cor `accent` atrás do ícone, tirado do selo circular da
  marca, e pelo rótulo em peso 600. O mesmo disco destaca o dia na página inicial e não é usado em
  outros lugares.
- Os módulos que montam a navegação vêm do token `MODULOS_FRONTEND`, cujo padrão é a lista `MODULOS`
  de `modulos/registro.ts`. Os testes trocam a lista sem mexer no registro.
- Alvos de toque têm no mínimo 48px (`--sgi-alvo-toque`).

## Consequências

- No celular, os módulos ficam visíveis e no alcance do polegar. A troca de formato não depende de
  JavaScript.
- Módulo com nome longo precisa de `rotuloCurto`. Sem ele, o rótulo é cortado com reticências na
  barra inferior.
- A ordem dos módulos (`ItemMenu.ordem`) passa a decidir quais ficam na barra e quais vão para o
  "Mais".
- Componentes e páginas novos seguem a mesma regra: base para o celular e `min-width` para crescer.
  O guia de estilo (`docs/frontend/guiaDeEstilo.md`, seções 11 e 14) registra isso.
