# Planejamento do CESJO SGI

Data: 2026-11-20. Fonte única de requisitos:
[documentoRequisitosCentroDia.pdf](requisitos/documentoRequisitosCentroDia.pdf). Identidade visual:
[guiaDeEstilo.md](frontend/guiaDeEstilo.md).

Este documento registra as decisões de arquitetura, o esboço do modelo de dados, os perfis e permissões,
os aspectos transversais, a ordem de entrega e o backlog que vira issues no GitHub. Tudo o que o
documento de requisitos não define fica na seção [Perguntas para a equipe CESJO](#9-perguntas-para-a-equipe-cesjo)
e não é decidido por conta própria.

## 1. Decisões

| Tema | Decisão |
|---|---|
| Ponto de partida | Recomeço do zero. O MVP antigo fica só na tag `mvp-legado` e não é reaproveitado. Nenhuma dependência de thera-org/kernel ou thera-org/naupe. |
| Organização | Uma única organização (CESJO). Sem multi-tenancy. |
| Backend | Java 25, Spring Boot 4.2, Spring Web MVC, Spring Data JPA, Spring Security, Flyway, PostgreSQL 18. Monólito modular. |
| Testes backend | JUnit, Testcontainers com Postgres real, Spring Modulith para verificar fronteiras entre módulos. |
| Frontend | Angular 22 (componentes standalone, signals, formulários reativos tipados), Angular Material 22 e Angular CDK (licença MIT) com tema Material 3 construído a partir dos tokens do guia de estilo, ícones Material Symbols; biblioteca de gráficos a definir na issue de dashboards. Testes com Vitest e alguns fluxos ponta a ponta com Playwright. O PrimeNG foi descartado porque, a partir da versão 22, passou a exigir chave de licença PrimeUI. |
| Repositório | Monorepo: `backend/` e `frontend/`. |
| Autenticação | Sessão do Spring Security com contas locais no Postgres, senha com Argon2, cookie HttpOnly, proteção CSRF. Frontend e API no mesmo domínio (proxy). |
| Criptografia | AES-256-GCM na aplicação para campos de texto livre sensíveis e para todos os arquivos enviados. Campos categóricos (tipo de violação, crença, tipo de deficiência) ficam em claro, protegidos por perfil, auditoria de leitura e disco cifrado, para que relatórios e dashboards agreguem direto em SQL. |
| Hospedagem | VPS com Docker Compose: Caddy (TLS e arquivos estáticos do Angular), aplicação Spring Boot e Postgres. Backup diário cifrado fora da VPS. |

### Por que monólito modular

O volume é pequeno (centenas de pessoas atendidas, dezenas de contas de acesso) e há uma só organização.
Um único artefato com módulos bem separados é mais barato de operar numa VPS do que serviços
independentes, e o Spring Modulith garante em teste que um módulo só usa a API pública do outro.

## 2. Glossário

O documento de requisitos usa "usuário" no sentido do SUAS: a pessoa atendida pelo serviço. Para não
confundir com quem entra no sistema, o código e as telas usam:

| Termo | Significado |
|---|---|
| Pessoa | Pessoa com deficiência, desde o cadastro na lista de espera. Guarda dados pessoais e responsáveis. |
| Candidato | Pessoa com inscrição ativa na lista de espera (RF001 a RF003). |
| Usuário atendido | Pessoa inserida no serviço (RF007). Corresponde a uma inserção ativa. |
| Responsável / cuidador familiar | Familiar ligado à pessoa. O responsável aparece no RF001; o cuidador familiar responsável, no RF007. |
| Colaborador | Integrante da equipe do Centro-Dia (RF014). Profissionais das visitas, escutas e atividades são colaboradores. |
| Conta de acesso | Login no sistema, com um perfil (Coordenador, Equipe Técnica, Administrativo, Consulta). Pode estar ligada a um colaborador. |

## 3. Arquitetura do backend

Pacote raiz `br.org.cesjo.sgi`. Cada módulo é um pacote de primeiro nível. Só o que estiver no pacote
raiz do módulo (serviços de aplicação, eventos, DTOs públicos) pode ser usado por outro módulo; o resto
fica em subpacotes internos.

| Módulo | Requisitos | Responsabilidade |
|---|---|---|
| `acesso` | RNF 3.3, seção 4 | Contas de acesso, login, perfis, permissões. |
| `auditoria` | RNF 3.3 | Registro e consulta do log de auditoria. |
| `arquivos` | RF001, RNF 3.3 | Armazenamento cifrado de arquivos e metadados. |
| `notificacoes` | RF003 | Notificações internas exibidas no sistema. |
| `pessoas` | RF001, RF007 | Pessoa, responsáveis, cuidadores, documentos da pessoa. |
| `listaespera` | RF001 a RF003 | Inscrições, posição na fila, capacidade e vagas. |
| `atendimento` | RF004 a RF006 | Visitas domiciliares, escuta ativa e qualificada, PIA. |
| `atendidos` | RF007 a RF009 | Inserção no serviço, desligamentos, violações de direitos. |
| `atividades` | RF010, RF011 | Atividades, atendimentos, estudos de caso, contatos e comunicações. |
| `institucional` | RF012 a RF015 | Reuniões e eventos, parcerias, colaboradores, doações. |
| `relatorios` | seção 5.2 | Exportação PDF e Excel, usada pelos outros módulos. |
| `estatisticas` | RF016, RF017 | Relatórios estatísticos e dashboards, lidos das APIs Java dos outros módulos (ADR 0195). |
| `importacao` | seção 5.2 | Importação de planilhas Excel. |

Código de infraestrutura compartilhado (conversor de criptografia, tratamento de erros, configuração web)
fica em `br.org.cesjo.sgi.compartilhado`, sem regra de negócio.

Convenções:

- API REST em `/api/**`, JSON, erros no formato `ProblemDetail` (RFC 9457), listas paginadas.
- Documentação da API com springdoc-openapi, disponível só fora de produção.
- Esquema do banco somente por migrações Flyway. O Hibernate apenas valida (`ddl-auto=validate`).
- Relatórios estatísticos e dashboards ficam no módulo `estatisticas` e leem os outros módulos só
  pelas APIs Java públicas deles, que fazem as contagens em SQL agregado (ADR 0195).
- Eventos de domínio do Spring Modulith ligam módulos sem dependência direta. Exemplo: o desligamento
  de um usuário atendido publica um evento, e `listaespera` recalcula as vagas e pede uma notificação.

## 4. Arquitetura do frontend

- Projeto Angular em `frontend/`, rotas com carregamento sob demanda por módulo (as mesmas áreas do
  backend).
- Layout institucional com Angular Material (`MatToolbar` e `MatSidenav`): barra superior com logo e
  notificações, menu lateral fixo em telas grandes e gaveta recolhível no celular, fundo branco e
  destaques conforme o guia de estilo.
- Tema Material 3 gerado a partir dos tokens semânticos do guia (`primary`, `primaryDark`,
  `accent`, `backgroundSecondary` etc.). Nenhum valor HEX dentro de componentes.
- Contraste WCAG AA: texto em `primary-dark` sobre branco, nunca turquesa sobre branco em texto pequeno.
- Locale `pt-BR` (datas, números, mensagens de validação).
- Guards de rota e itens de menu filtrados pelas permissões da conta. O backend continua sendo a
  autoridade: o frontend só esconde o que a conta não pode usar.
- Em desenvolvimento, `ng serve` faz proxy de `/api` para o backend. Em produção, o Caddy serve os
  arquivos estáticos e repassa `/api` para a aplicação, no mesmo domínio, o que permite cookie de
  sessão sem CORS.

## 5. Esboço do modelo de dados

Campos marcados com (C) são cifrados na aplicação. Todas as tabelas têm `id` (UUID), `criado_em`,
`criado_por`, `atualizado_em`, `atualizado_por`. Este é um esboço; cada issue fecha os detalhes da sua
migração.

### Acesso, auditoria, arquivos, notificações

- `conta_acesso`: nome, login, senha_hash, perfil, ativa, tentativas_falhas, bloqueada_ate. A ligação
  com o colaborador fica em `colaborador.conta_acesso_id` (ADR 0046).
- `auditoria`: ocorrido_em, conta_id, acao (criar, alterar, excluir, ler_sensivel, exportar, login,
  login_falho), entidade, entidade_id, campos_alterados (nomes, sem valores sensíveis), ip.
- `arquivo`: nome_original, tipo_mime, tamanho, sha256, caminho_armazenamento, chave_id, enviado_por,
  enviado_em.
- `notificacao`: perfil_destino ou conta_destino, tipo, mensagem, referencia, criada_em, lida_em.

### Pessoas e lista de espera (RF001 a RF003)

- `pessoa`: nome_completo, data_nascimento, genero, tipo_deficiencia (sensorial, física, TEA,
  intelectual, múltipla, T21), telefone, endereço, bairro, município, crenca_religiosa, demais dados
  pessoais (a definir, ver perguntas).
- `responsavel`: pessoa_id, nome_completo, parentesco, telefone, cuidador_principal.
- `documento_pessoa`: pessoa_id, tipo (laudo_medico, comprovante_residencia, outro), arquivo_id.
- `inscricao_lista_espera`: pessoa_id, data_cadastro, origem_encaminhamento (CRAS, CREAS, Judiciário,
  Saúde, Educação, Demanda espontânea), situacao, data_saida, motivo_saida, observacoes (C).
  A posição na fila é calculada por ordem de `data_cadastro` entre as inscrições aguardando, sem coluna
  própria. Se a equipe definir prioridade para BPC ou CadÚnico, a ordenação muda num único lugar.
- `capacidade_servico`: vagas totais (por turno ou não, ver perguntas), vigente_desde.

### Atendimento (RF004 a RF006)

- `colaborador`: nome, funcao (assistente social, psicóloga, terapeuta ocupacional, outras), ativo,
  conta_acesso_id (opcional; a ligação fica no colaborador, ADR 0046).
- `visita_domiciliar`: pessoa_id, data_hora, status (agendada, realizada, cancelada),
  motivo_cancelamento, observacoes (C).
- `visita_profissional`: visita_id, colaborador_id.
- `escuta`: pessoa_id, data, colaborador_id, dinamica_familiar (C), necessidades_imediatas (C),
  habilidades (C), potencialidades (C).
- `pia`: pessoa_id, versao, conteudo (C, estrutura a definir), status (elaborado, apresentado à
  família, apresentado à equipe, aprovado), datas de cada mudança, aprovado_por.

### Usuários atendidos (RF007 a RF009)

- `insercao`: pessoa_id, data_insercao, turno (matutino, vespertino, integral),
  cuidador_responsavel_id, situacao (ativa, desligada). A faixa etária (18 a 29 jovem, 30 a 59 adulto)
  é calculada da data de nascimento, não guardada.
- `desligamento`: insercao_id, data, motivo (evasão, mudança de município, solicitação da família,
  alcance dos objetivos, outros), observacao (C).
- `violacao_direito`: pessoa_id, tipo (as 12 categorias do RF009), ambito (intrafamiliar,
  extrafamiliar, quando a categoria pede), data_identificacao, descricao (C), registrado_por.

### Atividades e comunicações (RF010, RF011)

- `atividade`: tipo (as 6 categorias do RF010), data, titulo, descricao (C quando for atendimento
  individual ou estudo de caso).
- `atividade_participante`: atividade_id, pessoa_id ou responsavel_id.
- `atividade_profissional`: atividade_id, colaborador_id.
- `comunicacao`: tipo (as categorias do RF011), direcao (enviada, recebida), data, instituicao,
  pessoa_id (opcional), assunto, descricao, arquivo_id (opcional).

### Gestão institucional (RF012 a RF015)

- `evento_institucional`: tipo (as categorias do RF012), direcao (realizado, recebido, participado,
  quando couber), data, descricao, parceiro_id (opcional).
- `parceiro`: nome, tipo, contato, situacao. `articulacao_parceria`: parceiro_id, data, descricao.
- `vinculo_colaborador`: colaborador_id, data_contratacao, data_desligamento, motivo.
- `doacao`: direcao (recebida, realizada), data, contraparte, tipo, descricao, quantidade ou valor.

## 6. Perfis e permissões

O código usa permissões finas (por exemplo `LISTA_ESPERA_EDITAR`, `PIA_APROVAR`) e cada perfil é um
conjunto fixo delas, definido no código. Uma conta tem um perfil. Matriz inicial, a confirmar com a
equipe CESJO nos pontos marcados com (?):

| Área | Coordenador | Equipe Técnica | Administrativo | Consulta |
|---|---|---|---|---|
| Contas de acesso e log de auditoria | gerenciar | não | não | não |
| Lista de espera (RF001 a RF003) | total | ler e editar | total | dados básicos |
| Laudos e comprovantes | ler | ler | enviar e ler (?) | não |
| Capacidade de vagas | editar | ler | ler | ler |
| Visitas domiciliares (RF004) | total | total | agendar e alterar | não |
| Escuta e PIA (RF005, RF006) | total e aprovar PIA | elaborar e editar | não | não |
| Usuários atendidos e desligamentos (RF007, RF008) | total | ler e editar | dados básicos | dados básicos |
| Crença religiosa e dados de saúde | ler e editar | ler e editar | não (?) | não |
| Violações de direitos (RF009) | total | registrar e ler | não | não |
| Atividades (RF010) | total | total | ler (?) | não |
| Contatos e comunicações (RF011) | total | total | total | não |
| Gestão institucional (RF012 a RF015) | total | ler (?) | registrar (?) | não |
| Relatórios e dashboards (RF016, RF017) | total | ler (?) | não (?) | ler |
| Importação de planilhas | sim | não | sim (?) | não |

"Dados básicos" significa nome, situação, turno e datas, sem contato, documentos ou campos sensíveis.
O conteúdo exato depende da resposta da equipe CESJO.

## 7. Aspectos transversais

- **Autenticação**: login por formulário, sessão no servidor, expiração por inatividade, bloqueio
  temporário após tentativas falhas, troca de senha obrigatória no primeiro acesso. A primeira conta de
  Coordenador é criada por variável de ambiente na implantação.
- **Log de auditoria (RNF 3.3)**: toda criação, alteração e exclusão; toda leitura de registro com dado
  sensível (escuta, PIA, violação, laudo); toda exportação; login e falha de login. O log guarda nomes
  de campos alterados, nunca os valores sensíveis. Só o Coordenador consulta.
- **Criptografia (RNF 3.3)**: conversor JPA com AES-256-GCM, chave mestra fornecida por segredo do
  ambiente, cada valor cifrado leva o identificador da chave para permitir rotação. Campos cifrados não
  são pesquisáveis; busca é feita por campos em claro (nome, datas, categorias). A chave tem cópia
  guardada fora da VPS, porque sem ela o backup é inútil.
- **Arquivos**: guardados cifrados num volume próprio, nunca no banco nem no repositório. Tipos aceitos
  PDF, JPG e PNG, conferidos pelo conteúdo e não só pela extensão. Limite inicial de 10 MB. Download
  sempre passa pela API, com checagem de permissão e registro de auditoria.
- **Exportação (seção 5.2)**: PDF gerado de HTML com templates Thymeleaf e openhtmltopdf, com logo e
  cores institucionais. Excel com Apache POI. Toda exportação é auditada.
- **Importação (seção 5.2)**: planilha Excel em modelo fixo, com pré-visualização, validação linha a
  linha e relatório de erros antes de gravar.
- **Notificações (RF003)**: notificações internas no sistema. Outros canais (e-mail, WhatsApp) só se a
  equipe pedir.
- **Desempenho (RNF 3.2)**: paginação em toda listagem, índices nas colunas de filtro, consultas de
  relatório em SQL agregado. Meta de 3 segundos verificada com massa de dados simulada.
- **Usabilidade (RNF 3.1)**: layout responsivo, testado em Chrome, Firefox, Edge e Safari no celular.
- **Operação (RNF 3.3 e 3.4)**: backup diário do banco e do volume de arquivos, cifrado e enviado para
  armazenamento fora da VPS, com retenção definida. Restauração testada periodicamente e procedimento
  de recuperação de desastres escrito. Monitor externo de disponibilidade com alerta. Meta de 99% de
  uptime.
- **LGPD**: o sistema guarda dados pessoais sensíveis (saúde, religião, violações). O repositório é
  público, então nenhum dado real entra em código, issues, testes ou capturas de tela.

## 8. Ordem de entrega

Cada marco termina com algo utilizável. A partir do marco M1 o sistema roda em produção, então
backup e segurança precisam estar prontos antes de entrar o primeiro dado real.

| Marco | Conteúdo | Requisitos |
|---|---|---|
| M0 Fundação | CI, esqueleto do frontend com tema, módulos do backend, login, perfis, contas, auditoria, criptografia, arquivos. | RNF 3.1, 3.3, seção 4 |
| M1 Lista de espera em produção | Cadastro de candidatos com anexos, fila, vagas e notificação, exportação da lista, importação da planilha atual, implantação na VPS com backup e monitoramento. | RF001 a RF003, RNF 3.3, 3.4, seção 5.2 |
| M2 Atendimento | Colaboradores básicos, visitas domiciliares, escuta, PIA com aprovação. | RF004 a RF006, RF014 (parcial) |
| M3 Usuários atendidos | Inserção no serviço, cadastro completo, desligamentos, violações de direitos. | RF007 a RF009 |
| M4 Atividades e comunicações | Atividades, atendimentos, estudos de caso, contatos, ofícios e encaminhamentos. | RF010, RF011 |
| M5 Gestão institucional | Reuniões e eventos, parcerias, vínculos de colaboradores, doações. | RF012 a RF015 |
| M6 Relatórios e dashboards | Dashboards, relatórios mensal, quadrimestral e anual, relatórios de intervenção e transparência, teste de desempenho. | RF016, RF017, RNF 3.2 |
| M7 Integrações | Levantamento e implementação das integrações externas que existirem de fato. | seção 5.1 |

## 9. Perguntas para a equipe CESJO

Nenhuma destas respostas foi inventada. Issues que dependem delas recebem o rótulo `aguardando-cesjo`.

1. **Prioridade na fila**: o documento diz que o serviço prioriza beneficiários do BPC inseridos no
   CadÚnico, mas o RF002 fala em ordem cronológica. Como a prioridade entra na fila? BPC e CadÚnico
   passam na frente, servem de desempate ou são só informação?
2. **Faixa etária**: o cadastro na lista de espera deve exigir data de nascimento e recusar quem está
   fora de 18 a 59 anos?
3. **Vagas**: a capacidade é um número total ou separada por turno (matutino, vespertino, integral)?
   Quem deve ser avisado de vaga aberta e por qual canal (só no sistema, e-mail, WhatsApp)?
4. **Saída da lista**: quais motivos de saída da lista de espera existem (desistência, não localizado,
   fora do perfil, inserido)? O candidato chamado que não comparece volta para a fila?
5. **PIA**: qual a estrutura do PIA (objetivos, metas, ações, prazos, responsáveis)? Existe modelo em
   papel? O PIA é revisado periodicamente, gerando nova versão?
6. **Dados pessoais completos (RF007)**: quais campos? CPF, NIS, RG, endereço completo, escolaridade,
   renda, benefícios, medicamentos?
7. **Cuidador familiar**: o cuidador responsável do RF007 é sempre o mesmo responsável do RF001? Pode
   haver mais de um?
8. **Acesso a dados sensíveis**: o perfil Administrativo pode ver laudos médicos e crença religiosa?
   O que são as "informações básicas" do perfil Consulta?
9. **Relatórios (RF016)**: há modelos atuais dos relatórios mensal, quadrimestral e anual? O que contém
   o relatório de intervenção (CREAS) e o relatório de transparência, e para quem é enviado cada um?
10. **Parcerias (RF013)**: o que precisa ser registrado sobre cada parceria (termo, vigência, contatos,
    ações realizadas)?
11. **Doações (RF015)**: as doações incluem dinheiro? É preciso emitir recibo ou prestar contas com
    valores?
12. **Integrações (seção 5.1)**: existe acesso formal (convênio, API, arquivo de exportação) ao
    CadÚnico, ao sistema do BPC e aos sistemas municipais de saúde e educação? Quem é o contato técnico?
13. **Planilhas (seção 5.2)**: a lista de espera e o cadastro de usuários existem hoje em Excel? Podem
    enviar um exemplo com dados fictícios?
14. **LGPD**: qual a base legal usada para tratar os dados (termo de consentimento, execução de
    política pública)? Quem é o encarregado de dados? Por quanto tempo os dados ficam guardados após o
    desligamento?
15. **Hospedagem**: quem paga e administra a VPS? Já existe domínio para o sistema?
16. **Identidade visual**: existe fonte institucional oficial?
17. **Pontos em aberto da matriz de permissões (seção 6)**: o Administrativo vê as atividades
    (RF010), registra a gestão institucional (RF012 a RF015) e importa planilhas? A Equipe Técnica lê a
    gestão institucional e os relatórios? O Administrativo vê relatórios?
18. **Funções dos colaboradores**: quais funções os colaboradores podem ter? Além de assistente
    social, psicólogo(a) e terapeuta ocupacional, há outras que precisam aparecer separadas nos
    relatórios (cuidador, monitor, motorista, coordenação, estagiário)?

Perguntas acrescentadas depois, com o número da pendência em [PENDENTE-CESJO](PENDENTE-CESJO.md):

40. **Volume esperado de uso (RNF 3.2)**: quantas pessoas o Centro-Dia atende ao mesmo tempo, quantas
    entram na lista de espera por ano, quantas atividades e contatos a equipe registra por mês e
    quantas pessoas usam o sistema ao mesmo tempo no pico?

## 10. Backlog

Rótulos: `epic`, um rótulo por área (`fundacao`, `operacao`, `lista-espera`, `atendimento`,
`atendidos`, `atividades`, `institucional`, `relatorios`, `integracoes`) e `aguardando-cesjo`.
Milestones: M0 a M7 da seção 8. Cada issue é uma fatia vertical (migração, API, tela e testes),
salvo quando o título diz o contrário. Critério comum a todas as issues de funcionalidade, sem repetir
em cada uma: permissões conforme a seção 6, operações registradas no log de auditoria, testes de
integração com Testcontainers e tela responsiva.

No GitHub, os épicos são as issues #1 (Fundação), #2 (Operação), #3 (Lista de espera), #4 (Atendimento),
#5 (Usuários atendidos), #6 (Atividades e atendimentos), #7 (Gestão institucional), #8 (Relatórios e
dashboards) e #9 (Integrações e importação). O item N desta lista é a issue #N+9 (o item 17 é a #26, por
exemplo). As referências entre issues nos critérios usam o número do GitHub.

