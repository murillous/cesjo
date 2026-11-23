# ADR 0235 — Índices das consultas lentas do teste de desempenho

**Status:** Aceito (2026-11-23) · Issue #25 · Substitui a proposta do ADR 0234

## Contexto

O teste de desempenho (ADR 0231) mostrou o log de auditoria como gargalo, e o ADR 0234 propôs um
índice para o filtro por login. Para saber se outras tabelas também precisavam de índice, o teste
rodou com `auto_explain` (planos de toda consulta acima de 15 ms) na massa de 10 vezes o volume
esperado. As consultas com plano ruim foram:

- **Auditoria por login**: a contagem da paginação lia a tabela (Bitmap Heap Scan), cerca de 340 ms.
- **Auditoria por entidade, ação e período**: o índice `auditoria_entidade_idx (entidade,
  ocorrido_em)` achava 100 mil linhas da entidade e a tabela descartava 67 mil de outras ações,
  cerca de 120 ms.
- **Inserção vigente de uma pessoa** (relatório de intervenção e lista de violações): não havia
  índice em `insercao.pessoa_id`, só o parcial da inserção ativa. Cada violação lia a tabela inteira
  de inserções (Seq Scan, 601 vezes), cerca de 110 ms.

As outras consultas acima de 15 ms já usam índice e gastam o tempo lendo as linhas do período
(contagens de atividades e comunicações em 5 anos, de 30 a 60 ms), ou fazem uma busca por inscrição
no histórico da lista de espera (cerca de 60 ms para 6.000 inscrições). Índices novos nelas ganhariam
pouco e encareceriam cada gravação.

Alternativas consideradas:

- **Só o índice do ADR 0234**: resolve o pior caso, mas deixa os outros dois planos ruins.
- **Índice em toda chave estrangeira**: a maioria não é usada em consulta, e cada índice deixa as
  gravações mais caras.
- **Só os índices que o plano mostrou que faltam**.

## Decisão

Migração `V60__indices_das_consultas.sql`:

- `auditoria_login_cobertura_idx (lower(conta_login), ocorrido_em DESC, id DESC) INCLUDE
  (conta_login)` substitui `auditoria_login_idx`, como propôs o ADR 0234.
- `auditoria_entidade_acao_idx (entidade, acao, ocorrido_em DESC, id DESC)` substitui
  `auditoria_entidade_idx`. O filtro só por entidade continua usando a primeira coluna.
- `ix_insercao_pessoa (pessoa_id, data_insercao DESC)`, novo.

Os índices são criados sem `CONCURRENTLY`, dentro da transação do Flyway. No volume real, o log terá
no máximo algumas centenas de milhares de linhas, e o bloqueio de escrita dura poucos segundos na
subida da versão.

## Consequências

- Os tempos com os índices estão em `docs/desempenho.md`.
- Cada gravação no log de auditoria mantém o mesmo número de índices. A troca não acrescenta custo.
- Se o log crescer muito além da massa testada, a contagem sem filtro (cerca de 200 ms em 2 milhões
  de linhas) continua a crescer. A saída segue a do ADR 0234: não contar o total exato nessa
  listagem.
