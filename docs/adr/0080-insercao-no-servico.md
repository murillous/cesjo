# ADR 0080 — Inserção no serviço: modelo, regras e cuidador familiar

**Status:** Provisório (PENDENTE-CESJO P7) (2026-11-21) · Issue #39

## Contexto

O RF007 começa quando um candidato da fila é inserido no serviço. O item 30 do backlog pede, a
partir da fila, a data de inserção, o turno e o cuidador familiar responsável; a inscrição sai da
fila como "inserida" e as vagas disponíveis diminuem. A seção 5 do planejamento esboça a tabela
`insercao` (pessoa, data, turno matutino, vespertino ou integral, cuidador, situação ativa ou
desligada).

A pergunta P7 ainda não foi respondida: o cuidador responsável do RF007 é sempre o responsável do
RF001? Pode haver mais de um?

Alternativas consideradas para o cuidador:

- Texto livre na inserção: rápido, mas duplica a pessoa que já existe como responsável e não serve de
  contato depois.
- Sempre o responsável do RF001: simples, mas trava o caso comum de outra pessoa da família cuidar.
- Um responsável da pessoa, escolhido entre os cadastrados ou cadastrado na hora: reaproveita o
  módulo Pessoas e permite trocar de pessoa sem perder o registro.

## Decisão

- O usuário atendido é a pessoa com uma inserção. A tabela `insercao` (migração V25) guarda
  `pessoa_id`, `inscricao_id`, `data_insercao`, `turno`, `cuidador_responsavel_id`, `situacao`
  (`ATIVA` ou `DESLIGADA`) e as colunas de autoria. A faixa etária não tem coluna: sai da data de
  nascimento.
- O turno é obrigatório: `MATUTINO`, `VESPERTINO` ou `INTEGRAL` (enum público `atendidos.Turno`). Se
  a capacidade for por turno (P3), a contagem por turno já existe (ADR 0081).
- Regras, conferidas no backend:
  - só uma inscrição que está `AGUARDANDO` pode ser inserida, e cada inscrição vira no máximo uma
    inserção (restrição única em `inscricao_id`);
  - a data de inserção não pode ser futura nem anterior à data de cadastro na fila;
  - uma pessoa tem no máximo uma inserção ativa (índice único parcial). Depois de desligada, pode
    voltar por uma inscrição nova.
- Cuidador familiar responsável: um por inserção, escolhido entre os responsáveis da pessoa
  (`cuidadorResponsavelId`) ou cadastrado na hora como novo responsável (`nomeNovoCuidador`), nunca os
  dois. A tela já traz marcado o responsável do cadastro na fila. A regra fica em `NovaInsercao`
  (backend) e `formulario-insercao.ts` (frontend), com o marcador `PENDENTE-CESJO(P7)`.
- Tudo numa transação: a inscrição sai da fila, o cuidador novo é cadastrado e a inserção é criada.
  Se qualquer passo falhar, nada fica gravado.
- API HTTP: `GET /api/atendidos/candidatos/{inscricaoId}` (dados do candidato e responsáveis para a
  tela) e `POST /api/atendidos/insercoes` (201 com `Location: /api/atendidos/{id}`). Erros de regra
  respondem 422; inscrição inexistente, 404.
- Auditoria: criação de `insercao`, alteração de `inscricao_lista_espera` (campo `situacao`) e, quando
  houver, criação de `responsavel`.

## Consequências

- Se a equipe disser que o cuidador é sempre o responsável do RF001, a tela deixa de oferecer "Outra
  pessoa" e `NovaInsercao` passa a exigir o id. Se houver mais de um cuidador, entra uma tabela de
  ligação e um ADR novo.
- Parentesco e telefone do cuidador entram com o cadastro completo (#40, pergunta P7).
- A tela de inserção abre por `/atendidos/inserir/{inscricaoId}`. O botão "Inserir no serviço" na
  fila entra junto com a listagem da fila (#28).
