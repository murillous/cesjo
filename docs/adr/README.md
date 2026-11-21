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
- [0025](0025-formato-do-valor-cifrado-e-rotacao.md) — Formato do valor cifrado, configuração das chaves e rotação
- [0026](0026-armazenamento-de-arquivos-cifrados.md) — Armazenamento dos arquivos cifrados e tabela `arquivo`
- [0027](0027-validacao-de-tipo-e-tamanho-dos-arquivos.md) — Validação de tipo e tamanho dos arquivos enviados
- [0028](0028-api-http-e-api-java-dos-arquivos.md) — API HTTP e API Java do módulo Arquivos
- [0029](0029-componente-de-envio-de-arquivo-no-frontend.md) — Componente de envio de arquivo no frontend
- [0030](0030-modelo-das-notificacoes.md) — Notificação por perfil ou por conta, com leitura registrada por conta
- [0031](0031-api-java-de-notificacoes.md) — API Java síncrona para outros módulos criarem notificações
- [0032](0032-api-http-da-caixa-de-notificacoes.md) — API HTTP da caixa de notificações da conta logada
- [0033](0033-auditoria-das-notificacoes.md) — O que é auditado nas notificações
- [0034](0034-contador-por-consulta-periodica.md) — Contador de notificações por consulta periódica de 60 segundos
- [0035](0035-frontend-mobile-first-e-navegacao-inferior.md) — Frontend mobile first com navegação inferior no celular
- [0036](0036-fonte-atkinson-hyperlegible.md) — Fonte Atkinson Hyperlegible Next enquanto não há fonte institucional (provisória)
- [0037](0037-pessoa-e-inscricao-na-lista-de-espera.md) — Pessoa no módulo Pessoas e inscrição no módulo Lista de espera
- [0038](0038-dados-do-cadastro-na-lista-de-espera.md) — Dados obrigatórios e validação do cadastro na lista de espera (provisório)
- [0039](0039-permissoes-da-lista-de-espera.md) — Permissões do cadastro e da leitura da lista de espera (provisório)
- [0045](0045-cadastro-e-api-java-dos-colaboradores.md) — Cadastro de colaboradores e API Java para os outros módulos (provisório)
- [0046](0046-ligacao-entre-colaborador-e-conta-de-acesso.md) — Ligação entre colaborador e conta de acesso guardada no colaborador
- [0047](0047-permissoes-dos-colaboradores.md) — Permissões do cadastro de colaboradores (provisório)
- [0048](0048-telas-de-colaboradores.md) — Telas de listagem e cadastro de colaboradores
- [0049](0049-candidato-em-avaliacao-e-api-da-lista-de-espera.md) — Candidato em avaliação e API Java da lista de espera (provisório)
- [0070](0070-agendamento-e-agenda-das-visitas.md) — Agendamento e agenda das visitas domiciliares
- [0071](0071-permissoes-das-visitas.md) — Permissões da agenda e do agendamento de visitas
- [0072](0072-telas-de-visitas-e-agenda-do-dia.md) — Telas da agenda de visitas e agenda do dia na página inicial
- [0073](0073-realizacao-cancelamento-e-reagendamento-da-visita.md) — Realização, cancelamento e reagendamento da visita, com observações cifradas
- [0074](0074-permissoes-do-resultado-da-visita.md) — Permissões para registrar a realização e ler as observações da visita (provisório)
- [0075](0075-acoes-na-ficha-da-visita.md) — Ações de realização, cancelamento e reagendamento na ficha da visita
- [0100](0100-escuta-ativa-e-qualificada.md) — Escuta ativa e qualificada com textos cifrados e leitura auditada
- [0101](0101-permissoes-da-escuta.md) — Permissões da escuta ativa e qualificada
- [0102](0102-telas-de-escuta.md) — Telas de escuta organizadas pela pessoa atendida
- [0103](0103-modelo-provisorio-e-versoes-do-pia.md) — Modelo provisório do PIA, conteúdo em JSON cifrado e versões (provisório)
- [0104](0104-permissoes-do-pia.md) — Permissões de leitura e edição do PIA
- [0105](0105-tela-do-pia.md) — Tela de elaboração e leitura do PIA
- [0106](0106-status-e-novas-versoes-do-pia.md) — Status do PIA em ordem fixa, com data, e mudança por nova versão (provisório)
- [0107](0107-aprovacao-do-pia-pelo-coordenador.md) — Aprovação do PIA só pelo Coordenador
- [0108](0108-impressao-do-pia.md) — PIA impresso ou salvo em PDF pelo navegador
