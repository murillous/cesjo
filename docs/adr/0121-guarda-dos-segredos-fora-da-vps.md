# ADR 0121 — Cópia dos segredos da implantação fora da VPS

**Status:** Provisório (PENDENTE-CESJO P15) (2026-11-21)

## Contexto

Sem as chaves de criptografia da aplicação (ADR 0025), a senha do repositório restic e as
credenciais do destino do backup, uma VPS perdida não se recupera: o backup existe, mas é ilegível.
Esses segredos estão no `deploy/.env` e no `deploy/backup.env`, só na VPS
([ADR 0052](0052-segredos-da-implantacao-no-env.md), [ADR 0054](0054-backup-diario-com-restic.md)).
Ainda não se sabe quem administra a VPS (pergunta 15).

Alternativas consideradas: guardar os segredos no próprio destino do backup (quem perde o acesso ao
destino perde tudo junto); guardar só na cabeça de quem implantou; um cofre de segredos pago.

## Decisão

- PENDENTE-CESJO(P15): a coordenação do CESJO guarda duas cópias do `.env` e do `backup.env`:
  1. num gerenciador de senhas com conta da instituição (por exemplo Bitwarden ou 1Password), com
     acesso de pelo menos duas pessoas;
  2. impressa ou num pen drive cifrado, em envelope lacrado no cofre ou arquivo da instituição.
- Nenhuma das cópias fica na VPS, no repositório, no e-mail ou no mesmo provedor do destino do backup.
- Toda troca de chave ou de senha (rotação, troca de destino) atualiza as duas cópias no mesmo dia, e
  o guia de recuperação traz a lista de conferência.

## Consequências

- A perda da VPS, do provedor ou de uma pessoa não impede a recuperação.
- Duas pessoas e um envelope sabem os segredos que abrem todos os dados sensíveis; o acesso a eles é
  restrito como o de qualquer documento sigiloso.
- Quando a pergunta 15 for respondida, um ADR novo diz quem são os guardiões e onde ficam as cópias.
