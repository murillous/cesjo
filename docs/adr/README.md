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
- [0040](0040-geracao-de-pdf-e-excel-no-modulo-relatorios.md) — Geração de PDF e Excel no módulo Relatórios
- [0041](0041-tabela-simples-de-exportacao.md) — Tabela simples como contrato da exportação
- [0042](0042-resposta-http-de-download-da-exportacao.md) — Resposta HTTP de download e parâmetro de formato da exportação
- [0043](0043-auditoria-e-permissao-da-exportacao.md) — Auditoria e permissão da exportação
- [0044](0044-exportacao-no-frontend.md) — Serviço e botão de exportação no frontend
- [0045](0045-cadastro-e-api-java-dos-colaboradores.md) — Cadastro de colaboradores e API Java para os outros módulos (provisório)
- [0046](0046-ligacao-entre-colaborador-e-conta-de-acesso.md) — Ligação entre colaborador e conta de acesso guardada no colaborador
- [0047](0047-permissoes-dos-colaboradores.md) — Permissões do cadastro de colaboradores (provisório)
- [0048](0048-telas-de-colaboradores.md) — Telas de listagem e cadastro de colaboradores
- [0049](0049-candidato-em-avaliacao-e-api-da-lista-de-espera.md) — Candidato em avaliação e API Java da lista de espera (provisório)
- [0050](0050-empacotamento-e-compose-de-producao.md) — Imagens Docker e compose de produção em `deploy/` (provisório)
- [0051](0051-caddy-como-borda.md) — Caddy como borda: TLS, estáticos, proxy de `/api` e cabeçalhos
- [0052](0052-segredos-da-implantacao-no-env.md) — Segredos da implantação num `.env` fora do repositório
- [0053](0053-ci-das-imagens-de-implantacao.md) — CI constrói as imagens e testa a pilha sem publicar
- [0054](0054-backup-diario-com-restic.md) — Backup diário com restic para um repositório fora da VPS (provisório)
- [0055](0055-documentos-da-pessoa.md) — Documentos da pessoa no módulo Pessoas (provisório)
- [0056](0056-anexos-na-lista-de-espera.md) — Anexos na lista de espera e cadastro incompleto
- [0057](0057-ordem-e-posicao-na-fila.md) — Ordem e posição na fila da lista de espera (provisório)
- [0058](0058-dados-basicos-da-fila-para-o-consulta.md) — Dados básicos da fila para o perfil Consulta (provisório)
- [0059](0059-busca-em-lote-de-pessoas-para-a-fila.md) — Busca em lote de pessoas para montar a fila
- [0065](0065-retencao-dos-backups.md) — Retenção dos backups: 7 diários, 4 semanais e 6 mensais (provisório)
- [0066](0066-saude-publica-em-saude.md) — Estado de saúde publicado em `/saude`
- [0067](0067-monitor-externo-no-github-actions.md) — Monitor externo de disponibilidade no GitHub Actions (provisório)
- [0068](0068-relatorio-mensal-de-uptime.md) — Relatório mensal de uptime a partir das execuções do monitor
- [0069](0069-alerta-de-disco-cheio.md) — Alerta de disco cheio pelo contêiner de backup
- [0070](0070-agendamento-e-agenda-das-visitas.md) — Agendamento e agenda das visitas domiciliares
- [0071](0071-permissoes-das-visitas.md) — Permissões da agenda e do agendamento de visitas
- [0072](0072-telas-de-visitas-e-agenda-do-dia.md) — Telas da agenda de visitas e agenda do dia na página inicial
- [0073](0073-realizacao-cancelamento-e-reagendamento-da-visita.md) — Realização, cancelamento e reagendamento da visita, com observações cifradas
- [0074](0074-permissoes-do-resultado-da-visita.md) — Permissões para registrar a realização e ler as observações da visita (provisório)
- [0075](0075-acoes-na-ficha-da-visita.md) — Ações de realização, cancelamento e reagendamento na ficha da visita
- [0080](0080-insercao-no-servico.md) — Inserção no serviço: modelo, regras e cuidador familiar (provisório)
- [0081](0081-apis-entre-lista-de-espera-pessoas-e-atendidos.md) — APIs entre Lista de espera, Pessoas e Atendidos
- [0082](0082-permissoes-dos-usuarios-atendidos.md) — Permissões dos usuários atendidos (provisório)
- [0083](0083-listagem-e-ficha-dos-usuarios-atendidos.md) — Listagem e ficha dos usuários atendidos (provisório)
- [0090](0090-edicao-do-candidato.md) — Edição dos dados do candidato
- [0091](0091-situacoes-motivos-e-historico-da-inscricao.md) — Situações, motivos de saída e histórico da inscrição (provisório)
- [0092](0092-capacidade-e-painel-de-vagas.md) — Capacidade do serviço e painel de vagas (provisório)
- [0093](0093-contagem-de-vagas-ocupadas.md) — Contagem de vagas ocupadas atrás de uma interface
- [0094](0094-em-avaliacao-segue-na-fila.md) — Candidato em avaliação segue na fila (provisório)
- [0095](0095-aviso-de-vaga-aberta.md) — Aviso de vaga aberta (provisório)
- [0100](0100-escuta-ativa-e-qualificada.md) — Escuta ativa e qualificada com textos cifrados e leitura auditada
- [0101](0101-permissoes-da-escuta.md) — Permissões da escuta ativa e qualificada
- [0102](0102-telas-de-escuta.md) — Telas de escuta organizadas pela pessoa atendida
- [0103](0103-modelo-provisorio-e-versoes-do-pia.md) — Modelo provisório do PIA, conteúdo em JSON cifrado e versões (provisório)
- [0104](0104-permissoes-do-pia.md) — Permissões de leitura e edição do PIA
- [0105](0105-tela-do-pia.md) — Tela de elaboração e leitura do PIA
- [0106](0106-status-e-novas-versoes-do-pia.md) — Status do PIA em ordem fixa, com data, e mudança por nova versão (provisório)
- [0107](0107-aprovacao-do-pia-pelo-coordenador.md) — Aprovação do PIA só pelo Coordenador
- [0108](0108-impressao-do-pia.md) — PIA impresso ou salvo em PDF pelo navegador
- [0110](0110-desligamento-do-usuario-atendido.md) — Desligamento do usuário atendido (provisório)
- [0111](0111-evento-de-vaga-liberada.md) — Evento UsuarioDesligado para a vaga liberada
- [0112](0112-registro-de-violacao-de-direitos.md) — Registro de violação de direitos
- [0120](0120-procedimento-de-restauracao.md) — Restauração por script na imagem de backup, numa pilha com só o banco no ar
- [0121](0121-guarda-dos-segredos-fora-da-vps.md) — Cópia dos segredos da implantação fora da VPS (provisório)
- [0122](0122-teste-de-restauracao-no-ci-e-exercicio.md) — Restauração testada no CI a cada PR e exercício trimestral
- [0123](0123-auditoria-de-acessibilidade-no-ci.md) — Auditoria de acessibilidade com axe e Playwright no CI
- [0124](0124-correcoes-de-acessibilidade.md) — Correções de acessibilidade achadas pela auditoria
- [0125](0125-checklist-manual-de-navegadores.md) — Checklist manual para Safari no iPhone e leitor de tela
- [0130](0130-exportacao-da-lista-de-espera.md) — Exportação da lista de espera em PDF e Excel (provisório)
- [0135](0135-integracao-de-vagas-com-atendidos.md) — Vagas ocupadas pelos atendidos ativos e aviso de vaga no desligamento
- [0136](0136-insercao-de-candidato-em-avaliacao.md) — Candidato em avaliação pode ser inserido no serviço
- [0140](0140-campos-do-cadastro-completo.md) — Campos do cadastro completo do usuário atendido (provisório)
- [0141](0141-cifra-e-campos-consultaveis-do-cadastro.md) — O que é cifrado e o que fica consultável no cadastro completo
- [0142](0142-permissoes-do-cadastro-completo.md) — Permissões do cadastro completo e da crença e saúde (provisório)
- [0143](0143-opcoes-de-genero-e-escolaridade.md) — Opções de gênero e de escolaridade (provisório)
- [0144](0144-tela-do-cadastro-completo.md) — Tela de completar o cadastro do usuário atendido
- [0145](0145-consulta-de-violacoes-de-direitos.md) — Consulta de violações de direitos (provisório)
- [0150](0150-registro-de-atividades.md) — Registro de atividades com usuários e famílias (provisório)
- [0151](0151-api-java-de-contagem-de-atividades.md) — API Java do módulo Atividades para contagem por tipo e período
- [0152](0152-permissoes-das-atividades.md) — Permissões das atividades (provisório)
- [0153](0153-telas-de-atividades.md) — Telas de atividades: diário por data e ficha de registro
- [0154](0154-atendimentos-e-estudos-de-caso.md) — Atendimentos e estudos de caso ligados a um usuário, com leitura auditada (provisório)
- [0155](0155-listagem-de-atividades-por-periodo.md) — Listagem de atividades por período e atividades na ficha do usuário
- [0160](0160-registro-de-contatos-e-comunicacoes.md) — Registro de contatos e comunicações (provisório)
- [0161](0161-api-java-de-contagem-de-comunicacoes.md) — API Java de contagem de comunicações
- [0162](0162-permissoes-dos-contatos-e-comunicacoes.md) — Permissões dos contatos e comunicações
- [0163](0163-telas-de-contatos-e-comunicacoes.md) — Telas de contatos e comunicações
- [0164](0164-oficios-declaracoes-e-encaminhamentos.md) — Ofícios, declarações e encaminhamentos, com direção e anexo opcional (provisório)
- [0165](0165-telas-de-oficios-declaracoes-e-encaminhamentos.md) — Telas de ofícios, declarações e encaminhamentos
- [0170](0170-vinculos-dos-colaboradores.md) — Histórico de vínculos: contratações e desligamentos dos colaboradores (provisório)
- [0171](0171-tela-de-vinculos-dos-colaboradores.md) — Histórico de vínculos na ficha do colaborador
- [0172](0172-parceiros-e-acoes-de-articulacao.md) — Parceiros e ações de articulação (provisório)
- [0173](0173-permissoes-das-parcerias.md) — Permissões das parcerias (provisório)
- [0174](0174-telas-de-parcerias.md) — Telas de parcerias
- [0175](0175-reunioes-e-eventos-institucionais.md) — Reuniões e eventos institucionais
- [0176](0176-permissoes-das-reunioes-e-eventos.md) — Permissões das reuniões e eventos institucionais (provisório)
- [0177](0177-telas-de-reunioes-e-eventos.md) — Telas de reuniões e eventos
- [0178](0178-registro-de-doacoes.md) — Registro de doações recebidas e realizadas (provisório)
- [0179](0179-permissoes-das-doacoes.md) — Permissões das doações (provisório)
- [0180](0180-telas-de-doacoes.md) — Telas de doações
- [0195](0195-modulo-estatisticas-e-dashboard-de-perfil.md) — Módulo Estatísticas e dashboard de perfil dos usuários atendidos
- [0196](0196-permissao-dos-relatorios-e-dashboards.md) — Permissão dos relatórios e dashboards (provisório)
- [0197](0197-graficos-em-html-e-css-sem-biblioteca.md) — Gráficos em HTML e CSS, sem biblioteca, e área de relatórios com abas
- [0198](0198-dashboard-de-violacoes-atividades-e-desligamentos.md) — Dashboard de violações, atividades e desligamentos por período (provisório)
- [0199](0199-violacoes-no-dashboard-so-para-quem-le-violacoes.md) — Gráfico de violações só para quem lê violações de direitos (provisório)
