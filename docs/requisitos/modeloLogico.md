# Modelo Lógico do Sistema de Gestão do Centro-Dia

## 1. Objetivo e estado

Este modelo representa os conceitos necessários ao núcleo prioritário do sistema e organiza a expansão dos demais módulos descritos nos RF001 a RF017.

O modelo foi reconciliado com:

- [documento de requisitos](requisitos.md);
- [DER editável](DER.md);
- [modelo físico PostgreSQL](modeloFisico.md);
- [decisões do projeto](decisoesProjeto.md);
- descrição geral do SGI.

Estado atual:

- núcleo de beneficiário e lista de espera: proposto para implementação após validação das pendências de negócio;
- demais módulos: estrutura conceitual, ainda sem autorização para migrations definitivas.

## 2. Terminologia

### 2.1 Conta de acesso

Profissional autorizado a autenticar-se e operar o sistema.

Não representa a pessoa atendida pelo Centro-Dia.

### 2.2 Beneficiário

Pessoa com deficiência que pode estar:

- cadastrada;
- aguardando na lista de espera;
- em avaliação;
- vinculada ao serviço;
- desligada do serviço.

O beneficiário mantém a mesma identidade durante todo o fluxo.

### 2.3 Vínculo com o serviço

Período em que um beneficiário está efetivamente inserido no Centro-Dia. Substitui a criação de uma segunda pessoa na antiga tabela `usuarios`.

## 3. Núcleo do modelo

### 3.1 Conta de Acesso (`ContaAcesso`)

#### Finalidade

Representar quem utiliza administrativamente o sistema.

#### Atributos

| Campo | Descrição | Regra |
|---|---|---|
| `id_conta` | Identificador | PK |
| `nome` | Nome do profissional | Obrigatório |
| `email` | Identificador de autenticação | Obrigatório e único |
| `senha_hash` | Hash da credencial | Obrigatório; nunca armazenar senha em texto puro |
| `perfil` | Perfil de acesso | Obrigatório |

Valores documentados para `perfil`:

- `COORDENADOR`;
- `EQUIPE_TECNICA`;
- `ADMINISTRATIVO`;
- `CONSULTA`.

As permissões detalhadas por operação ainda precisam de uma matriz de autorização.

### 3.2 Beneficiário (`Beneficiario`)

#### Finalidade

Representar a pessoa candidata ou atendida pelo Centro-Dia.

#### Atributos do núcleo

| Campo | Descrição | Regra atual |
|---|---|---|
| `id_beneficiario` | Identificador | PK |
| `nome` | Nome completo | Obrigatório |
| `data_nascimento` | Data de nascimento | Obrigatória para validar a faixa de 18 a 59 anos |
| `cpf` | CPF | Opcional e sem unicidade até validação do negócio |
| `responsavel` | Nome completo do responsável | Obrigatório conforme RF001 |
| `telefone` | Telefone de contato | Obrigatório conforme RF001 |
| `tipo_deficiencia` | Tipo de deficiência | Obrigatório |
| `beneficiario_bpc` | Indica recebimento do BPC | Obrigatório, booleano |
| `inscrito_cadunico` | Indica inscrição no CadÚnico | Obrigatório, booleano |
| `origem_encaminhamento` | Origem do encaminhamento | Obrigatório |
| `laudo_medico_referencia` | Referência segura ao arquivo | Obrigatório conforme RF001 |
| `comprovante_residencia_referencia` | Referência segura ao arquivo | Obrigatório conforme RF001 |
| `id_conta_cadastro` | Conta responsável pelo cadastro | FK obrigatória para `ContaAcesso` |
| `data_criacao` | Data e hora de criação | Obrigatória |
| `data_atualizacao` | Data e hora da última atualização | Obrigatória |

Valores documentados para `tipo_deficiencia`:

- `SENSORIAL`;
- `FISICA`;
- `TEA`;
- `INTELECTUAL`;
- `MULTIPLA`;
- `T21`.

Valores documentados para `origem_encaminhamento`:

- `CRAS`;
- `CREAS`;
- `JUDICIARIO`;
- `SAUDE`;
- `EDUCACAO`;
- `DEMANDA_ESPONTANEA`.

O requisito usa tipo de deficiência no singular. A possibilidade de registrar mais de um tipo simultaneamente ainda precisa ser confirmada antes de qualquer normalização.

### 3.3 Lista de Espera (`ListaEspera`)

#### Finalidade

