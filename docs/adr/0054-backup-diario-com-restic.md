# ADR 0054 — Backup diário com restic para um repositório fora da VPS

**Status:** Provisório (PENDENTE-CESJO P15) (2026-11-21) · Detalha o [ADR 0005](0005-hospedagem-vps-docker-compose.md)

## Contexto

O RNF 3.3 e o ADR 0005 pedem backup diário do banco e do volume de arquivos, cifrado e enviado para
fora da VPS, com retenção e alerta de falha (issue #21). Ainda não se sabe o provedor da VPS nem onde
o backup vai ficar (pergunta 15). Os arquivos do volume já estão cifrados pela aplicação (ADR 0026),
mas o dump do banco tem em claro os campos que não são texto livre sensível (nome, datas, contatos).

Alternativas consideradas: `pg_dump` mais `gpg` e `rclone` em script próprio (cifra, envio,
retenção e verificação feitos à mão); `borg` (só fala SSH com o servidor remoto); backup gerenciado
do provedor da VPS (depende do provedor, que é desconhecido, e copia o disco inteiro).

## Decisão

- Um serviço `backup` no compose, com imagem própria (`deploy/backup/Dockerfile`) sobre
  `postgres:18-alpine`, para o `pg_dump` ter a mesma versão do servidor, mais `restic`, `curl` e
  `tzdata`. O `crond` do BusyBox roda o `backup.sh` no horário de `SGI_BACKUP_HORARIO` (padrão
  `0 3 * * *`, fuso `TZ`, padrão `America/Sao_Paulo`).
- O `backup.sh` faz, nesta ordem: `pg_dump --format=custom` pela rede interna; `restic backup` do
  dump e do volume `cesjo-arquivos` (montado só leitura); `restic forget --prune` com a retenção do
  [ADR 0065](0065-retencao-dos-backups.md); `restic check`. O dump em claro é apagado da VPS depois do
  envio, com sucesso ou com falha. Um `flock` impede dois backups ao mesmo tempo.
- O restic cifra tudo com AES-256 e autentica com Poly1305 antes de enviar. A senha do repositório
  (`RESTIC_PASSWORD`) é um segredo novo, guardado fora da VPS junto com as chaves da aplicação.
- PENDENTE-CESJO(P15): o destino é qualquer repositório aceito pelo restic (S3 e compatíveis, B2,
  SFTP, Azure, GCS, rclone), escolhido por `RESTIC_REPOSITORY` e credenciais em `deploy/backup.env`.
- O repositório só é criado (`restic init`) quando o restic responde que ele não existe (código 10).
  Senha errada ou destino fora do ar interrompem o backup com erro, sem criar um repositório novo.
- Os segredos do backup ficam em `deploy/backup.env`, separado do `.env` do
  [ADR 0052](0052-segredos-da-implantacao-no-env.md), para a aplicação não recebê-los. O `backup.env`
  está no `.gitignore`.
- **Alerta:** ao terminar, o script chama `SGI_BACKUP_URL_SUCESSO` ou `SGI_BACKUP_URL_FALHA`
  (POST com uma frase no corpo). Isso serve para qualquer serviço de "ping" de tarefa agendada. Num
  serviço com prazo (Healthchecks.io, Cronitor), a falta do ping de sucesso também gera alerta, o que
  cobre o backup que nem chegou a rodar. Sem URL, o resultado fica só no log do contêiner.
- Rodar à mão: `docker compose exec backup backup.sh`.

## Consequências

- Backup incremental com deduplicação: só o que mudou vai para o destino a cada dia, e o volume de
  arquivos não é reenviado inteiro.
- Restaurar exige o restic, a `RESTIC_PASSWORD`, as credenciais do destino e as chaves de
  criptografia da aplicação. O procedimento completo de recuperação de desastres é a issue #22.
- O contêiner de backup roda como root, porque o `crond` precisa e para ler os arquivos do volume.
  Ele só tem o volume de arquivos em somente leitura e a senha do banco.
- O backup é lógico (`pg_dump`), não contínuo: pode-se perder até um dia de dados. Se a equipe
  precisar de menos perda, troca-se `SGI_BACKUP_HORARIO` ou passa-se a arquivar o WAL.
- Quando a pergunta 15 for respondida, um ADR novo fixa o destino.
