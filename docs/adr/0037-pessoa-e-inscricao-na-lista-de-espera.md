# ADR 0037 — Pessoa no módulo Pessoas e inscrição no módulo Lista de espera

**Status:** Aceito (2026-11-21) · Issue #26

## Contexto

O RF001 cadastra um candidato com dados da pessoa (nome, tipo de deficiência, telefone), do
responsável e da inscrição (origem do encaminhamento, data de cadastro). A mesma pessoa depois vira
usuária atendida (RF007), recebe visitas (RF004) e aparece em atividades (RF010). O planejamento
(seção 5) separa `pessoa` e `responsavel` de `inscricao_lista_espera`.

Alternativas consideradas: uma tabela só de candidato na lista de espera, copiada para o cadastro do
usuário atendido na inserção (duplica dados e cria duas versões da mesma pessoa); a lista de espera
acessar as tabelas de pessoa direto (quebra a fronteira do Spring Modulith).

## Decisão

- O módulo `pessoas` é dono das tabelas `pessoa` e `responsavel` e expõe a API Java `Pessoas`
  (`cadastrar(NovaPessoa)`, `obter(id)`), com os tipos públicos `NovaPessoa`, `DadosPessoa`,
  `TipoDeficiencia` e `Telefone`. A auditoria de pessoa e responsável é feita lá.
- O módulo `listaespera` é dono de `inscricao_lista_espera` e guarda só o `pessoa_id`. O cadastro
  chama `Pessoas.cadastrar` e grava a inscrição na mesma transação.
- Uma pessoa tem um tipo de deficiência; quem tem mais de uma é cadastrado como `MULTIPLA`.
- A inscrição nasce com situação `AGUARDANDO`. A posição na fila não tem coluna; sai da ordem de
  `data_cadastro` e, no empate, de `criado_em` (índice `ix_inscricao_fila`).
- API HTTP: `POST /api/lista-espera/inscricoes` (201 com `Location`) e
  `GET /api/lista-espera/inscricoes/{id}`. A resposta junta os dados da inscrição e da pessoa.
- No RF001 o responsável tem só o nome. Parentesco, telefone próprio e cuidador principal entram
  com o cadastro completo (RF007, pergunta 7).

## Consequências

- A inserção no serviço (#39) reaproveita a mesma pessoa, sem cópia.
- Listar a fila com nome (#28) precisa de consulta que junte inscrição e pessoa. Para não quebrar a
  fronteira, a API `Pessoas` vai ganhar uma busca em lote por ids quando a listagem entrar.
- Não há trava contra cadastrar a mesma pessoa duas vezes. Sem CPF no RF001, não há chave confiável;
  a busca por nome da listagem ajuda a evitar duplicidade.
