# ADR 0069 — Alerta de disco cheio pelo contêiner de backup

**Status:** Aceito (2026-11-21)

## Contexto

A issue #23 pede alerta de disco cheio. Com o disco cheio, o Postgres para de gravar, os uploads
falham e o backup não consegue gerar o dump. O monitor externo só enxerga o `/saude`, e o indicador
`diskSpace` do Spring Boot só fica `DOWN` com menos de 10 MB livres, tarde demais para agir.

Alternativas consideradas: agente de monitoramento do provedor da VPS (provedor desconhecido); um
contêiner só para isso (mais uma imagem para manter, com o mesmo `crond` e `curl` que o backup já
tem); baixar o limite do `diskSpace` do Actuator (derrubaria o `/saude` inteiro por causa de disco).

## Decisão

- O serviço `backup` ([ADR 0054](0054-backup-diario-com-restic.md)) ganha o script `disco.sh`,
  agendado no mesmo `crond` a cada `SGI_DISCO_HORARIO` (padrão `*/15 * * * *`).
- O script mede, com `df -P`, o sistema de arquivos do volume `cesjo-arquivos`, que é o mesmo onde o
  Docker guarda os volumes do banco e dos arquivos. Com uso igual ou acima de
  `SGI_DISCO_LIMITE_PERCENTUAL` (padrão 85%), registra `ALERTA` no log e chama `SGI_DISCO_URL_FALHA`;
  abaixo, chama `SGI_DISCO_URL_SUCESSO`.
- As variáveis ficam no `deploy/backup.env`. As URLs seguem o mesmo esquema de ping do backup: num
  serviço com prazo, a falta de ping também alerta, o que cobre o contêiner parado.

## Consequências

- O alerta chega com folga (15% do disco) e em até 15 minutos.
- Se os volumes do Docker estiverem noutro disco que não o do volume de arquivos (configuração fora
  do padrão), `SGI_DISCO_CAMINHO` precisa apontar para um caminho montado nesse disco.
- O contêiner de backup passa a ser também o contêiner de verificações periódicas da VPS.
