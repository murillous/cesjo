# ADR 0198 — Dashboard de violações, atividades e desligamentos por período

**Status:** Provisório (PENDENTE-CESJO P32) (2026-11-22) · Issue #57

## Contexto

A issue #57 (RF017) pede violações por categoria, atividades realizadas por período e taxa de
desligamento por motivo, com filtro de período. O módulo Estatísticas lê os outros módulos só pelas
APIs Java públicas (ADR 0195). O módulo Atividades já expõe `Atividades.contarPorTipo(inicio, fim)`
(ADR 0151). O módulo Atendidos não tinha contagens por período.

O documento de requisitos não define a taxa de desligamento. Alternativas consideradas para a base
da taxa:

- **Sobre o total de desligamentos do período**: vira só a distribuição dos motivos, que o número
  absoluto já mostra. Não diz se o serviço perde muita ou pouca gente.
- **Sobre os ativos no fim do período**: quem saiu no período já não está ativo, então a base
  encolhe justamente quando há muitos desligamentos.
- **Sobre os usuários atendidos em algum dia do período**: inseridos até o fim e não desligados antes
  do início. É a forma usual de taxa de saída num período e não depende do dia em que se olha.

## Decisão

- `UsuariosAtendidos` ganha três contagens por período, feitas em SQL agregado em
  `atendidos.interno.ContagensDoPeriodo`: `contarViolacoesPorTipo` (pela data de identificação),
  `contarDesligamentosPorMotivo` (pela data do desligamento) e `contarAtendidosNoPeriodo`. Toda
  categoria e todo motivo aparecem, com zero quando não houve.
- `GET /api/estatisticas/dashboards/atendimento?inicio=AAAA-MM-DD&fim=AAAA-MM-DD`, com as duas datas
  inclusive e obrigatórias. Fim antes do início responde 422. O período é o record `Periodo`, que o
  relatório mensal (issue #58) também usa.
- A taxa de desligamento, geral e por motivo, é desligamentos do período sobre os usuários atendidos
  no período, de 0 a 1. O marcador `PENDENTE-CESJO(P32)` fica em `ServicoDashboards.taxa`.
- Os rótulos dos tipos de atividade ficam em `estatisticas.interno.RotulosAtividade`, iguais aos do
  frontend, porque o enum do módulo Atividades não tem rótulo.
- Tela: aba "Violações, atividades e desligamentos" em `/relatorios/atendimento`, com um seletor de
  período que abre no ano corrente (1º de janeiro até hoje). Três números no topo (atividades,
  atendidos no período, desligamentos com a taxa) e três gráficos de barras (ADR 0197). No gráfico
  de desligamentos, a porcentagem de cada barra é a taxa do motivo, e a nota abaixo do título diz
  isso.
- Como no dashboard de perfil, não há cache e abrir o dashboard não grava na auditoria (ADR 0195).

## Consequências

- Se a equipe definir outra base para a taxa, a mudança fica em `ServicoDashboards.taxa` e, se for
  outra contagem, num método novo de `UsuariosAtendidos`.
- "Atividades realizadas por período" é a contagem por tipo no período escolhido. Se a equipe quiser
  a evolução mês a mês, entra um gráfico novo que chama `contarPorTipo` uma vez por mês.
