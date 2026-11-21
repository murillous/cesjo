# ADR 0070 — Agendamento e agenda das visitas domiciliares

**Status:** Aceito (2026-11-21) · Issue #34

## Contexto

O RF004 pede o agendamento de visitas domiciliares para avaliação de perfil, com data e hora, um ou
mais profissionais (assistente social, psicóloga, terapeuta ocupacional) e status agendada,
realizada ou cancelada. A visita é de um candidato da lista de espera (ADR 0049) e os profissionais
são colaboradores ativos (ADR 0045). A equipe precisa ver a agenda por período.

Alternativas consideradas para a data e hora: `timestamptz` (o "dia" de uma visita mudaria com o fuso
de quem consulta, e o servidor roda em UTC); data e hora em colunas separadas (complica ordenar e
filtrar por período).

## Decisão

- O módulo `atendimento` é dono de `visita_domiciliar` e `visita_profissional` (migração `V15`). A
  visita guarda o `pessoa_id` e os `colaborador_id`; nomes vêm de `Pessoas.nomes` e
  `Colaboradores.obterVarios` (que traz também inativos, para as visitas antigas).
- `data_hora` é `timestamp` sem fuso, a hora local do Centro-Dia. "Hoje" é sempre o dia em
  `America/Sao_Paulo` (`atendimento.interno.Fuso`), mesmo com o servidor em UTC.
- Agendar (`POST /api/visitas`, 201 com `Location`) exige pessoa com inscrição ativa, pelo menos um
  profissional, todos ativos (`Colaboradores.exigirAtivos`), e um dia que não tenha passado (hoje
  vale, inclusive horário anterior ao atual, para registrar a visita marcada de manhã). A visita nasce
  `AGENDADA` e o candidato vai para em avaliação, na mesma transação.
- Agenda: `GET /api/visitas?de=AAAA-MM-DD&ate=AAAA-MM-DD`, datas inclusive, em ordem de horário,
  com no máximo 62 dias por consulta. É uma lista sem paginação: o período limitado faz o papel da
  página (seção 7, desempenho).
- `GET /api/visitas/{id}` devolve uma visita. `GET /api/visitas/candidatos` e
  `GET /api/visitas/profissionais` alimentam a escolha na tela de agendamento, sob a permissão de
  agendar.
- Auditoria: criação da visita e alteração da situação da inscrição.

## Consequências

- A realização, o cancelamento e o reagendamento (issue #35) mudam o status da mesma tabela.
- Se o serviço passar a atender em outro fuso, só `Fuso` muda.
- Uma visita fica com o nome do profissional mesmo depois de ele ficar inativo.
