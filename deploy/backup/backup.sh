#!/bin/sh
# Backup diário do CESJO SGI (issue #21, ADR 0054 e ADR 0065).
#
# 1. Dump do Postgres (formato custom do pg_dump).
# 2. restic backup do dump e do volume de arquivos (já cifrados pela aplicação). O restic cifra tudo
#    com AES-256 antes de enviar ao repositório, que fica fora da VPS.
# 3. Retenção: restic forget --prune com os prazos do backup.env.
# 4. restic check confere a integridade do repositório.
# 5. Avisa o resultado: GET em SGI_BACKUP_URL_SUCESSO ou em SGI_BACKUP_URL_FALHA.
#
# Rodar à mão: docker compose exec backup backup.sh
set -eu

# Pelo crond, o ambiente do contêiner vem do arquivo gravado pelo entrada.sh. Pelo
# docker compose exec, o ambiente já está presente e prevalece.
if [ -z "${RESTIC_REPOSITORY:-}" ] && [ -f /run/sgi-ambiente ]; then
	# shellcheck disable=SC1091
	. /run/sgi-ambiente
fi

# PENDENTE-CESJO(P14): prazo de guarda dos backups ainda não definido; padrão de 7 diários,
# 4 semanais e 6 mensais, ajustável no backup.env (ADR 0065)
: "${SGI_BACKUP_MANTER_DIARIOS:=7}"
: "${SGI_BACKUP_MANTER_SEMANAIS:=4}"
: "${SGI_BACKUP_MANTER_MENSAIS:=6}"

DUMP_DIR=/var/backups/sgi
ARQUIVOS_DIR=/dados/arquivos

registrar() {
	echo "[backup $(date '+%Y-%m-%dT%H:%M:%S%z')] $*"
}

avisar() {
	# $1: URL (pode estar vazia); $2: mensagem enviada no corpo.
	[ -n "${1:-}" ] || return 0
	curl -fsS -m 15 --retry 3 -o /dev/null --data-raw "$2" "$1" \
		|| registrar "aviso: não foi possível chamar a URL de alerta"
}

executar() {
	registrar "início"
	mkdir -p "$DUMP_DIR"
	rm -f "$DUMP_DIR"/cesjo.dump

	# Código 10 do restic: o repositório não existe. Só nesse caso ele é criado; qualquer outro erro
	# (senha errada, destino fora do ar) interrompe o backup.
	codigo=0
	restic cat config > /dev/null || codigo=$?
	if [ "$codigo" -eq 10 ]; then
		registrar "repositório restic ainda não existe; criando"
		restic init
	elif [ "$codigo" -ne 0 ]; then
		registrar "repositório restic inacessível (código $codigo)"
		return "$codigo"
	fi

	registrar "pg_dump"
	PGPASSWORD="$POSTGRES_PASSWORD" pg_dump -h "${PGHOST:-db}" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
		--format=custom --no-owner --file="$DUMP_DIR/cesjo.dump"

	registrar "restic backup"
	restic backup --host cesjo --tag diario "$DUMP_DIR" "$ARQUIVOS_DIR"

	registrar "restic forget --prune (diários $SGI_BACKUP_MANTER_DIARIOS, semanais $SGI_BACKUP_MANTER_SEMANAIS, mensais $SGI_BACKUP_MANTER_MENSAIS)"
	restic forget --host cesjo --tag diario --prune \
		--keep-daily "$SGI_BACKUP_MANTER_DIARIOS" \
		--keep-weekly "$SGI_BACKUP_MANTER_SEMANAIS" \
		--keep-monthly "$SGI_BACKUP_MANTER_MENSAIS"

	registrar "restic check"
	restic check

	# O dump em claro não fica na VPS depois de enviado.
	rm -f "$DUMP_DIR"/cesjo.dump
	registrar "fim: sucesso"
}

# Um backup por vez: se o anterior ainda está rodando, este sai com erro.
exec 9> /run/sgi-backup.lock
if ! flock -n 9; then
	registrar "outro backup em andamento"
	avisar "${SGI_BACKUP_URL_FALHA:-}" "backup do CESJO SGI não rodou: outro backup em andamento"
	exit 1
fi

# Subshell fora de condição: assim o set -e vale dentro de executar (num "if", o sh o ignora).
set +e
(set -e; executar)
status=$?
set -e

if [ "$status" -eq 0 ]; then
	avisar "${SGI_BACKUP_URL_SUCESSO:-}" "backup do CESJO SGI concluído"
else
	rm -f "$DUMP_DIR"/cesjo.dump
	registrar "fim: FALHA (código $status)"
	avisar "${SGI_BACKUP_URL_FALHA:-}" "backup do CESJO SGI falhou (código $status); veja docker compose logs backup"
	exit "$status"
fi