### Épico 0 — Fundação (RNF 3.1, 3.3, seção 4)

1. **Configurar CI no GitHub Actions** (M0). Critérios: `./mvnw verify` roda com Testcontainers a cada
   push e PR; build, lint e testes do frontend rodam no mesmo pipeline; PR com falha fica bloqueado.
2. **Criar esqueleto do frontend Angular com tema institucional** (M0, RNF 3.1). Critérios: Angular 22
   e Angular Material 22 em `frontend/`; tema Material 3 com os tokens do guia de estilo; layout com barra
   superior, logo e menu lateral recolhível no celular; locale pt-BR; proxy de `/api` em desenvolvimento.
3. **Estruturar módulos e convenções do backend** (M0). Critérios: pacotes de módulo da seção 3; teste
   do Spring Modulith verificando fronteiras; erros em `ProblemDetail`; paginação padrão; springdoc
   desligado em produção.
4. **Implementar login por sessão** (M0, RNF 3.3). Critérios: tabela `conta_acesso` por migração;
   senha com Argon2; cookie HttpOnly e CSRF; logout; expiração por inatividade; bloqueio temporário
   após tentativas falhas; primeira conta de Coordenador criada por variável de ambiente; tela de login.
5. **Implementar perfis e permissões** (M0, RNF 3.3, seção 4). Critérios: quatro perfis mapeados para
   permissões no código conforme a seção 6; endpoints protegidos por permissão; menu e rotas do
   frontend filtrados; teste que garante 403 para perfil sem permissão.
