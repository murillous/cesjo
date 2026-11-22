# ADR 0177 — Telas de reuniões e eventos

**Status:** Aceito (2026-11-22) · Issue #52

## Contexto

O registro do RF012 (ADR 0175) precisa de listagem e formulário. Os sete tipos têm nomes longos, e
só visitas e eventos têm indicação de realizado, recebido ou participado.

## Decisão

- Módulo novo `eventos` no frontend, com item de menu "Reuniões e eventos" (curto "Eventos", ícone
  `event`) para quem tem `EVENTOS_LER`.
- `/eventos`: lista do mais recente ao mais antigo. Cada registro mostra data, tipo, a indicação
  (quando houver), o começo da descrição (duas linhas) e o parceiro. Filtros de tipo e período num
  formulário com "Filtrar" e "Limpar", empilhados no celular e numa linha a partir do tablet.
- `/eventos/novo` (só com `EVENTOS_REGISTRAR`) e `/eventos/:id`: formulário com tipo, data,
  descrição, participantes e parceiro. A indicação aparece como botões de opção só quando o tipo é
  visita ou evento, com rótulos próprios ("Recebida no Centro-Dia", "Centro-Dia participou"); trocar
  para um tipo sem indicação limpa a escolha. Sem permissão de registrar, a ficha só mostra os dados.

## Consequências

- O formulário não deixa enviar combinação que o backend recusaria.
- A lista não tem exportação; ela entra com os relatórios do RF016, se a equipe pedir.
