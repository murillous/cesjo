# syntax=docker/dockerfile:1
# Serviço de backup do CESJO SGI (issue #21, ADR 0054 e ADR 0065) e alerta de disco cheio
# (issue #23, ADR 0069).
# Base postgres:18-alpine para o pg_dump ter a mesma versão do servidor.
FROM postgres:18-alpine
RUN apk add --no-cache restic curl tzdata
COPY --chmod=755 backup.sh disco.sh restaurar.sh entrada.sh /usr/local/bin/
ENTRYPOINT ["/usr/local/bin/entrada.sh"]