6. **Gerenciar contas de acesso** (M0, seção 4.1). Critérios: Coordenador cria, edita perfil, desativa
   e redefine senha de contas; troca de senha obrigatória no primeiro acesso; a última conta ativa de
   Coordenador não pode ser desativada.
7. **Registrar e consultar log de auditoria** (M0, RNF 3.3). Critérios: registro de criação, alteração,
   exclusão, login e falha de login; mecanismo para registrar leitura de dado sensível e exportação;
   valores sensíveis nunca gravados no log; tela de consulta com filtros por período, conta e entidade,
   só para Coordenador.
8. **Cifrar campos sensíveis** (M0, RNF 3.3). Critérios: conversor JPA AES-256-GCM; chave mestra por
   segredo do ambiente; identificador de chave em cada valor; teste que confirma texto cifrado no banco
   e texto claro na entidade; aplicação não sobe sem chave configurada.
9. **Armazenar arquivos cifrados** (M0, RF001, RNF 3.3). Critérios: upload e download pela API;
   arquivo cifrado no volume com metadados e SHA-256 no banco; só PDF, JPG e PNG conferidos pelo
   conteúdo; limite de 10 MB; download auditado e sujeito a permissão.
10. **Exibir notificações internas** (M1, RF003). Critérios: notificação por perfil ou por conta;
    contador na barra superior; marcar como lida; API para outros módulos criarem notificações.

