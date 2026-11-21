# ADR 0122 — Restauração testada no CI a cada PR e exercício trimestral

**Status:** Aceito (2026-11-21)

## Contexto

Backup que nunca foi restaurado não é backup. A issue #22 pede uma restauração executada com sucesso e
com o tempo registrado. Ainda não existe VPS para um exercício real (pergunta 15), e um teste feito
uma vez só envelhece a cada mudança no esquema ou na imagem.

## Decisão

- O job `implantacao` do CI ([ADR 0053](0053-ci-das-imagens-de-implantacao.md)) faz uma
  recuperação de desastres completa a cada PR: grava uma tabela e um arquivo de referência, faz o
  backup, apaga a pilha inteira com `docker compose down -v`, sobe só o banco, roda o `restaurar.sh`,
  sobe o resto e confere a tabela, o arquivo, a escrita no volume, a contagem de contas e o `/saude`.
  O tempo do banco vazio ao sistema no ar vai para o resumo da execução.
- O repositório restic do CI fica numa pasta do runner montada por um `compose.ci.yaml` gerado no
  job, para sobreviver ao `down -v`, como um destino fora da VPS sobreviveria.
- Em produção, a cada três meses, alguém da equipe restaura o último backup numa VPS ou máquina
  descartável seguindo `docs/recuperacao-de-desastres.md` e anota a data, o snapshot e o tempo na
  tabela de exercícios do próprio guia.

## Consequências

- Uma mudança que quebre a restauração (esquema, imagem, script) falha no PR.
- O tempo medido no CI é de uma base quase vazia; o exercício trimestral mede o tempo real, com o
  volume de dados da instituição e o download do destino remoto.
