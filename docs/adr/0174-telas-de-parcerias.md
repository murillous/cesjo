# ADR 0174 — Telas de parcerias

**Status:** Aceito (2026-11-22) · Issue #53

## Contexto

O cadastro de parceiros (ADR 0172) precisa de listagem, cadastro e ficha, e as ações de articulação
precisam ser vistas e registradas. O módulo de colaboradores (ADR 0048 e ADR 0171) já tem o mesmo
formato de lista e ficha.

## Decisão

- Módulo novo `parcerias` no frontend, com item de menu "Parcerias" (ícone `handshake`) para quem
  tem `PARCERIAS_LER`.
- `/parcerias`: lista paginada em ordem de nome, com filtro Ativos, Inativos e Todos, mostrando tipo
  e vigência. No celular cada parceiro é um bloco; a partir do tablet vira tabela de quatro colunas.
- `/parcerias/novo` (só com `PARCERIAS_GERENCIAR`) e `/parcerias/:id`: formulário com os campos do
  ADR 0172; a vigência é um grupo com início e fim, empilhados no celular. Sem permissão de gerenciar,
  a ficha só mostra os dados.
- Na ficha, a seção "Ações de articulação" lista as ações como linha do tempo e tem o botão
  "Registrar ação", que abre um formulário curto (data e o que foi feito) na própria seção.

## Consequências

- Segue o mesmo desenho das telas de colaboradores, então não há padrão novo para aprender.