### Épico Operação (RNF 3.2, 3.3, 3.4)

11. **Empacotar e implantar na VPS com Docker Compose** (M1, RNF 3.4). Critérios: imagem da aplicação;
    compose de produção com Caddy (TLS automático, estáticos do Angular, proxy de `/api`), aplicação e
    Postgres; segredos fora do repositório; roteiro de implantação no README.
12. **Fazer backup diário automático** (M1, RNF 3.3). Critérios: dump diário do Postgres e cópia do
    volume de arquivos, cifrados e enviados para armazenamento fora da VPS; retenção configurada;
    alerta quando o backup falha.
13. **Documentar e testar recuperação de desastres** (M1, RNF 3.4). Critérios: procedimento escrito
    para restaurar o sistema numa VPS nova, incluindo a chave de criptografia; restauração executada uma
    vez com sucesso e tempo registrado.
14. **Monitorar disponibilidade** (M1, RNF 3.4). Critérios: monitor externo do endpoint de saúde com
    alerta; alerta de disco cheio; relatório mensal de uptime para acompanhar a meta de 99%.
15. **Validar navegadores e acessibilidade** (M1, RNF 3.1). Critérios: fluxos da lista de espera
    conferidos em Chrome, Firefox, Edge e Safari no celular; contraste WCAG AA nas telas principais;
    navegação por teclado nos formulários.
