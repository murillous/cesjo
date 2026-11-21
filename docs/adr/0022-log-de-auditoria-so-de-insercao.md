# ADR 0022 — Log de auditoria só de inserção, guardado sem prazo

**Status:** Provisório (PENDENTE-CESJO P14) (2026-11-20)

## Contexto

Um log de auditoria que pode ser alterado ou apagado não prova nada. A aplicação conecta no banco
com um único usuário, dono das tabelas, então permissões (`REVOKE UPDATE, DELETE`) não protegem
contra esse usuário e dependem de como a VPS cria os usuários do banco. O prazo de guarda dos dados
pessoais ainda não foi definido (pergunta 14 da seção 9 do planejamento), e o log tem login e IP.

Alternativas consideradas: permissão por usuário do banco (exige dois usuários, um dono para o
Flyway e outro para a aplicação, decisão da implantação); só não expor update e delete no código;
expurgo automático depois de um prazo escolhido agora.

## Decisão

- Gatilhos na migração V1 recusam `UPDATE`, `DELETE` e `TRUNCATE` na tabela `auditoria` com
  `SQLSTATE 42501`, para qualquer usuário que não desligue os gatilhos.
- No código, a entidade `RegistroAuditoria` é `@Immutable` e o repositório não estende
  `CrudRepository`: não existe método de alterar nem de excluir.
- Sem expurgo: o log fica guardado para sempre até a equipe responder a pergunta 14. O marcador
  `PENDENTE-CESJO(P14)` fica na migração V1, junto dos gatilhos.

## Consequências

- Nem a aplicação nem um `DELETE` manual apagam o log; só um superusuário que desligue os gatilhos.
- Um expurgo futuro precisa de uma migração que troque os gatilhos para permitir só `DELETE` de
  linhas mais antigas que o prazo, e de um ADR que substitua este.
- Os testes usam um banco compartilhado e não conseguem limpar a tabela; cada teste cria seus
  registros com uma entidade única.
- Separar usuário dono e usuário da aplicação continua sendo boa prática e pode entrar depois, com
  o gatilho como segunda barreira.
