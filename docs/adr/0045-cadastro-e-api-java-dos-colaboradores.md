# ADR 0045 — Cadastro de colaboradores e API Java para os outros módulos

**Status:** Provisório (PENDENTE-CESJO P18) (2026-11-21) · Issue #50

## Contexto

O RF014 pede o controle de colaboradores. No marco M2 entra só o cadastro básico (nome, função e
situação, seção 5 do planejamento); contratações e desligamentos ficam para a issue #51 (M5). As
visitas domiciliares (#34), a escuta (#36) e as atividades escolhem colaboradores como profissionais,
então precisam listar quem está ativo e validar a escolha sem acessar a tabela de outro módulo.

O documento de requisitos não lista as funções. O planejamento cita assistente social, psicóloga,
terapeuta ocupacional e "outras", sem confirmação da equipe CESJO.

Alternativas consideradas: colocar o colaborador no módulo `atendimento`, que é quem mais o usa (o
planejamento põe colaboradores em `institucional`, junto dos vínculos do RF014); função como texto
livre (impede contar por função nos relatórios); excluir colaborador (perde o histórico de quem fez
visitas e escutas).

## Decisão

- O módulo `institucional` é dono da tabela `colaborador` (migração `V10`): `nome`, `funcao`,
  `ativo`, `conta_acesso_id` (ADR 0046) e as colunas de autoria e versão de sempre.
- Função é o enum `FuncaoColaborador`: `ASSISTENTE_SOCIAL`, `PSICOLOGO`, `TERAPEUTA_OCUPACIONAL` e
  `OUTRA`, com a mesma lista no `ck_colaborador_funcao`. É provisória (pergunta 18); a tela mostra
  rótulos neutros quanto a gênero ("Psicólogo(a)").
- Colaborador não é excluído. Quem sai da equipe fica inativo (`ativo = false`); as datas e o motivo
  do desligamento entram com a issue #51.
- API HTTP: `GET /api/colaboradores` (paginada, em ordem de nome, filtro opcional `ativo`),
  `GET /api/colaboradores/{id}`, `POST /api/colaboradores` (201 com `Location`, nasce ativo) e
  `PUT /api/colaboradores/{id}` (edição completa, inclusive `ativo` e `contaId`).
- Auditoria: criação e alteração do `colaborador`, com os nomes dos campos que mudaram (`nome`,
  `funcao`, `ativo`, `conta_acesso_id`). Edição sem mudança não gera registro.
- API Java pública `institucional.Colaboradores`, só de leitura e sem checagem de perfil (a permissão
  é de quem chama, como em `Pessoas`):
  - `listarAtivos()`: ativos em ordem de nome, para montar a escolha do profissional;
  - `obterAtivo(id)`;
  - `exigirAtivos(ids)`: lança `ColaboradorIndisponivelException` (422) se algum id não existe ou
    está inativo; os módulos chamam antes de gravar visita, escuta ou atividade;
  - `obterAtivoPorConta(contaId)`: colaborador ligado à conta logada, para sugerir o profissional.
  Os tipos públicos são `ResumoColaborador` (id, nome, função) e `FuncaoColaborador`.

## Consequências

- Os módulos de atendimento e atividades guardam só o `colaborador_id` e validam por
  `exigirAtivos`. Registros antigos continuam apontando para o colaborador mesmo depois de inativo.
- Mudar a lista de funções exige mudar o enum e uma migração nova que troque o `CHECK`.
- Quando a issue #51 entrar, a situação `ativo` pode passar a ser derivada do vínculo vigente; a API
  Java não muda.
