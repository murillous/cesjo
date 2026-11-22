# ADR 0161 — API Java de contagem de comunicações

**Status:** Aceito (2026-11-22) · Issue #48

## Contexto

O relatório mensal (issue #58) consolida quantitativos de vários módulos. Ele precisa saber quantos
contatos e comunicações de cada tipo houve no mês, sem ler a tabela `comunicacao`, que é interna do
módulo `atividades`. As atividades do RF010 já seguem esse modelo (`atividades.Atividades`, ADR 0151,
em PR aberto).

Alternativas consideradas: o relatório chamar a API HTTP (mais lento e passa pela permissão de quem
pede); um método genérico com filtros abertos (sem uso ainda).

## Decisão

Interface pública `atividades.Comunicacoes`, separada de `Atividades` para não conflitar com o código
em PR:

- `Map<TipoComunicacao, Long> contarPorTipo(LocalDate inicio, LocalDate fim)`: datas inclusive, todo
  tipo no mapa com zero quando não houve registro, `IllegalArgumentException` quando o fim é anterior
  ao início.
- Não checa perfil (a permissão é de quem chama) e não audita, porque só conta.
- A contagem é uma consulta agregada (`group by tipo`) com índice em `(data_comunicacao, tipo)`.

## Consequências

- Com os ofícios, declarações e encaminhamentos (issue #49), a interface ganhou
  `contarPorDirecao(tipo, inicio, fim)`, que conta os enviados e os recebidos (ADR 0164).
- Se o relatório pedir outros cortes (por instituição, por exemplo), entra um método novo aqui.
