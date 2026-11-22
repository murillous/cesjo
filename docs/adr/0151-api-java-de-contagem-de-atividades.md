# ADR 0151 — API Java do módulo Atividades para contagem por tipo e período

**Status:** Aceito (2026-11-22) · Issue #45

## Contexto

O dashboard de atividades (issue #57) mostra as atividades realizadas por período, e o relatório
mensal (issue #58) consolida quantitativos do mês. A seção 3 do planejamento prevê que os relatórios
leiam por consultas SQL próprias do módulo `relatorios`; os módulos `atendidos` e `listaespera` já
expõem contagens por API Java (ADR 0081, ADR 0093).

Alternativas consideradas: o módulo `relatorios` consultar a tabela `atividade` direto (acopla o
relatório ao esquema de outro módulo); um endpoint REST de contagem (o relatório roda no mesmo
processo e não precisa de HTTP).

## Decisão

- Interface pública `atividades.Atividades`, implementada por `ServicoAtividades`:
  `Map<TipoAtividade, Long> contarPorTipo(LocalDate inicio, LocalDate fim)`, com as duas datas
  inclusive. Todo tipo aparece no mapa, com zero quando não houve atividade. `fim` antes de `inicio`
  lança `IllegalArgumentException`.
- A contagem é uma consulta agregada (`group by tipo`) sobre o índice `ix_atividade_data_tipo`.
- A interface não checa perfil (a permissão é de quem chama) e não audita, porque só devolve números.

## Consequências

- O relatório mensal chama `contarPorTipo` com o primeiro e o último dia do mês; o quadrimestral e o
  anual, com os seus limites.
- Contagem por mês dentro de um período, por profissional ou por participante entra como método novo
  na mesma interface quando um relatório pedir.
