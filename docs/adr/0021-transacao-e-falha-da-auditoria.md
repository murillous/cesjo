# ADR 0021 — Transação do registro de auditoria e comportamento em falha

**Status:** Aceito (2026-11-20)

## Contexto

Quem chama `Auditoria.registrar` pode estar numa transação de escrita (criar, alterar, excluir),
numa transação só de leitura (leitura de dado sensível, exportação) ou fora de transação (falha de
login, tarefa agendada). No Postgres, uma transação marcada como só de leitura recusa `INSERT`.

Alternativas consideradas:

- Sempre na transação da operação (`REQUIRED`): o registro some se a operação for desfeita, o que é
  certo, mas falha nas transações só de leitura e não grava nada na falha de login, que costuma
  terminar em exceção.
- Sempre numa transação própria (`REQUIRES_NEW`): grava em qualquer caso, mas deixa no log
  criações e alterações que foram desfeitas depois, e o log passa a mentir.
- Gravação assíncrona ou em fila: não perde desempenho, mas a operação pode acontecer sem registro.

Para a falha da gravação: engolir o erro e só escrever no log da aplicação (a operação segue sem
registro) ou deixar a exceção subir (a operação falha).

## Decisão

- Se há uma transação de escrita ativa, o registro entra nela: só vale se a operação for confirmada.
- Se não há transação, ou se a transação ativa é só de leitura, o registro é gravado numa transação
  própria (`REQUIRES_NEW`) e confirmado na hora.
- Se a gravação falhar, o erro é escrito no log da aplicação e a exceção sobe. Na transação de
  escrita, a operação é desfeita junto; na leitura sensível e na exportação, o dado não é entregue.
  Nada acontece sem registro.

A regra fica em `AuditoriaEmBanco.registrar`.

## Consequências

- O log só tem operações que de fato aconteceram, e toda leitura sensível e exportação entregue
  tem registro.
- Uma operação que falha (desfeita) não deixa registro. Tentativas negadas por permissão também não;
  se a equipe quiser registrar tentativas, entra um ADR novo.
- Banco fora do ar ou tabela com problema derruba as operações auditadas, inclusive o registro de
  falha de login, que então responde erro. É o comportamento mais conservador para dado sensível.
- O registro na transação de escrita é gravado no `flush` da operação, então um erro de gravação
  aparece na confirmação, não na chamada de `registrar`.
