# ADR 0120 — Restauração por script na imagem de backup, numa pilha com só o banco no ar

**Status:** Aceito (2026-11-21)

## Contexto

A issue #22 pede um procedimento para restaurar o sistema numa VPS nova e uma restauração executada
com sucesso, com o tempo registrado (RNF 3.4). O backup ([ADR 0054](0054-backup-diario-com-restic.md))
guarda num repositório restic o dump do Postgres e o volume de arquivos cifrados. Restaurar à mão,
comando por comando, é fácil de errar no meio de um incidente.

Alternativas consideradas: só comandos no guia (o que havia até aqui); restaurar o disco inteiro por
imagem do provedor (provedor desconhecido, pergunta 15); restaurar com a aplicação no ar (o Flyway e
a criação da primeira conta disputam o banco com o `pg_restore`).

## Decisão

- O script `deploy/backup/restaurar.sh`, na mesma imagem do backup, restaura um snapshot (padrão
  `latest`): `restic restore` numa pasta temporária, `pg_restore --clean --if-exists --no-owner
  --single-transaction --exit-on-error` no banco e cópia do volume de arquivos, que é esvaziado
  antes e devolvido ao usuário da aplicação (uid 10001).
- Ele roda num contêiner avulso com o volume de arquivos montado para escrita em
  `/restauro/arquivos`: `docker compose run --rm --no-deps -v cesjo-arquivos:/restauro/arquivos
  --entrypoint restaurar.sh backup [snapshot]`. O serviço `backup` continua com o volume só leitura.
- A ordem numa VPS nova é: subir só o `db`, rodar o `restaurar.sh`, subir o resto. A aplicação só
  sobe depois que o banco já tem o histórico do Flyway e as contas restauradas.
- O procedimento completo fica em `docs/recuperacao-de-desastres.md`.

## Consequências

- Uma restauração é três comandos, os mesmos que o CI executa
  ([ADR 0122](0122-teste-de-restauracao-no-ci-e-exercicio.md)).
- O `--single-transaction` desfaz o banco inteiro se algo falhar no meio, sem deixar meio restaurado.
- A restauração apaga os dados atuais do banco e do volume. Para recuperar só um registro antigo,
  restaura-se num banco à parte (o guia descreve como).
