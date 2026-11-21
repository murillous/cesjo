# ADR 0009 — Identidade da conta fora do módulo acesso e erros de segurança fora do tratador genérico

**Status:** Aceito (2026-11-20) · Detalha o [ADR 0007](0007-contratos-da-fundacao.md)

## Contexto

O ADR 0007 pôs `ContaAtual` no módulo `acesso` e mandou todo módulo registrar auditoria. Mas o
próprio `acesso` registra auditoria (login, falha de login, contas), então `acesso` depende de
`auditoria`. Se a auditoria lesse a conta por `ContaAtual`, `auditoria` dependeria de `acesso`, e o
Spring Modulith acusaria ciclo.

Além disso, o `TratadorGlobalErros` tem um `@ExceptionHandler(Exception.class)` que capturava o
`AccessDeniedException` do `@PreAuthorize` e respondia 500 em vez de 403.

Alternativas consideradas: o acesso publicar eventos (`LoginRealizado`) que a auditoria escuta; o
acesso sempre preencher `contaId`; mover `Perfil` e `Permissao` para `compartilhado`.

## Decisão

- `compartilhado.seguranca.Identidade` (id e login) e `compartilhado.seguranca.IdentidadeAtual`
  leem quem está logado sem depender de `acesso`. `acesso.ContaAutenticada` estende `Identidade`.
- A auditoria usa só `IdentidadeAtual`. Os módulos que não são usados pelo `acesso` (notificações,
  arquivos e os seguintes) podem continuar usando `acesso.ContaAtual`, que também dá o perfil.
- `acesso` chama `auditoria.Auditoria` direto, sem eventos.
- O `TratadorGlobalErros` relança `AccessDeniedException` e `AuthenticationException`, e o Spring
  Security responde 401 e 403. O formato `ProblemDetail` dessas respostas fica com o módulo `acesso`.

## Consequências

- Não há ciclo: `acesso` depende de `auditoria`, e `auditoria` só de `compartilhado`.
- A regra fica: módulo que o `acesso` usa não importa nada de `acesso`.
- `ErrosDeSegurancaTests` garante 401 sem login e 403 sem permissão.
