# ADR 0111 — Evento de vaga liberada publicado por Atendidos e ouvido pela Lista de espera

**Status:** Aceito (2026-11-21) · Issue #42

## Contexto

O desligamento (ADR 0110) libera uma vaga, e a notificação de vaga aberta (#31) é da Lista de espera.
Atendidos já depende da Lista de espera (ADR 0081). Se a Lista de espera chamasse Atendidos, ou ouvisse
um evento declarado em Atendidos (por exemplo `atendidos.UsuarioDesligado`), os dois módulos
dependeriam um do outro, e o Spring Modulith recusa o ciclo (`ModularidadeTests`).

Alternativas consideradas:

- `atendidos.UsuarioDesligado`, ouvido pela Lista de espera: cria o ciclo.
- Atendidos chamar uma API Java da Lista de espera ("vaga liberada"): funciona, mas amarra Atendidos a
  quem reage à vaga; outros ouvintes (relatórios, painel) exigiriam mais chamadas.
- Evento de aplicação do Spring com o tipo declarado pelo módulo que ouve: a Lista de espera publica o
  contrato do que quer ouvir, e Atendidos, que já depende dela, publica.

## Decisão

- O evento é o record público `listaespera.VagaLiberada(insercaoId, data)`, sem dado pessoal. Atendidos
  o publica com `ApplicationEventPublisher` ao registrar o desligamento.
- A publicação acontece dentro da transação do desligamento. O ouvinte da #31 deve usar
  `@EventListener` (roda na mesma transação: a notificação, que também entra na transação de quem
  chama, ADR 0031, é desfeita junto se o desligamento falhar) ou
  `@TransactionalEventListener(phase = AFTER_COMMIT)` se preferir avisar só depois de gravado. Não há
  registro de publicações do Spring Modulith (`spring-modulith-starter-jpa`), então ouvinte assíncrono
  não tem garantia de entrega.
- Turno e motivo não vão no evento. Se a capacidade for por turno (P3), o turno entra como campo novo.

## Consequências

- A direção das dependências continua Atendidos para Lista de espera, sem ciclo.
- Outros módulos que dependam da Lista de espera podem ouvir o mesmo evento.
- O teste `DesligamentoTests` confere a publicação com `@RecordApplicationEvents`.
