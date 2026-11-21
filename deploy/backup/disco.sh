#!/bin/sh
# Alerta de disco cheio do CESJO SGI (issue #23, ADR 0069).
#
# Mede o uso do sistema de arquivos onde ficam os volumes do Docker (o mesmo do volume de arquivos
# montado em /dados/arquivos) e avisa SGI_DISCO_URL_FALHA quando passa do limite, ou
# SGI_DISCO_URL_SUCESSO quando está abaixo. Roda a cada SGI_DISCO_HORARIO pelo crond.
#
# Rodar à mão: docker compose exec backup disco.sh
set -eu

if [ -z "${RESTIC_REPOSITORY:-}" ] && [ -f /run/sgi-ambiente ]; then
	# shellcheck disable=SC1091
	. /run/sgi-ambiente
fi

: "${SGI_DISCO_LIMITE_PERCENTUAL:=85}"
: "${SGI_DISCO_CAMINHO:=/dados/arquivos}"

avisar() {
	[ -n "${1:-}" ] || return 0
	curl -fsS -m 15 --retry 3 -o /dev/null --data-raw "$2" "$1" \
		|| echo "[disco] aviso: não foi possível chamar a URL de alerta"
}

# Formato POSIX do df: a 5ª coluna da 2ª linha é o uso, por exemplo "42%".
uso="$(df -P "$SGI_DISCO_CAMINHO" | awk 'NR == 2 { sub("%", "", $5); print $5 }')"
livre="$(df -Ph "$SGI_DISCO_CAMINHO" | awk 'NR == 2 { print $4 }')"
mensagem="disco da VPS do CESJO SGI em ${uso}% (limite ${SGI_DISCO_LIMITE_PERCENTUAL}%, livre ${livre})"

if [ "$uso" -ge "$SGI_DISCO_LIMITE_PERCENTUAL" ]; then
	echo "[disco $(date '+%Y-%m-%dT%H:%M:%S%z')] ALERTA: $mensagem"
	avisar "${SGI_DISCO_URL_FALHA:-}" "$mensagem"
	exit 1
fi

echo "[disco $(date '+%Y-%m-%dT%H:%M:%S%z')] ok: $mensagem"
avisar "${SGI_DISCO_URL_SUCESSO:-}" "$mensagem"