16. **Testar desempenho das consultas** (M6, RNF 3.2). Critérios: massa simulada de pelo menos 10 vezes
    o volume esperado; listagens, relatórios e dashboards respondem em até 3 segundos com 20 usuários
    simultâneos; resultado documentado.

### Épico 1 — Lista de espera (RF001 a RF003)

17. **Cadastrar candidato na lista de espera** (M1, RF001). Critérios: formulário com os dados
    obrigatórios do RF001 (exceto anexos): nome da pessoa, nome do responsável, tipo de deficiência,
    telefone, origem do encaminhamento, data de cadastro; validação no backend e no frontend; inscrição
    criada com situação "aguardando".
18. **Anexar laudo médico e comprovante de residência** (M1, RF001). Critérios: upload dos dois
    arquivos no cadastro e na edição; candidato sem os dois anexos aparece como cadastro incompleto;
    visualização e download conforme permissão.
19. **Listar a fila com posição** (M1, RF002). Critérios: listagem paginada em ordem cronológica com a
    posição de cada candidato; filtros por tipo de deficiência, origem e situação; busca por nome;
    regra de ordenação isolada para receber a prioridade BPC/CadÚnico se ela for definida (pergunta 1).
20. **Ver e editar candidato com histórico de situação** (M1, RF002). Critérios: ficha do candidato com
    dados, anexos e posição; edição de dados; mudança de situação com data e motivo; histórico das
    mudanças visível. Rótulo `aguardando-cesjo` para a lista de motivos de saída (pergunta 4).
