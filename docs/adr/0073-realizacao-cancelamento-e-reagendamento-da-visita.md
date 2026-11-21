# ADR 0073 — Realização, cancelamento e reagendamento da visita, com observações cifradas

**Status:** Aceito (2026-11-21) · Issue #35

## Contexto

O planejamento (item 26) pede a mudança da visita para "realizada" ou "cancelada" com motivo,
observações cifradas e reagendamento criando visita nova. As observações são texto livre sobre a
casa e a família: dado sensível pela seção 7 (criptografia e auditoria de leitura).

Alternativas consideradas: editar data e hora da visita no lugar (perde o histórico da visita que não
aconteceu, contra o critério de aceite); devolver as observações junto com a visita (toda abertura da
agenda ou da ficha viraria leitura sensível no log, e o Administrativo veria o texto); observações
também no cancelamento (o motivo já cobre, e o planejamento só marca `observacoes` como cifrada).

## Decisão

- Migração `V16`: `motivo_cancelamento` (até 500 caracteres, em claro, como no planejamento),
  `observacoes` (`text`, cifrada pelo `TextoCifradoConverter`) e `visita_origem_id` (a visita cancelada
  que esta reagenda, única). Um `CHECK` exige o motivo em toda visita cancelada.
- Só uma visita `AGENDADA` muda de status; depois disso ela não muda mais (422).
- `POST /api/visitas/{id}/realizar` com `observacoes` opcionais (até 5.000 caracteres). Uma visita
  de dia futuro ainda não pode ser realizada.
- `POST /api/visitas/{id}/cancelar` com `motivo` obrigatório.
- `POST /api/visitas/{id}/reagendar` com nova data e hora, profissionais e motivo: numa transação
  só, cancela a visita com o motivo e cria a nova para a mesma pessoa, com as regras do agendamento
  (ADR 0070). Responde 201 com a visita nova. Se a nova não passar nas regras, a antiga continua
  agendada.
- A visita devolvida pela API nunca traz as observações, só `temObservacoes`, além de
  `motivoCancelamento`, `visitaOrigemId` e `reagendamentoId`. As observações saem só por
  `GET /api/visitas/{id}/observacoes`, e cada leitura é registrada como `LER_SENSIVEL`.
- Auditoria: alteração com os campos `status` e `observacoes` (realizar) ou `status` e
  `motivo_cancelamento` (cancelar e reagendar), e criação da visita nova no reagendamento.
- O candidato continua em avaliação depois do cancelamento (ADR 0049).

## Consequências

- O histórico de uma pessoa mostra todas as tentativas de visita, ligadas pelo reagendamento.
- Corrigir as observações de uma visita já realizada não é possível nesta versão; se a equipe
  precisar, entra uma edição auditada.
