# ADR 0150 — Registro de atividades com usuários e famílias (provisório)

**Status:** Aceito (2026-11-22) · Issue #45 · Provisório nos pontos P23 e P24

## Contexto

O RF010 pede o registro de seis tipos de atividade. A issue #45 cobre os três coletivos: atividades
com famílias, com usuários e envolvendo usuários e famílias, cada uma com data, título, descrição,
participantes e profissionais. Os outros três tipos (atendimentos e estudos de caso, issue #46) usam o
mesmo registro, e os dashboards e o relatório mensal (issues #57 e #58) contam atividades por período e
por tipo.

O esboço da seção 5 do planejamento propõe `atividade`, `atividade_participante` (pessoa ou
responsável) e `atividade_profissional`, com descrição cifrada só nos tipos individuais.

Alternativas consideradas:

- Uma tabela por grupo de tipos (coletivas e individuais). Duplica regras e obriga as contagens a
  somar duas tabelas.
- Participante como pessoa ou responsável (familiar). O responsável não tem cadastro próprio além do
  nome, e a equipe não disse se quer registrar cada familiar presente (pergunta P23).
- Descrição cifrada só nos tipos individuais. O conversor de cifra é por coluna, então seriam duas
  colunas ou uma regra por linha; e a descrição de uma atividade coletiva também pode citar a
  situação de um usuário.
- Agenda de atividades planejadas. O RF010 fala em registro, e os relatórios contam atividades
  realizadas (pergunta P24).

## Decisão

- O módulo `atividades` é dono das tabelas `atividade`, `atividade_participante` e
  `atividade_profissional` (migração `V45`). A atividade guarda só ids: `pessoa_id` dos participantes e
  `colaborador_id` dos profissionais.
- `atividade.tipo` é o enum público `atividades.TipoAtividade`, com restrição `CHECK` no banco. Os
  tipos coletivos são `COM_FAMILIAS`, `COM_USUARIOS` e `COM_USUARIOS_E_FAMILIAS`.
- Todos os campos são obrigatórios: tipo, data (`data_atividade`), título (até 200 caracteres, em
  claro, para listar e buscar), descrição (até 10.000 caracteres, sempre cifrada pelo
  `TextoCifradoConverter`), pelo menos um participante e pelo menos um profissional.
- Participantes são usuários atendidos ativos, conferidos por `UsuariosAtendidos.exigirAtivos`; a
  escolha vem de `UsuariosAtendidos.listarAtivos`, os dois novos na API pública do módulo Atendidos.
  Nas atividades com famílias, o participante indica de qual usuário é a família. **Provisório
  (P23).**
- A data não pode ser futura (dia em `America/Sao_Paulo`): só se registra atividade realizada.
  **Provisório (P24).**
- Profissionais são colaboradores ativos (`Colaboradores.exigirAtivos`, ADR 0045).
- `POST /api/atividades` registra (201); `PUT /api/atividades/{id}` corrige com as mesmas regras, mas
  só os participantes e profissionais acrescentados precisam estar ativos. A auditoria guarda a
  criação e, na correção, só os nomes do que mudou (`tipo`, `data_atividade`, `titulo`, `descricao`,
  `participantes`, `profissionais`).
- `GET /api/atividades` lista paginado, da mais recente para a mais antiga, sem a descrição.
  `GET /api/atividades/{id}` devolve a descrição. `GET /api/atividades/opcoes` devolve usuários
  ativos, colaboradores ativos e o colaborador sugerido pela conta logada (ADR 0046).
- A atividade não é excluída.

## Consequências

- Atendimentos e estudos de caso (issue #46) entram como mais valores de `TipoAtividade`, na mesma
  tabela.
- Se a equipe quiser registrar familiares presentes (P23), entra uma coluna ou tabela de
  participantes familiares, sem mudar a contagem por tipo.
- A descrição não é pesquisável; a busca fica no título, nas datas, no tipo e nas pessoas.
- O título aparece na listagem para todos que leem atividades, então a tela pede para não escrever
  dado sigiloso nele.
