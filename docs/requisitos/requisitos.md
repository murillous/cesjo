# Documento de Requisitos do Sistema CESJO

> Versão Markdown consolidada do documento de requisitos do sistema.
> O conteúdo funcional foi preservado; a formatação e a terminologia foram normalizadas para facilitar versionamento e rastreabilidade.

## 1. Visão geral do projeto

### 1.1 Descrição do sistema

Sistema de gestão de informações para o Centro-Dia que atende pessoas com deficiência física, sensorial, intelectual, múltipla, Transtorno do Espectro Autista (TEA) e Trissomia 21 (T21), na faixa etária de 18 a 59 anos, priorizando beneficiários do Benefício de Prestação Continuada (BPC) inscritos no Cadastro Único (CadÚnico).

### 1.2 Objetivos

- Organizar e gerenciar a lista de espera do serviço.
- Sistematizar informações para relatórios mensais, quadrimestrais e anuais.
- Controlar o fluxo de atendimento desde o cadastro até o acompanhamento.
- Facilitar a elaboração de relatórios estatísticos e gerenciais.

## 2. Requisitos funcionais

### 2.1 Módulo de Gestão da Lista de Espera

#### RF001 - Cadastro de candidatos

O sistema deve permitir o cadastro de pessoas com deficiência na lista de espera.

Dados obrigatórios:

- Nome completo da pessoa com deficiência.
- Nome completo do responsável.
- Tipo de deficiência: sensorial, física, TEA, intelectual, múltipla ou T21.
- Laudo médico, por upload de arquivo.
- Comprovante de residência em São Luís, por upload de arquivo.
- Telefone de contato.
- Origem do encaminhamento: CRAS, CREAS, Judiciário, Saúde, Educação ou demanda espontânea.
- Data de cadastro.

#### RF002 - Controle de posição na lista

O sistema deve manter ordem cronológica de cadastro e permitir a visualização da posição na fila.

#### RF003 - Gestão de vagas disponíveis

O sistema deve controlar o número de vagas disponíveis e notificar quando houver abertura.

### 2.2 Módulo de Atendimento

#### RF004 - Agendamento de visita domiciliar

O sistema deve permitir agendar visitas domiciliares para avaliação de perfil.

Dados:

- Data e hora da visita.
- Profissionais responsáveis: assistente social, psicóloga e terapeuta ocupacional.
- Status da visita: agendada, realizada ou cancelada.

#### RF005 - Registro de escuta ativa e qualificada

O sistema deve permitir registrar informações coletadas durante a escuta.

Dados:

- Dinâmica familiar.
- Necessidades imediatas.
- Habilidades identificadas.
- Potencialidades.
- Data da escuta.
- Profissional responsável.

#### RF006 - Gestão do PIA

O sistema deve permitir criar, editar e gerenciar Planos de Atendimento Individual e Familiar (PIAs).

Funcionalidades:

- Criar novo PIA.
- Editar PIA existente.
- Apresentar PIA para a família.
- Apresentar PIA para a equipe de cuidadores.
- Controlar o status: elaborado, apresentado à família, apresentado à equipe ou aprovado.

### 2.3 Módulo de Usuários Atendidos

Neste documento, "usuário atendido" significa a pessoa inserida no serviço. Para evitar confusão com quem acessa o sistema, os demais documentos utilizam o termo canônico **beneficiário** para a pessoa atendida e **conta de acesso** para o profissional autorizado.

#### RF007 - Cadastro de usuários atendidos

O sistema deve permitir cadastrar usuários inseridos no serviço.

Dados:

- Dados pessoais completos.
- Gênero e faixa etária: 18 a 29 anos, jovem; 30 a 59 anos, adulto.
- Tipo de deficiência.
- Crença religiosa: católica, evangélica, testemunha de Jeová, matriz africana ou outras.
- Bairro de moradia.
- Turno de atendimento: matutino, vespertino ou integral.
- Cuidador familiar responsável.
- Data de inserção.

#### RF008 - Controle de desligamentos

O sistema deve registrar desligamentos com os seguintes motivos:

- Evasão.
- Mudança para outro município.
- Solicitação da família.
- Alcance dos objetivos.
- Outros.

#### RF009 - Registro de violações de direitos

O sistema deve categorizar e registrar violações identificadas.

Tipos de violação:

- Exploração de imagem.
- Isolamento social.
- Confinamento.
- Atitudes discriminatórias e preconceituosas no seio familiar.
- Falta de cuidados adequados por parte do cuidador familiar.
- Alto grau de estresse do cuidador familiar.
- Desvalorização da potencialidade ou capacidade da pessoa.
- Violência patrimonial.
- Negligência ou abandono.
- Abuso sexual intrafamiliar ou extrafamiliar.
- Violência física ou psicológica intrafamiliar ou extrafamiliar.
- Outras violações.

