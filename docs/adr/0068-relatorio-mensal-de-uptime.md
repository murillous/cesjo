# ADR 0068 — Relatório mensal de uptime a partir das execuções do monitor

**Status:** Aceito (2026-11-21)

## Contexto

A issue #23 pede um relatório mensal de uptime para acompanhar a meta de 99% do RNF 3.4. O monitor
externo ([ADR 0067](0067-monitor-externo-no-github-actions.md)) já deixa um histórico: uma execução
por verificação, verde ou vermelha, guardada pelo GitHub por 90 dias.

Alternativas consideradas: o relatório do serviço dedicado (só existe se houver um); gravar cada
verificação num arquivo do repositório (um commit a cada 5 minutos).

## Decisão

- O workflow `.github/workflows/relatorio-uptime.yml` roda no dia 1 de cada mês, às 12h UTC, e pode
  ser disparado à mão com o mês (`AAAA-MM`).
- Ele lista as execuções agendadas do `monitoramento.yml` no mês, dia a dia (a busca por data do
  GitHub devolve no máximo 1000 execuções e um dia tem até 288). Execução verde conta como amostra no
  ar, vermelha como fora do ar. Execuções puladas, canceladas ou em andamento não entram.
- Uptime = verdes / (verdes + vermelhas). O relatório mostra o uptime contra a meta de 99%, o número
  de verificações, o tempo fora do ar estimado (5 minutos por verificação vermelha) e a cobertura
  (verificações feitas sobre as esperadas).
- O resultado vai para o resumo da execução e para um artefato `uptime-AAAA-MM`, guardado por 90
  dias.

## Consequências

- O relatório sai sozinho todo mês, sem serviço externo.
- É uma estimativa por amostragem: verificações que o GitHub não fez não contam, e a cobertura mostra
  quanto isso pesou.
- Depois de 90 dias o GitHub apaga as execuções e o artefato. Quem acompanha a meta copia o resumo
  mensal para um lugar da coordenação.
