#!/bin/sh
# Entrada do serviço de backup: agenda o backup.sh no crond do BusyBox e fica em primeiro plano.
# O crond não repassa o ambiente do contêiner para as tarefas, então o ambiente é gravado num
# arquivo legível só pelo root e carregado pelo backup.sh.
set -eu

: "${SGI_BACKUP_HORARIO:=0 3 * * *}"

umask 077
export -p > /run/sgi-ambiente
mkdir -p /etc/crontabs
# A saída das tarefas vai para o log do contêiner (docker compose logs backup).
echo "$SGI_BACKUP_HORARIO /usr/local/bin/backup.sh > /proc/1/fd/1 2>&1" > /etc/crontabs/root

echo "backup agendado: '$SGI_BACKUP_HORARIO' (TZ=${TZ:-UTC}), repositório restic configurado"
# -c explícito: a imagem do Postgres não tem o link /var/spool/cron/crontabs do Alpine.
exec crond -f -l 8 -c /etc/crontabs
