# ADR 0111 — Evento UsuarioDesligado para a vaga liberada

**Status:** Aceito (2026-11-21) · Issue #42

## Contexto

O desligamento (ADR 0110) libera uma vaga, e a notificação de vaga aberta (#31) é da Lista de espera.
Atendidos já depende da Lista de espera (ADR 0081). Se a Lista de espera chamasse Atendidos, ou ouvisse
um evento declarado em Atendidos, os dois módulos dependeriam um do outro, e o Spring Modulith recusa
o ciclo (`ModularidadeTests`).

Em paralelo, as PRs #89 e #92 (#30 e #31) criaram na Lista de espera a interface pública
`OcupacaoDeVagas` (com implementação padrão que conta inscrições `INSERIDO`) e
`AberturaDeVaga.vagasLiberadas()`. Esse código ainda não está na pilha desta PR.

Alternativas consideradas:

- Atendidos chamar `AberturaDeVaga` direto dentro do desligamento: amarra o serviço de desligamento a
  quem reage à vaga, e o código ainda não existe nesta pilha.
- Evento com o tipo declarado na Lista de espera, publicado por Atendidos: funciona, mas o evento
  descreve um fato de Atendidos.
- Evento de aplicação do Spring declarado em Atendidos e ouvido dentro de Atendidos por um ouvinte que
  chama a API da Lista de espera.

## Decisão

- O evento é o record público `atendidos.UsuarioDesligado(insercaoId, data)`, sem dado pessoal.
  `ServicoDesligamento` o publica com `ApplicationEventPublisher` dentro da transação do
  desligamento.
- Quem reage à vaga ouve o evento num módulo que pode depender de Atendidos. Para a notificação de
  vaga, o ouvinte fica em Atendidos e chama a Lista de espera, na direção que já existe.
- O ouvinte deve usar `@EventListener` (mesma transação: a notificação, que também entra na transação
  de quem chama, ADR 0031, é desfeita junto se o desligamento falhar) ou
  `@TransactionalEventListener(phase = AFTER_COMMIT)`. Não há registro de publicações do Spring
  Modulith, então ouvinte assíncrono não tem garantia de entrega.
- A ligação com o código das PRs #89 e #92 entra num PR de integração depois dos merges:
  - Atendidos implementa `OcupacaoDeVagas` com `UsuariosAtendidos.contarAtivos()`, no lugar da
    implementação padrão que conta `INSERIDO` (que deixaria de ver os desligamentos);
  - um ouvinte de `UsuarioDesligado` em Atendidos chama `AberturaDeVaga.vagasLiberadas()`;
  - `ListaEspera.registrarInsercao` (ADR 0081) passa a gravar `historico_situacao_inscricao`.

## Consequências

- A direção das dependências continua Atendidos para Lista de espera, sem ciclo.
- Até o PR de integração, o desligamento publica o evento e ninguém ouve: a vaga aparece nas contagens
  (`contarAtivos`), mas a notificação ainda não sai.
- Turno e motivo não vão no evento. Se a capacidade for por turno (P3), o turno entra como campo novo.
- O teste `DesligamentoTests` confere a publicação com `@RecordApplicationEvents`.
