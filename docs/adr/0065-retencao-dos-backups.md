# ADR 0065 — Retenção dos backups: 7 diários, 4 semanais e 6 mensais

**Status:** Provisório (PENDENTE-CESJO P14) (2026-11-21)

## Contexto

O backup diário ([ADR 0054](0054-backup-diario-com-restic.md)) precisa de uma retenção configurada
(issue #21). Backups guardam também dados que já foram corrigidos ou excluídos no sistema, então o
prazo de guarda é uma questão de LGPD. A equipe CESJO ainda não disse por quanto tempo os dados ficam
guardados (pergunta 14). Guardar demais mantém dado pessoal sensível além do necessário; guardar de
menos impede recuperar um erro percebido tarde.

Alternativas consideradas: só os últimos 30 diários; 12 mensais (um ano); sem expurgo.

## Decisão

- PENDENTE-CESJO(P14): manter 7 backups diários, 4 semanais e 6 mensais, o que cobre cerca de seis
  meses para trás. Os padrões ficam num lugar só, no começo do `deploy/backup/backup.sh`, e podem ser
  trocados sem reconstruir a imagem por `SGI_BACKUP_MANTER_DIARIOS`, `SGI_BACKUP_MANTER_SEMANAIS` e
  `SGI_BACKUP_MANTER_MENSAIS` no `deploy/backup.env`.
- A retenção é aplicada a cada backup com `restic forget --prune`, só nos snapshots do host `cesjo`
  com a etiqueta `diario`. Snapshots feitos à mão com outra etiqueta não são apagados por ela.

## Consequências

- Um dado excluído do sistema some dos backups em no máximo uns seis meses.
- Um erro percebido depois de seis meses não pode ser desfeito pelo backup.
- Quando a equipe responder a pergunta 14, um ADR novo fixa o prazo; a troca é só de variável.