21. **Configurar capacidade e mostrar vagas disponíveis** (M1, RF003). Critérios: Coordenador define a
    capacidade; vagas disponíveis = capacidade menos inserções ativas; painel com vagas e tamanho da
    fila. Rótulo `aguardando-cesjo` (pergunta 3, capacidade por turno).
22. **Notificar abertura de vaga** (M1, RF003). Critérios: quando as vagas disponíveis passam de zero
    para mais de zero, Coordenador e Administrativo recebem notificação com o próximo candidato da fila;
    gatilho por aumento de capacidade agora e por desligamento quando o épico 3 entrar.
23. **Exportar a lista de espera em PDF e Excel** (M1, seção 5.2). Critérios: exportação respeita os
    filtros da tela; PDF com identidade institucional; exportação auditada.
24. **Importar a lista de espera atual de planilha Excel** (M1, seção 5.2). Critérios: modelo de
    planilha documentado; pré-visualização com erros por linha; gravação só das linhas válidas após
    confirmação; data de cadastro original preservada para manter a ordem da fila. Rótulo
    `aguardando-cesjo` (pergunta 13).

### Épico 2 — Atendimento (RF004 a RF006)

25. **Agendar visita domiciliar** (M2, RF004). Critérios: agendamento para um candidato com data, hora e
    um ou mais profissionais (colaboradores); status inicial "agendada"; agenda das visitas por período;
    candidato passa a constar como em avaliação.
