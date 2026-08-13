# Matriz de Rastreabilidade dos Requisitos

## Legenda

- **Pronto:** há informação suficiente para planejar uma microetapa de implementação.
- **Parcial:** a estrutura pode ser iniciada, mas parte do comportamento depende de definição.
- **Bloqueado:** a implementação produziria regra de negócio ou contrato inventado.

Nenhum requisito funcional está considerado concluído, pois o repositório ainda não possui código-fonte.

## Requisitos funcionais

| Requisito | Modelo ou módulo relacionado | Situação | Pendências principais |
|---|---|---|---|
| RF001 - Cadastro de candidatos | `Beneficiario`, `ListaEspera` | Parcial | Política de arquivos, CPF, possibilidade de ausência de responsável e validação da residência |
| RF002 - Posição na lista | `ListaEspera` | Parcial | Combinação entre cronologia e prioridade BPC + CadÚnico; desempate |
| RF003 - Vagas disponíveis | A definir | Bloqueado | Escopo da vaga, capacidade por turno, abertura, ocupação e canal de notificação |
| RF004 - Visita domiciliar | `VisitaDomiciliar` | Parcial | Participação de vários profissionais, reagendamento e motivo de cancelamento |
| RF005 - Escuta ativa | `Escuta` | Parcial | Permissões, histórico de alterações e vínculo do profissional responsável |
| RF006 - PIA | `PIA` | Parcial | Versionamento, responsáveis, datas de apresentação e regra de aprovação |
| RF007 - Usuário atendido | `Beneficiario`, `VinculoServico` | Parcial | Definição de "dados pessoais completos" e fluxo de admissão |
| RF008 - Desligamento | `Desligamento` | Parcial | Reingresso, alteração e cancelamento de desligamento |
| RF009 - Violações de direitos | `ViolacaoDireito` | Parcial | Sigilo, permissões, encaminhamentos e possibilidade de múltiplas categorias |
| RF010 - Atividades | `Atividade` | Parcial | Participantes, duração, profissionais, local e evidências |
| RF011 - Comunicações | A definir | Parcial | Remetente, destinatário, instituição, direção, anexos e vínculo com beneficiário |
| RF012 - Reuniões e eventos | A definir | Parcial | Participantes, duração, local, responsáveis e resultados |
| RF013 - Parcerias | A definir | Bloqueado | Dados obrigatórios, estados, responsáveis e operações esperadas |
| RF014 - Colaboradores | A definir | Parcial | Dados cadastrais, cargos, vínculos e motivos de desligamento |
| RF015 - Doações | A definir | Bloqueado | Doador, destinatário, itens, quantidades, valores, comprovantes e estados |
| RF016 - Relatórios | Consultas derivadas | Bloqueado | Layouts, filtros, métricas, agrupamentos e regras de fechamento |
| RF017 - Dashboards | Consultas derivadas | Parcial | Fórmulas, filtros, atualização e regra da taxa de desligamento |

## Requisitos não funcionais

| Área | Situação | Definição necessária |
|---|---|---|
| Usabilidade | Parcial | Navegadores e versões suportadas; critérios de acessibilidade |
| Desempenho | Parcial | Quantidade de usuários simultâneos, volume de dados e percentil do limite de 3 segundos |
| Controle de acesso | Parcial | Matriz de permissões por perfil, operação e tipo de dado |
| Criptografia | Parcial | Dados classificados como sensíveis e estratégia em trânsito e em repouso |
| Auditoria | Bloqueado | Eventos auditados, conteúdo, consulta, retenção e proteção do log |
| Backup | Bloqueado | Retenção, local, criptografia, teste de restauração, RPO e RTO |
| Disponibilidade | Parcial | Forma de medição, janelas de manutenção e responsabilidade operacional |
| Recuperação de desastre | Bloqueado | Procedimentos, responsáveis, RPO, RTO e ambiente de recuperação |

## Integrações e intercâmbio de dados

| Item | Situação | Definição necessária |
|---|---|---|
| CadÚnico | Bloqueado | Órgão responsável, API ou arquivo, credenciais, campos e base legal |
| BPC | Bloqueado | Sistema de origem, API ou arquivo, credenciais e campos |
| Saúde e educação | Bloqueado | Sistemas municipais, protocolos, escopo e responsáveis |
| Importação Excel | Bloqueado | Modelo da planilha, validações, duplicidade e relatório de erros |
| Exportação PDF/Excel | Bloqueado | Conteúdo, layout, filtros, permissão e proteção de dados |

## Ordem recomendada

1. Validar as decisões registradas em [decisoesProjeto.md](decisoesProjeto.md).
2. Criar a estrutura inicial do backend com Java 21 e PostgreSQL.
3. Criar a migration do núcleo definido em [modeloFisico.md](modeloFisico.md).
4. Implementar o cadastro básico do beneficiário sem upload.
5. Definir e implementar a política de arquivos.
6. Implementar a entrada cronológica na lista.
7. Validar a prioridade BPC + CadÚnico e implementar a consulta de posição.
8. Detalhar e implementar os requisitos seguintes em microetapas independentes.

