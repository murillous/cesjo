# ADR 0036 — Fonte Atkinson Hyperlegible Next enquanto não há fonte institucional

**Status:** Provisório (PENDENTE-CESJO P16) (2026-11-21)

## Contexto

O guia de estilo pede que a fonte oficial seja confirmada com o material de identidade visual. A
pergunta 16 do planejamento ("existe fonte institucional oficial?") ainda não tem resposta. O
esqueleto do frontend usava Inter, uma das sugestões genéricas do guia.

Alternativas consideradas:

- **Inter, Roboto, Open Sans ou Montserrat**: as sugestões do guia. São boas fontes de interface, mas
  não dizem nada sobre o público do sistema.
- **Atkinson Hyperlegible Next**: desenhada pelo Braille Institute para leitura com baixa visão.
  Distingue caracteres parecidos (`0` e `O`; `1`, `l` e `I`) e tem licença SIL Open Font License.
  Combina com um centro de referência da pessoa com deficiência e ajuda quem lê números de documentos
  (CPF, NIS) na tela do celular.

## Decisão

- A aplicação usa **Atkinson Hyperlegible Next** em todos os textos, numa família só, pelo pacote
  `@fontsource-variable/atkinson-hyperlegible-next`. O pacote `@fontsource-variable/inter` sai.
- A fonte é definida num lugar só: `$font-family` em `frontend/src/styles/_tokens.scss`, com o
  marcador `PENDENTE-CESJO(P16)`. O tema do Angular Material lê o mesmo valor.
- A escala de tipografia parte do celular: 13, 15, 17, 20, 24 e 30px (legenda, small, body, h3, h2 e
  h1). A partir de 992px, só o h1 cresce, para 36px.

## Consequências

- Se a CESJO informar uma fonte institucional, a troca é feita no pacote do `angular.json` e em
  `$font-family`, e entra um ADR que substitui este.
- O texto padrão no celular tem 17px, um pouco maior que o comum, para leitura em campo.
- O zero da Atkinson tem um traço que o diferencia da letra O. Isso aparece em datas e números.
