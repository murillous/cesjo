# ADR 0233 — Relatórios e dashboards numa transação só de leitura

**Status:** Aceito (2026-11-23) · Issue #25

## Contexto

Os relatórios estatísticos (ADR 0200, ADR 0203 e ADR 0210) e o dashboard de atendimento (ADR 0198)
somam uma dúzia de contagens, cada uma pela API Java do módulo dono do dado. Cada método dessas APIs
é `@Transactional(readOnly = true)`, então cada contagem pegava e devolvia uma conexão do pool. Com
20 usuários simultâneos e o pool padrão de 10 conexões, o relatório mensal esperava na fila do pool
a cada contagem: sozinho levava cerca de 50 ms, sob carga tinha p50 de cerca de 500 ms (resultado
em `docs/desempenho.md`).

Alternativas consideradas:

- **Aumentar o pool de conexões**: esconde a espera, mas cada conexão é um processo no Postgres e a
  VPS é pequena (ADR 0005).
- **Uma consulta SQL única por relatório**: quebra a regra de que cada módulo conta o próprio dado
  pela API Java (ADR 0195).
- **Uma transação só de leitura por relatório ou painel**: as APIs dos módulos entram na transação
  aberta e todas as contagens usam a mesma conexão.

## Decisão

- `IndicadoresDoRelatorio.calcular`, `IndicadoresDaTransparencia.calcular`,
  `ConteudoDoRelatorioDeIntervencao.calcular` e `ServicoDashboards` ficam
  `@Transactional(readOnly = true)`.
- A exportação continua fora dessa transação: o cálculo termina antes de a base de exportação
  gravar o registro de auditoria.

## Consequências

- Cada relatório ou painel pega uma conexão do pool por requisição. No teste, o p50 do relatório
  mensal sob carga caiu de cerca de 500 ms para cerca de 250 ms.
- A conexão fica presa durante todo o cálculo, que é curto (dezenas de milissegundos).
- Um relatório novo que use a mesma lista de indicadores herda a transação.