Representar cada entrada de um beneficiário na lista de espera.

#### Atributos

| Campo | Descrição | Regra atual |
|---|---|---|
| `id_lista_espera` | Identificador da entrada | PK |
| `id_beneficiario` | Beneficiário da entrada | FK obrigatória |
| `data_entrada` | Data de ingresso na fila | Obrigatória |
| `data_saida` | Data de encerramento da entrada | Nula enquanto ativa |
| `motivo_saida` | Motivo do encerramento | Opcional até definição do fluxo |
| `observacao` | Observação administrativa | Opcional |
| `id_conta_cadastro` | Conta que registrou a entrada | FK obrigatória |
| `data_criacao` | Data e hora do registro | Obrigatória |

#### Regras

1. Um beneficiário pode possuir várias entradas históricas.
2. Um beneficiário não pode possuir duas entradas ativas ao mesmo tempo.
3. Uma entrada é considerada ativa quando `data_saida` é nula.
4. A posição não é persistida; ela é calculada.
5. A ordenação cronológica usa `data_entrada` e, em empate técnico, `id_lista_espera`.
6. A aplicação da prioridade BPC + CadÚnico continua pendente de validação do negócio.

Não foi criado campo `status` porque o documento de requisitos não define estados e transições da fila. O campo poderá ser adicionado quando o fluxo de convocação, admissão e saída estiver validado.

### 3.4 Vaga (`Vaga`)

O RF003 exige controle de vagas e notificação de abertura, mas ainda não define:

- se existe uma capacidade global ou por turno;
- como uma vaga é ocupada ou liberada;
- se há reserva;
- quem recebe a notificação;
- qual canal de notificação é usado.

Por isso, `Vaga` permanece como conceito pendente e não integra o modelo físico executável.

## 4. Atendimento e avaliação

### 4.1 Visita Domiciliar (`VisitaDomiciliar`)

| Campo inicial | Descrição |
|---|---|
| `id_visita` | PK |
| `id_beneficiario` | FK para `Beneficiario` |
| `data_hora` | Data e hora agendadas |
| `status` | `AGENDADA`, `REALIZADA` ou `CANCELADA` |
| `id_conta_registro` | Conta que realizou o registro |

A associação de vários profissionais à visita ainda precisa ser definida.

### 4.2 Escuta (`Escuta`)

| Campo inicial | Descrição |
|---|---|
| `id_escuta` | PK |
| `id_beneficiario` | FK para `Beneficiario` |
| `dinamica_familiar` | Texto da escuta |
| `necessidades` | Necessidades imediatas |
| `habilidades` | Habilidades identificadas |
| `potencialidades` | Potencialidades identificadas |
| `data_escuta` | Data da escuta |
| `id_conta_responsavel` | Profissional responsável |

### 4.3 Plano de Atendimento (`PIA`)

| Campo inicial | Descrição |
|---|---|
| `id_pia` | PK |
| `id_beneficiario` | FK para `Beneficiario` |
| `descricao` | Conteúdo do plano |
| `status` | Estado documentado do PIA |
| `id_conta_elaboracao` | Conta responsável pela elaboração |

Valores documentados para `status`:

- `ELABORADO`;
- `APRESENTADO_FAMILIA`;
- `APRESENTADO_EQUIPE`;
- `APROVADO`.

Antes do modelo físico, ainda precisam ser definidos versionamento, datas das apresentações, responsável por cada transição e regra de aprovação.

## 5. Inserção no serviço

### 5.1 Vínculo com o Serviço (`VinculoServico`)

#### Finalidade

Representar a inserção do beneficiário no atendimento regular, sem duplicar seus dados pessoais.

#### Atributos iniciais

| Campo | Descrição |
|---|---|
| `id_vinculo` | PK |
| `id_beneficiario` | FK para `Beneficiario` |
| `genero` | Gênero informado |
| `crenca` | Crença religiosa informada |
| `bairro` | Bairro de moradia |
| `turno` | `MATUTINO`, `VESPERTINO` ou `INTEGRAL` |
| `cuidador` | Cuidador familiar responsável |
| `data_insercao` | Data de inserção no serviço |

A faixa etária é calculada a partir de `Beneficiario.data_nascimento`:

- 18 a 29 anos: jovem;
- 30 a 59 anos: adulto.

### 5.2 Desligamento (`Desligamento`)