26. **Registrar realização ou cancelamento da visita** (M2, RF004). Critérios: mudança para "realizada"
    ou "cancelada" com motivo; observações cifradas; reagendamento cria nova visita.
27. **Registrar escuta ativa e qualificada** (M2, RF005). Critérios: registro com data, profissional
    responsável, dinâmica familiar, necessidades imediatas, habilidades e potencialidades; campos de
    texto cifrados; leitura auditada; sem acesso para Administrativo e Consulta.
28. **Elaborar e editar PIA** (M2, RF006). Critérios: criar PIA para a pessoa, editar enquanto
    "elaborado", versões anteriores preservadas; conteúdo cifrado. Rótulo `aguardando-cesjo` para a
    estrutura do PIA (pergunta 5); começa com seções de texto livre.
29. **Controlar status e aprovação do PIA** (M2, RF006, seção 4.1). Critérios: transições "apresentado
    à família", "apresentado à equipe" e "aprovado", cada uma com data; só Coordenador aprova; PIA
    aprovado não é mais editável, mudança gera nova versão; PIA imprimível em PDF para apresentação.

### Épico 3 — Usuários atendidos (RF007 a RF009)

30. **Inserir candidato no serviço** (M3, RF007, RF003). Critérios: a partir da fila, registra data de
    inserção, turno e cuidador familiar responsável; inscrição sai da fila como "inserida"; vagas
    disponíveis diminuem.
31. **Completar cadastro do usuário atendido** (M3, RF007). Critérios: dados pessoais completos, gênero,
    data de nascimento com faixa etária calculada (jovem 18 a 29, adulto 30 a 59), crença religiosa,
    bairro; crença visível só para perfis autorizados. Rótulo `aguardando-cesjo` (perguntas 6 e 7).
32. **Listar e consultar usuários atendidos** (M3, RF007). Critérios: listagem paginada com filtros por
    turno, tipo de deficiência, faixa etária, gênero e bairro; ficha do usuário reunindo inscrição,
    visitas, escutas e PIA; perfil Consulta vê só dados básicos.
33. **Registrar desligamento** (M3, RF008, RF003). Critérios: data, motivo (evasão, mudança de
    município, solicitação da família, alcance dos objetivos, outros) e observação cifrada; inserção
    passa a "desligada"; vaga liberada dispara a notificação da issue 22.
34. **Registrar violação de direitos** (M3, RF009). Critérios: as 12 categorias do RF009, âmbito intra
    ou extrafamiliar quando a categoria pede, data de identificação, descrição cifrada; acesso só para
    Coordenador e Equipe Técnica.
