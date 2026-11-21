# Pendências com a equipe CESJO

Perguntas que a equipe CESJO ainda não respondeu (seção 9 do [planejamento](planejamento.md)) e o que
o sistema faz enquanto isso. Cada decisão provisória segue o [ADR 0006](adr/0006-pendencias-cesjo.md):
fica num lugar só do código, com o marcador `PENDENTE-CESJO(Pn)`, e tem um ADR com o motivo.

Quando uma resposta chegar:

1. Anote a resposta na pergunta, abaixo.
2. Mude o ponto do código indicado em "Onde mudar" (`git grep -n "PENDENTE-CESJO(Pn)"` acha todos).
3. Escreva um ADR novo que substitui o provisório e marque a pergunta como respondida.

Situação: **aberta** (sem resposta, nada depende dela ainda), **provisória** (o sistema já assume
algo) ou **respondida**.

---

## P1. Prioridade na fila

> O documento diz que o serviço prioriza beneficiários do BPC inseridos no CadÚnico, mas o RF002 fala
> em ordem cronológica. Como a prioridade entra na fila? BPC e CadÚnico passam na frente, servem de
> desempate ou são só informação?

Situação: provisória.

| Decisão provisória | Onde mudar | ADR |
|---|---|---|
| A fila é só cronológica: data de cadastro, com o momento do cadastro como desempate. Não há prioridade para BPC ou CadÚnico. | `listaespera.interno.OrdemDaFila` | [0057](adr/0057-ordem-e-posicao-na-fila.md) |

## P2. Faixa etária

> O cadastro na lista de espera deve exigir data de nascimento e recusar quem está fora de 18 a 59
> anos?

Situação: provisória.

| Decisão provisória | Onde mudar | ADR |
|---|---|---|
| Data de nascimento é opcional no cadastro da lista de espera e não há bloqueio por idade. Quando informada, não pode ser futura. | `NovaInscricao` (backend, `listaespera.interno`) e `formulario-inscricao.ts` (frontend) | [0038](adr/0038-dados-do-cadastro-na-lista-de-espera.md) |

## P3. Vagas

> A capacidade é um número total ou separada por turno (matutino, vespertino, integral)? Quem deve ser
> avisado de vaga aberta e por qual canal (só no sistema, e-mail, WhatsApp)?

Situação: aberta.

## P4. Saída da lista

> Quais motivos de saída da lista de espera existem (desistência, não localizado, fora do perfil,
> inserido)? O candidato chamado que não comparece volta para a fila?

Situação: provisória. A inscrição tem as situações `AGUARDANDO`, `EM_AVALIACAO` e `INSERIDO` (saída
por inserção no serviço, ADR 0081); as outras situações de saída entram com a issue #29 (marcador em
`listaespera.SituacaoInscricao`).

| Decisão provisória | Onde mudar | ADR |
|---|---|---|
| O candidato com visita agendada fica `EM_AVALIACAO`, continua na fila na mesma ordem e não volta para `AGUARDANDO` quando a visita é cancelada. | `listaespera.SituacaoInscricao` (`ATIVAS`) e `listaespera.interno.ServicoCandidatos` | [0049](adr/0049-candidato-em-avaliacao-e-api-da-lista-de-espera.md) |

## P5. PIA

> Qual a estrutura do PIA (objetivos, metas, ações, prazos, responsáveis)? Existe modelo em papel? O
> PIA é revisado periodicamente, gerando nova versão?

Situação: aberta.

## P6. Dados pessoais completos (RF007)

> Quais campos? CPF, NIS, RG, endereço completo, escolaridade, renda, benefícios, medicamentos?

Situação: aberta.

## P7. Cuidador familiar

> O cuidador responsável do RF007 é sempre o mesmo responsável do RF001? Pode haver mais de um?

Situação: provisória.

| Decisão provisória | Onde mudar | ADR |
|---|---|---|
| Um cuidador familiar por inserção, escolhido entre os responsáveis da pessoa (o do RF001 vem marcado) ou cadastrado na hora como novo responsável. | `NovaInsercao` (backend, `atendidos.interno`) e `formulario-insercao.ts` (frontend) | [0080](adr/0080-insercao-no-servico.md) |

## P8. Acesso a dados sensíveis

> O perfil Administrativo pode ver laudos médicos e crença religiosa? O que são as "informações
> básicas" do perfil Consulta?

Situação: provisória.

| Decisão provisória | Onde mudar | ADR |
|---|---|---|
| O Administrativo envia e lê laudos e comprovantes. | `acesso.Perfil`, linha do `ADMINISTRATIVO` | [0015](adr/0015-matriz-de-perfis-da-fundacao.md) |
| O Administrativo não vê crença religiosa nem dados de saúde (aplicado quando esses campos entrarem). | `acesso.Perfil` | [0015](adr/0015-matriz-de-perfis-da-fundacao.md) |
| "Dados básicos" do Consulta são só nome, situação, turno e datas (aplicado quando as listagens entrarem). | `acesso.Perfil` | [0015](adr/0015-matriz-de-perfis-da-fundacao.md) |
| Na fila da lista de espera, o Consulta vê posição, nome, situação e data de cadastro, sem tipo de deficiência, origem nem situação dos documentos, e não filtra por tipo nem origem (`LISTA_ESPERA_FILA_LER`). | `acesso.Perfil` (linha do `CONSULTA`), `ItemFila.basico()` e `ListaEsperaControlador.listar` | [0058](adr/0058-dados-basicos-da-fila-para-o-consulta.md) |
| Administrativo e Consulta veem só os dados básicos dos usuários atendidos (`ATENDIDOS_LER_BASICO`). | `acesso.Perfil` | [0082](adr/0082-permissoes-dos-usuarios-atendidos.md) |

