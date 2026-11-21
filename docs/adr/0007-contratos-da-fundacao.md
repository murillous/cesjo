# ADR 0007 — Contratos públicos da Fundação entre módulos

**Status:** Aceito (2026-11-20)

## Contexto

As issues da Fundação (#13 a #19) são feitas em paralelo, mas dependem umas das outras: todo módulo
registra auditoria, todo endpoint checa permissão, a auditoria e as notificações precisam saber qual
conta está logada. Se cada frente inventar sua versão dessas peças, a integração vira retrabalho.

Alternativas consideradas: fazer as issues em sequência; deixar cada frente criar o que precisar e
unificar no fim.

## Decisão

Os contratos abaixo existem antes das frentes começarem. A frente dona pode completar a
implementação, mas não muda as assinaturas sem combinar.

Backend:

- `acesso.Permissao`: enum de permissões finas; cada uma é uma authority com o mesmo nome, checada
  com `@PreAuthorize("hasAuthority('NOME')")`. `@EnableMethodSecurity` fica ligado em
  `acesso.SegurancaMetodosConfig`.
- `acesso.Perfil`: os quatro perfis, cada um com um conjunto fixo de permissões. É o único lugar da
  matriz da seção 6 do planejamento no backend.
- `acesso.ContaAutenticada` e `acesso.ContaAtual`: o principal do Spring Security implementa
  `ContaAutenticada`; os outros módulos leem a conta logada só por `ContaAtual`.
- `auditoria.Auditoria`, `auditoria.EventoAuditoria` e `auditoria.AcaoAuditoria`: API pública do log
  de auditoria. A implementação provisória `AuditoriaEmLog` só escreve no log da aplicação até a
  issue #16 gravar no banco.
- Teste: `@ComConta(perfil = ...)` autentica o teste como uma conta do perfil, com as permissões do
  perfil como authorities.
- Migrações: cada issue usa a sua versão do Flyway para não colidir: #16 `V1`, #13 a #15 `V2`, #18
  `V3`, #19 `V4`. Migrações extras da mesma issue usam `V<n>_1`, `V<n>_2` e assim por diante.

Frontend:

- `acesso/permissao.ts`: tipos `Permissao` e `Perfil`, com os nomes do backend.
- `acesso/sessao.ts`: serviço `Sessao` com `conta`, `autenticada` e `temPermissao`.
- `ModuloFrontend.permissao`: o módulo que declara uma permissão some do menu e tem a rota protegida
  por `exigePermissao` para quem não a tem.

ADRs das frentes paralelas: #13 a #15 usam de 0010 a 0019, #16 de 0020 a 0024, #17 e #18 de 0025 a
0029, #19 de 0030 a 0034.

## Consequências

- As frentes trabalham ao mesmo tempo sem esperar umas pelas outras.
- O backend continua sendo a autoridade: o frontend só esconde o que a conta não pode usar.
- Permissões novas entram em `Permissao` e no tipo do frontend juntas.
