# ADR 0203 — Relatórios quadrimestral e anual: mesmo conteúdo, novos recortes do período

**Status:** Provisório (PENDENTE-CESJO P9) (2026-11-23) · Issue #59

## Contexto

A issue #59 (RF016) pede o mesmo conteúdo do relatório mensal (ADR 0200) agregado por quadrimestre e
por ano, com exportação em PDF e Excel. A pergunta P9 (modelos atuais dos relatórios) segue sem
resposta, então não se sabe se a equipe usa o quadrimestre do ano civil ou outro calendário (por
exemplo, o do convênio com o município).

Alternativas consideradas:

- **Um endpoint genérico com início e fim livres**: flexível, mas a pessoa teria de saber as datas
  de cada quadrimestre, e o título do relatório não diria o recorte.
- **Somar os relatórios mensais**: errado para os indicadores de "último dia" (fila e ativos) e para
  "usuários atendidos no período", que não se somam.
- **Novas fábricas de `PeriodoDoRelatorio` e novos endpoints**: o ADR 0200 já deixou o período
  parametrizável; cada indicador conta direto no período inteiro.

## Decisão

- `PeriodoDoRelatorio.quadrimestral(Year, int)` e `PeriodoDoRelatorio.anual(Year)` montam o período
  e o título; `IndicadoresDoRelatorio` não muda.
- Quadrimestres do ano civil: 1 é janeiro a abril, 2 é maio a agosto, 3 é setembro a dezembro. O
  marcador `PENDENTE-CESJO(P9)` fica em `PeriodoDoRelatorio.quadrimestral`.
- Títulos: "Relatório quadrimestral de maio a agosto de 2026" e "Relatório anual de 2026". O nome do
  arquivo baixado sai do título (ADR 0042).
- Endpoints, com `RELATORIOS_LER`, sem cache, como o mensal:
  `GET /api/estatisticas/relatorios/quadrimestral?ano=AAAA&quadrimestre=1..3`,
  `GET /api/estatisticas/relatorios/anual?ano=AAAA` e as exportações em `.../exportacao` com
  `formato=pdf|xlsx`. A exportação registra `EXPORTAR` em `relatorio_estatistico` na auditoria
  (ADR 0043); ver a prévia não é auditado.
- O período corrente sai parcial; um período que começa depois de hoje responde 422, assim como
  quadrimestre fora de 1 a 3. Ano ou quadrimestre que não são números respondem 400.

## Consequências

- Se a equipe usar outro calendário, a mudança fica em `PeriodoDoRelatorio.quadrimestral` (e no
  rótulo dos quadrimestres na tela, ADR 0204).
- O relatório anual de um ano com muitos registros faz as mesmas consultas agregadas do mensal num
  intervalo maior; o teste de desempenho (issue #25) cobre esse caso.