35. **Consultar violações de direitos** (M3, RF009). Critérios: lista por usuário e geral com filtro por
    categoria e período; toda leitura de descrição auditada.

### Épico 4 — Atividades e atendimentos (RF010, RF011)

36. **Registrar atividades com usuários e famílias** (M4, RF010). Critérios: tipos atividades com
    famílias, com usuários e envolvendo ambos; data, título, descrição, participantes e profissionais.
37. **Registrar atendimentos e estudos de caso** (M4, RF010). Critérios: atendimentos multiprofissionais,
    atendimentos técnicos individualizados e estudos de caso ligados a um usuário; texto cifrado;
    leitura auditada.
38. **Listar atividades por período** (M4, RF010). Critérios: filtros por tipo, período, profissional e
    usuário; atividades aparecem na ficha do usuário.
39. **Registrar contatos telefônicos e atendimentos ao público** (M4, RF011). Critérios: contatos com
    cuidadores familiares, contatos institucionais e atendimento a público não acompanhado; data,
    pessoa ou instituição, assunto.
40. **Registrar ofícios, declarações e encaminhamentos** (M4, RF011). Critérios: expedição e
    recebimento de ofícios e declarações; encaminhamentos enviados e recebidos por instituição; anexo
    opcional; listagem por instituição e período.

### Épico 5 — Gestão institucional (RF012 a RF015)

41. **Cadastrar colaboradores** (M2, RF014 parcial). Critérios: nome, função e situação; colaborador
    pode ser ligado a uma conta de acesso; usado como profissional em visitas, escutas e atividades.
42. **Registrar contratações e desligamentos de colaboradores** (M5, RF014). Critérios: histórico de
    vínculos com data de contratação, data e motivo de desligamento; colaborador desligado não aparece
    para novos agendamentos.
43. **Registrar reuniões e eventos institucionais** (M5, RF012). Critérios: os sete tipos do RF012;
    data, descrição, participantes; visitas e eventos com indicação de realizado, recebido ou
    participado; ligação opcional a parceiro.
44. **Gerenciar parcerias** (M5, RF013). Critérios: cadastro de parceiros e registro das ações de
    articulação com data e descrição. Rótulo `aguardando-cesjo` (pergunta 10).
45. **Registrar doações** (M5, RF015). Critérios: doações recebidas e realizadas com data, contraparte,
    tipo e descrição; listagem por período. Rótulo `aguardando-cesjo` (pergunta 11).

### Épico 6 — Relatórios e dashboards (RF016, RF017, seção 5.2)

46. **Criar base de exportação PDF e Excel** (M1, seção 5.2). Critérios: componente reutilizável que
    gera PDF a partir de template HTML com logo e cores institucionais e Excel a partir de uma tabela;
    toda exportação auditada. Pré-requisito da issue 23.
47. **Dashboard de perfil dos usuários atendidos** (M6, RF017). Critérios: usuários por tipo de
    deficiência, por gênero e faixa etária e por turno; dados atualizados a cada acesso; gráficos com
    as cores do tema.
48. **Dashboard de violações, atividades e desligamentos** (M6, RF017). Critérios: violações por
    categoria, atividades realizadas por período, taxa de desligamento por motivo; filtro de período;
    visível só para perfis autorizados.
49. **Gerar relatório estatístico mensal** (M6, RF016). Critérios: escolha do mês; consolida
    quantitativos de lista de espera, usuários, atendimentos, atividades e desligamentos; exporta PDF e
    Excel. Rótulo `aguardando-cesjo` (pergunta 9) para o conteúdo exato.
50. **Gerar relatórios quadrimestral e anual** (M6, RF016). Critérios: mesmo conteúdo do mensal
    agregado por quadrimestre e por ano; exporta PDF e Excel.
51. **Gerar relatório de intervenção (CREAS)** (M6, RF016). Critérios definidos após resposta da
    pergunta 9. Rótulo `aguardando-cesjo`.
52. **Gerar relatório de transparência** (M6, RF016). Critérios definidos após resposta da pergunta 9.
    Rótulo `aguardando-cesjo`.

### Épico 7 — Integrações e importação (seção 5)

53. **Importar usuários atendidos de planilha Excel** (M3, seção 5.2). Critérios: mesmo fluxo da issue
    24 (modelo, pré-visualização, erros por linha); cria pessoa e inserção ativa. Rótulo
    `aguardando-cesjo` (pergunta 13).
54. **Levantar integrações com CadÚnico, BPC e sistemas municipais** (M7, seção 5.1). Critérios:
    documento com o meio de acesso disponível para cada sistema (API, arquivo, nenhum), requisitos
    legais e contato técnico; proposta de issues de implementação. Rótulo `aguardando-cesjo`
    (pergunta 12). Nenhuma integração é implementada antes disso.