| Campo | Descrição |
|---|---|
| `id_desligamento` | PK |
| `id_vinculo` | FK para `VinculoServico` |
| `motivo` | Motivo documentado |
| `data_desligamento` | Data do desligamento |
| `observacao` | Complemento quando necessário |

Motivos documentados:

- `EVASAO`;
- `MUDANCA_MUNICIPIO`;
- `SOLICITACAO_FAMILIA`;
- `ALCANCE_OBJETIVOS`;
- `OUTROS`.

### 5.3 Violação de Direito (`ViolacaoDireito`)

Representa uma violação identificada durante um vínculo com o serviço.

O modelo deve suportar os tipos definidos no RF009, descrição complementar, data do registro e conta responsável. Sigilo e permissões específicas precisam ser definidos antes da implementação.

## 6. Atividades e comunicações

### 6.1 Atividade (`Atividade`)

Representa os registros do RF010:

- atividade com famílias;
- atividade com beneficiários;
- atividade com beneficiários e famílias;
- atendimento multiprofissional;
- atendimento técnico individualizado;
- estudo de caso.

O modelo de participantes não está fechado. Uma FK opcional para um único beneficiário, como existia no SQL histórico, não é suficiente para atividades coletivas.

### 6.2 Comunicação (`Comunicacao`)

Representa contatos, ofícios, declarações e encaminhamentos do RF011.

Antes do modelo físico, precisam ser definidos remetente, destinatário, direção, instituição, beneficiário relacionado e anexos.

## 7. Gestão institucional

Os seguintes conceitos estão previstos, mas dependem de detalhamento antes do modelo físico:

| Conceito | Requisito | Pendência central |
|---|---|---|
| `ReuniaoEvento` | RF012 | Participantes, local, duração e resultado |
| `Parceria` | RF013 | Dados obrigatórios, estado e operações |
| `Colaborador` | RF014 | Cadastro, cargo, vínculo e desligamento |
| `Doacao` | RF015 | Origem, destino, itens, quantidade, valor e comprovantes |

## 8. Relatórios e dashboards

RF016 e RF017 são projeções dos dados operacionais. Não devem gerar tabelas próprias sem necessidade comprovada.

Os indicadores documentados são:

- beneficiários por tipo de deficiência;
- beneficiários por gênero e faixa etária;
- beneficiários por turno;
- violações por categoria;
- atividades por período;
- taxa de desligamento por motivo.

As fórmulas, filtros e períodos precisam ser definidos na especificação de cada relatório.

## 9. Relacionamentos consolidados

| Origem | Cardinalidade | Destino |
|---|---|---|
| `ContaAcesso` | 1:N | `Beneficiario` |
| `ContaAcesso` | 1:N | `ListaEspera` |
| `Beneficiario` | 1:N | `ListaEspera` |
| `Beneficiario` | 1:N | `VisitaDomiciliar` |
| `Beneficiario` | 1:N | `Escuta` |
| `Beneficiario` | 1:N | `PIA` |
| `Beneficiario` | 1:N | `VinculoServico` |
| `VinculoServico` | 1:N | `Desligamento` |
| `VinculoServico` | 1:N | `ViolacaoDireito` |

## 10. Restrições mínimas

- E-mail de conta de acesso único.
- Senha armazenada somente como hash.
- Data de nascimento necessária para validar a faixa etária.
- Apenas uma entrada ativa na fila por beneficiário.
- `data_saida` da fila igual ou posterior a `data_entrada`.
- Posição calculada, nunca armazenada.
- Chaves estrangeiras obrigatórias para autoria dos registros sensíveis.
- Nenhuma exclusão física de histórico deve ser implementada sem política de retenção e proteção de dados.

## 11. Pendências que bloqueiam partes da implementação

Consulte a [matriz de rastreabilidade](matrizRastreabilidade.md) para a lista completa.

As pendências prioritárias são:

1. validar a regra combinada de prioridade e cronologia;
2. definir o ciclo de convocação e saída da fila;
3. definir a política dos arquivos obrigatórios;
4. detalhar vagas e notificações;
5. definir "dados pessoais completos";
6. definir permissões detalhadas e auditoria;
7. detalhar relatórios e integrações.

## 12. Limite de implementação autorizado pelo modelo

O [modelo físico](modeloFisico.md) fornece uma baseline somente para:

- contas de acesso;
- beneficiários;
- entradas na lista de espera;
- ordenação cronológica;
- histórico sem duplicidade de entrada ativa.

Os demais conceitos deste documento não devem virar migrations definitivas antes da resolução das pendências correspondentes.