### 2.4 Módulo de Atividades e Atendimentos

#### RF010 - Registro de atividades

O sistema deve permitir registrar os seguintes tipos de atividade:

- Atividades com famílias.
- Atividades com usuários.
- Atividades envolvendo usuários e famílias.
- Atendimentos multiprofissionais.
- Atendimentos técnicos individualizados.
- Estudos de caso.

#### RF011 - Controle de contatos e comunicações

O sistema deve registrar os seguintes tipos de comunicação:

- Contatos telefônicos com cuidadores familiares.
- Contatos telefônicos institucionais.
- Atendimento a público não acompanhado.
- Expedição ou recebimento de ofícios e declarações.
- Encaminhamentos enviados ou recebidos, por instituição.

### 2.5 Módulo de Gestão Institucional

#### RF012 - Registro de reuniões e eventos

O sistema deve registrar os seguintes tipos de atividade institucional:

- Reuniões entre coordenação e equipe técnica.
- Formações multiprofissionais.
- Reuniões com a direção geral.
- Monitoramento do órgão gestor.
- Visitas institucionais realizadas ou recebidas.
- Eventos realizados ou participados.
- Reuniões com parceiros.

#### RF013 - Gestão de parcerias

O sistema deve controlar a articulação e o fortalecimento de parcerias.

#### RF014 - Controle de colaboradores

O sistema deve registrar contratações e desligamentos de colaboradores.

#### RF015 - Gestão de doações

O sistema deve registrar doações realizadas e recebidas.

### 2.6 Módulo de Relatórios

#### RF016 - Relatórios estatísticos

O sistema deve gerar relatórios quantitativos:

- Mensal.
- Quadrimestral.
- Anual.
- De intervenção para o CREAS.
- De transparência.

#### RF017 - Dashboards analíticos

O sistema deve apresentar os seguintes indicadores em tempo real:

- Usuários por tipo de deficiência.
- Usuários por gênero e faixa etária.
- Usuários por turno de atendimento.
- Violações de direitos por categoria.
- Atividades realizadas por período.
- Taxa de desligamento por motivo.

## 3. Requisitos não funcionais

### 3.1 Usabilidade

- Interface intuitiva e de fácil navegação.
- Compatibilidade com diferentes navegadores.
- Design responsivo para acesso móvel.

### 3.2 Desempenho

- Tempo de resposta máximo de 3 segundos para consultas.
- Suporte a múltiplos usuários simultâneos.

### 3.3 Segurança

- Controle de acesso por perfil.
- Criptografia de dados sensíveis.
- Log de auditoria das operações.
- Backup automático diário.

### 3.4 Disponibilidade

- Sistema disponível 24 horas por dia, sete dias por semana, com 99% de disponibilidade.
- Procedimentos de recuperação de desastres.

## 4. Perfis de acesso

### 4.1 Coordenador

- Acesso total ao sistema.
- Aprovação de PIAs.
- Geração de relatórios gerenciais.

### 4.2 Equipe técnica

- Cadastro e edição de informações.
- Registro de atividades e atendimentos.
- Elaboração de PIAs.

### 4.3 Administrativo

- Gestão da lista de espera.
- Agendamento de visitas.
- Controle de contatos.

### 4.4 Consulta

- Visualização de relatórios.
- Consulta de informações básicas.

## 5. Integrações

### 5.1 Sistemas externos

- CadÚnico, para consulta de dados.
- Sistema de gestão do BPC.
- Sistemas municipais de saúde e educação.

### 5.2 Importação e exportação

- Importação de dados de planilhas Excel.
- Exportação de relatórios em PDF e Excel.

## 6. Pontos que exigem detalhamento antes da implementação correspondente

Estes itens não alteram os requisitos originais. Eles registram informações que o documento fonte não define:

- Regra de combinação entre a prioridade BPC + CadÚnico e a ordem cronológica do RF002.
- Critérios de desempate e regras de retorno à lista de espera.
- Fluxo de convocação, admissão e saída da lista.
- Escopo das vagas e canal das notificações do RF003.
- Tipos, tamanho, armazenamento, acesso e retenção dos arquivos do RF001.
- Conteúdo de "dados pessoais completos" do RF007.
- Regras de versão, apresentação e aprovação do PIA.
- Campos e critérios de cálculo dos relatórios e indicadores.
- Contratos, disponibilidade e autenticação das integrações externas.
- Volume de usuários simultâneos usado para validar desempenho.
- Matriz detalhada de permissões por operação e dado.
- Políticas de auditoria, backup, restauração, retenção e recuperação de desastre.

Consulte a [matriz de rastreabilidade](matrizRastreabilidade.md) para verificar o nível de prontidão de cada requisito.
