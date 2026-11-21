# ADRs do CESJO SGI

Registros de decisão de arquitetura: o que foi decidido e por quê. Toda decisão do projeto vira um
ADR, inclusive as provisórias tomadas enquanto a equipe CESJO não responde uma pergunta. O código e
as docs citam a decisão como `ADR NNNN`.

O [planejamento](../planejamento.md) continua sendo o retrato geral (módulos, modelo de dados,
backlog). Os ADRs guardam o histórico: quando uma decisão do planejamento muda, entra um ADR novo.

## Formato

Um arquivo por decisão, `NNNN-slug.md`, com:

- **Status**: `Proposto`, `Aceito`, `Provisório (PENDENTE-CESJO Pn)`, `Substituído por ADR NNNN` ou
  `Descontinuado`, com a data;
- **Contexto**: o problema, as forças e as alternativas consideradas;
- **Decisão**: o que foi decidido;
- **Consequências**: o que muda, o que fica mais fácil e o que fica mais caro.

ADRs aceitos não são reescritos. Uma mudança de decisão vira um ADR novo que substitui o anterior,
e o antigo passa a apontar para ele no Status. Decisões provisórias seguem o
[ADR 0006](0006-pendencias-cesjo.md).

## Índice

Um ADR novo entra no fim da lista. Números podem ficar vagos quando um bloco reservado para uma
frente de trabalho paralela não é usado todo.

- [0001](0001-registrar-decisoes-em-adr.md) — Registrar decisões em ADR
- [0002](0002-monolito-modular-spring-angular.md) — Monólito modular com Spring Boot, PostgreSQL e Angular
- [0003](0003-autenticacao-por-sessao.md) — Autenticação por sessão com contas locais e Argon2
- [0004](0004-criptografia-na-aplicacao.md) — Criptografia AES-256-GCM na aplicação para texto livre sensível e arquivos
- [0005](0005-hospedagem-vps-docker-compose.md) — Hospedagem numa VPS com Docker Compose
- [0006](0006-pendencias-cesjo.md) — Decisões provisórias marcadas como `PENDENTE-CESJO`
- [0007](0007-contratos-da-fundacao.md) — Contratos públicos da Fundação entre módulos
- [0008](0008-postgres-nativo-nos-testes-sem-docker.md) — Postgres nativo nos testes quando não há Docker
