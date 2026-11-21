#!/bin/sh
# Restauração do CESJO SGI a partir do repositório restic (issue #22, ADR 0120).
#
# Restaura o banco (pg_restore) e o volume de arquivos de um snapshot. Uso, em deploy/, com a
# aplicação parada e o banco no ar:
#
#   docker compose stop app
#   docker compose run --rm --no-deps -v cesjo-arquivos:/restauro/arquivos \
#     --entrypoint restaurar.sh backup [snapshot]
#
# Sem snapshot, usa o mais recente (latest). O volume de arquivos é esvaziado e preenchido com o
# conteúdo do snapshot; as tabelas do dump são recriadas (--clean). Os dados atuais se perdem.
set -eu

SNAPSHOT="${1:-latest}"
DESTINO_ARQUIVOS=/restauro/arquivos
TEMP=/tmp/restauro

registrar() {
	echo "[restauro $(date '+%Y-%m-%dT%H:%M:%S%z')] $*"
}

inicio="$(date +%s)"

if [ ! -d "$DESTINO_ARQUIVOS" ]; then
	registrar "monte o volume de arquivos em $DESTINO_ARQUIVOS (-v cesjo-arquivos:$DESTINO_ARQUIVOS)"
	exit 2
fi

registrar "snapshot $SNAPSHOT"
restic snapshots --host cesjo "$SNAPSHOT"

rm -rf "$TEMP"
mkdir -p "$TEMP"
registrar "restic restore"
restic restore "$SNAPSHOT" --host cesjo --target "$TEMP"

registrar "aguardando o banco"
for _ in $(seq 1 30); do
	pg_isready -h "${PGHOST:-db}" -U "$POSTGRES_USER" -d "$POSTGRES_DB" > /dev/null && break
	sleep 2
done

registrar "pg_restore"
PGPASSWORD="$POSTGRES_PASSWORD" pg_restore -h "${PGHOST:-db}" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
	--clean --if-exists --no-owner --single-transaction --exit-on-error \
	"$TEMP/var/backups/sgi/cesjo.dump"

registrar "arquivos"
find "$DESTINO_ARQUIVOS" -mindepth 1 -delete
cp -a "$TEMP/dados/arquivos/." "$DESTINO_ARQUIVOS/"
# Dono do usuário da aplicação (uid 10001, backend/Dockerfile).
chown -R 10001:10001 "$DESTINO_ARQUIVOS"

rm -rf "$TEMP"
registrar "fim: sucesso em $(( $(date +%s) - inicio )) s"