## P9. Relatórios (RF016)

> Há modelos atuais dos relatórios mensal, quadrimestral e anual? O que contém o relatório de
> intervenção (CREAS) e o relatório de transparência, e para quem é enviado cada um?

Situação: aberta.

## P10. Parcerias (RF013)

> O que precisa ser registrado sobre cada parceria (termo, vigência, contatos, ações realizadas)?

Situação: aberta.

## P11. Doações (RF015)

> As doações incluem dinheiro? É preciso emitir recibo ou prestar contas com valores?

Situação: aberta.

## P12. Integrações (seção 5.1)

> Existe acesso formal (convênio, API, arquivo de exportação) ao CadÚnico, ao sistema do BPC e aos
> sistemas municipais de saúde e educação? Quem é o contato técnico?

Situação: aberta.

## P13. Planilhas (seção 5.2)

> A lista de espera e o cadastro de usuários existem hoje em Excel? Podem enviar um exemplo com dados
> fictícios?

Situação: aberta.

## P14. LGPD

> Qual a base legal usada para tratar os dados (termo de consentimento, execução de política
> pública)? Quem é o encarregado de dados? Por quanto tempo os dados ficam guardados após o
> desligamento?

Situação: provisória.

| Decisão provisória | Onde mudar | ADR |
|---|---|---|
| O log de auditoria é guardado para sempre, sem expurgo. | `V1__cria_auditoria.sql` (gatilhos) | [0022](adr/0022-log-de-auditoria-so-de-insercao.md) |
| Backups mantidos por 7 diários, 4 semanais e 6 mensais (cerca de seis meses). | padrões em `deploy/backup/backup.sh`; `SGI_BACKUP_MANTER_*` no `deploy/backup.env` | [0065](adr/0065-retencao-dos-backups.md) |
| Laudo ou comprovante substituído continua guardado no módulo Arquivos, sem ligação com a pessoa. | `pessoas.interno.ServicoPessoas.anexarDocumento` | [0055](adr/0055-documentos-da-pessoa.md) |

## P15. Hospedagem

> Quem paga e administra a VPS? Já existe domínio para o sistema?

Situação: provisória.

| Decisão provisória | Onde mudar | ADR |
|---|---|---|
| Uma VPS com Docker Compose (Caddy, aplicação e Postgres). | implantação (issue #20) | [0005](adr/0005-hospedagem-vps-docker-compose.md) |
| Domínio, e-mail do Let's Encrypt e credenciais parametrizados; imagens construídas na própria VPS. | `deploy/.env` (a partir de `deploy/.env.example`), `deploy/compose.yaml` | [0050](adr/0050-empacotamento-e-compose-de-producao.md) |
| Destino do backup é qualquer repositório restic fora da VPS, escolhido na implantação. | `RESTIC_REPOSITORY` e credenciais no `deploy/backup.env` | [0054](adr/0054-backup-diario-com-restic.md) |

## P16. Identidade visual

> Existe fonte institucional oficial?

Situação: provisória.

| Decisão provisória | Onde mudar | ADR |
|---|---|---|
| Fonte Atkinson Hyperlegible Next em todo o sistema. | `$font-family` em `frontend/src/styles/_tokens.scss` | [0036](adr/0036-fonte-atkinson-hyperlegible.md) |

## P17. Pontos em aberto da matriz de permissões (seção 6)

> O Administrativo vê as atividades (RF010), registra a gestão institucional (RF012 a RF015) e
> importa planilhas? A Equipe Técnica lê a gestão institucional e os relatórios? O Administrativo vê
> relatórios?

Situação: provisória.

| Decisão provisória | Onde mudar | ADR |
|---|---|---|
| Nos pontos com (?), o perfil em dúvida fica sem acesso: Administrativo sem atividades, gestão institucional, relatórios e importação; Equipe Técnica sem gestão institucional e relatórios. | `acesso.Perfil` | [0015](adr/0015-matriz-de-perfis-da-fundacao.md) |
| Só Coordenador e Administrativo cadastram candidatos na lista de espera; a Equipe Técnica lê e, quando a edição entrar, edita. | `acesso.Perfil` (`LISTA_ESPERA_CADASTRAR`) | [0039](adr/0039-permissoes-da-lista-de-espera.md) |
| Só o Coordenador insere candidatos no serviço; a Equipe Técnica lê a ficha completa dos usuários atendidos. | `acesso.Perfil` (`ATENDIDOS_INSERIR`) | [0082](adr/0082-permissoes-dos-usuarios-atendidos.md) |
| Equipe Técnica e Administrativo não veem nem gerenciam o cadastro de colaboradores; escolhem o profissional pelas telas de visita, escuta e atividade. | `acesso.Perfil` (`COLABORADORES_LER`, `COLABORADORES_GERENCIAR`) | [0047](adr/0047-permissoes-dos-colaboradores.md) |

## P18. Funções dos colaboradores

> Quais funções os colaboradores do Centro-Dia podem ter? Além de assistente social, psicólogo(a) e
> terapeuta ocupacional, há outras que precisam aparecer separadas nos relatórios (cuidador,
> monitor, motorista, coordenação, estagiário)?

Situação: provisória.

| Decisão provisória | Onde mudar | ADR |
|---|---|---|
| Funções `ASSISTENTE_SOCIAL`, `PSICOLOGO`, `TERAPEUTA_OCUPACIONAL` e `OUTRA`, tiradas do planejamento. | `institucional.FuncaoColaborador`, `ck_colaborador_funcao` (migração nova) e `rotulos.ts` do módulo colaboradores (frontend) | [0045](adr/0045-cadastro-e-api-java-dos-colaboradores.md) |
