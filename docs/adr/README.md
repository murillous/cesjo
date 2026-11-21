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
- [0009](0009-identidade-neutra-e-erros-de-seguranca.md) — Identidade da conta fora do módulo acesso e erros de segurança fora do tratador genérico
- [0010](0010-api-de-sessao-e-seguranca-web.md) — API de sessão e segurança web do backend
- [0011](0011-expiracao-de-sessao-e-bloqueio-de-login.md) — Expiração da sessão por inatividade e bloqueio temporário de login
- [0012](0012-regra-de-senha.md) — Regra de senha e senhas temporárias
- [0013](0013-gestao-de-contas-de-acesso.md) — Gestão de contas de acesso pelo Coordenador
- [0014](0014-primeira-conta-de-coordenador.md) — Primeira conta de Coordenador por variáveis de ambiente
- [0015](0015-matriz-de-perfis-da-fundacao.md) — Matriz de perfis e permissões da Fundação (provisória)
- [0016](0016-sessao-no-frontend.md) — Sessão, telas de acesso e gestão de contas no frontend
- [0020](0020-tabela-e-conteudo-da-auditoria.md) — Tabela e conteúdo do registro de auditoria
- [0021](0021-transacao-e-falha-da-auditoria.md) — Transação do registro de auditoria e comportamento em falha
- [0022](0022-log-de-auditoria-so-de-insercao.md) — Log de auditoria só de inserção, guardado sem prazo
- [0023](0023-registro-de-leitura-sensivel-e-exportacao.md) — Leitura de dado sensível e exportação registradas por chamada explícita
- [0024](0024-consulta-do-log-de-auditoria.md) — Consulta do log de auditoria: filtros, período e tela
