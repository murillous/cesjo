# Decisões e Convenções do Projeto

## 1. Finalidade

Este documento registra as decisões necessárias para manter requisitos, DER, modelo lógico, modelo físico e visão do SGI consistentes.

Decisões de negócio que ainda exigem validação estão explicitamente identificadas como pendentes e não devem ser tratadas como regras confirmadas.

## 2. Terminologia canônica

| Termo | Significado |
|---|---|
| Conta de acesso | Profissional autorizado a entrar no sistema |
| Beneficiário | Pessoa com deficiência candidata ou atendida pelo Centro-Dia |
| Entrada na lista de espera | Registro histórico da participação de um beneficiário na fila |
| Vínculo com o serviço | Período em que o beneficiário está inserido no Centro-Dia |
| Colaborador | Pessoa com vínculo de trabalho registrado pelo RF014 |

O termo genérico `Usuario` não deve ser usado como nome de entidade porque o documento de requisitos o utiliza tanto para a pessoa atendida quanto para quem acessa o sistema.

## 3. Baseline tecnológica

| Camada | Decisão |
|---|---|
| Backend | Java 21 e Spring Boot 3.5 |
| API | REST com Spring Web e Bean Validation |
| Persistência | Spring Data JPA |
| Banco | PostgreSQL |
| Migrações | Ferramenta de migration a escolher na criação do projeto |
| Frontend | React com Vite; JavaScript ou TypeScript será decidido no bootstrap |
| Arquitetura | Monólito simples |

Justificativas:

- O repositório não possui implementação anterior a ser preservada.
- Java 21 já estava declarado na descrição do SGI e é compatível com Spring Boot 3.5.
- PostgreSQL já era a preferência expressa no prompt mestre.
- O modelo físico foi consolidado em PostgreSQL para o núcleo já suficientemente definido.

## 4. Hierarquia documental

Em caso de divergência, aplicar a seguinte ordem:

1. [Documento de requisitos](requisitos.md).
2. Decisões de negócio formalmente validadas e registradas neste documento.
3. [Modelo lógico](modeloLogico.md).
4. [DER](DER.md).
5. [Modelo físico](modeloFisico.md).
6. Descrição geral do SGI.

Os prompts mestre e auxiliar definem o processo de trabalho, não regras de negócio.

## 5. Decisões confirmadas por reconciliação

- Conta de acesso e beneficiário são entidades diferentes.
- O beneficiário mantém a mesma identidade antes e depois da admissão.
- A lista de espera é uma entidade histórica separada do beneficiário.
- A posição da fila é calculada e não armazenada.
- Não pode haver mais de uma entrada ativa por beneficiário.
- A faixa etária é calculada pela data de nascimento.
- Senhas são armazenadas somente como hash.
- Os perfis documentados são `COORDENADOR`, `EQUIPE_TECNICA`, `ADMINISTRATIVO` e `CONSULTA`.
- O Hibernate não modificará o schema automaticamente.

## 6. Decisões de negócio pendentes

### 6.1 Prioridade da lista

O documento informa simultaneamente:

- prioridade para beneficiários do BPC inscritos no CadÚnico;
- ordem cronológica no RF002.

Interpretação mínima proposta, ainda pendente de validação:

1. primeiro, entradas cujos beneficiários atendam simultaneamente BPC e CadÚnico;
2. dentro desse grupo, ordem por data de entrada;
3. depois, as demais entradas em ordem por data de entrada;
4. em empate, menor identificador da entrada.

Até a validação, implementar apenas a ordenação cronológica do RF002 e não anunciar a prioridade como concluída.

### 6.2 Ciclo da fila

Precisam ser definidos:

- convocação;
- recusa;
- ausência de resposta;
- admissão;
- desistência;
- retirada administrativa;
- retorno posterior à fila.

### 6.3 Arquivos

Precisam ser definidos:

- tipos e tamanho máximo;
- antivírus ou inspeção;
- armazenamento;
- autorização de leitura;
- retenção e exclusão;
- substituição e histórico.

### 6.4 Demais módulos

As pendências por requisito estão na [matriz de rastreabilidade](matrizRastreabilidade.md).

## 7. Controle de mudanças

Toda nova decisão de negócio deve registrar:

- data;
- responsável pela validação;
- requisitos afetados;
- alteração nos modelos lógico e físico;
- necessidade de migration;
- impacto em API, interface e testes.
