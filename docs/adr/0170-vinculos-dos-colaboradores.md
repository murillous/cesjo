# ADR 0170 — Histórico de vínculos: contratações e desligamentos dos colaboradores

**Status:** Provisório (PENDENTE-CESJO P26) (2026-11-22) · Issue #51

## Contexto

O RF014 pede o registro de contratações e desligamentos de colaboradores. O cadastro da issue #50
(ADR 0045) só tem a situação `ativo`, e o ADR 0045 deixou as datas e o motivo do desligamento para
esta issue. O planejamento esboça a tabela `vinculo_colaborador` com `colaborador_id`,
`data_contratacao`, `data_desligamento` e `motivo`. Um colaborador desligado não pode aparecer para
novos agendamentos (visitas, escutas e, no futuro, atividades), que já leem só os ativos pela API
Java `Colaboradores`.

O documento de requisitos não diz quais são os motivos de desligamento de colaborador.

Alternativas consideradas:

- Datas de contratação e desligamento como colunas do próprio `colaborador`: perde o histórico de
  quem sai e volta à equipe.
- Motivo como lista fechada (pedido de demissão, fim de contrato, dispensa, aposentadoria, outro):
  permite contar por motivo, mas seria inventar uma lista que a equipe não deu.
- Derivar `ativo` só dos vínculos: obriga a migrar os colaboradores já cadastrados sem data de
  contratação, que a equipe não tem como informar agora.

## Decisão

- Tabela `vinculo_colaborador` (migração `V55`), dona do módulo `institucional`: um vínculo por
  período de trabalho. A contratação abre o vínculo; o desligamento o fecha com data e motivo. No
  máximo um vínculo aberto por colaborador (índice único parcial), desligamento com data e motivo
  juntos e não anterior à contratação (restrição `ck_vinculo_desligamento`).
- Regras no `ServicoVinculos`: datas não podem ser futuras; não há contratação com vínculo aberto
  nem anterior ao último desligamento; desligar exige vínculo aberto.
- O desligamento deixa o colaborador inativo e a contratação o deixa ativo, na mesma transação.
  Assim a API `Colaboradores` continua sendo a única regra de quem aparece para novos agendamentos,
  sem mudar visitas e escutas. Registros antigos seguem mostrando o nome pelo `obterVarios`.
- Colaborador desligado (tem vínculos e nenhum aberto) só volta a ficar ativo por nova contratação:
  a edição recusa `ativo = true` com 422. Colaborador sem nenhum vínculo (cadastrado antes desta
  issue) mantém a situação livre na edição, como no ADR 0045. A troca de `ativo` com vínculo aberto
  continua livre, para afastamentos.
- O cadastro aceita `dataContratacao` opcional, que já abre o primeiro vínculo.
- O motivo do desligamento é texto livre obrigatório (até 2000 caracteres), cifrado na aplicação
  (formato v1 do ADR 0025), porque pode trazer detalhes pessoais e trabalhistas. É provisório
  (pergunta 26): `PENDENTE-CESJO(P26)` em `NovoDesligamentoColaborador`.
- API: `GET /api/colaboradores/{id}/vinculos` (`COLABORADORES_LER`), `POST .../contratacoes` (201) e
  `POST .../desligamento` (`COLABORADORES_GERENCIAR`). Sem permissão nova: as mesmas do cadastro
  (ADR 0047), então Equipe Técnica, Administrativo e Consulta seguem sem acesso (P17).
- Auditoria: criação do vínculo na contratação; alteração com `data_desligamento` e
  `motivo_desligamento` no desligamento; alteração de `ativo` do colaborador quando a situação muda.
  O log guarda só os nomes dos campos.

## Consequências

- Relatórios de rotatividade da equipe podem ler os períodos de trabalho por colaborador.
- Se a equipe CESJO der uma lista de motivos, o motivo vira enum com observação, numa migração nova;
  os motivos já gravados ficam como observação de um motivo "outro".
- Corrigir um vínculo registrado errado ainda não tem tela; fica para quando a equipe pedir.
