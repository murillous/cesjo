# ADR 0011 — Expiração da sessão por inatividade e bloqueio temporário de login

**Status:** Aceito (2026-11-20)

## Contexto

A seção 7 do planejamento pede expiração por inatividade e bloqueio temporário após tentativas
falhas. É preciso escolher os valores e como a falha aparece para quem tenta entrar.

O sistema guarda dados sensíveis (saúde, violações de direitos) e é usado em computadores de
escritório que podem ficar destravados. Por outro lado, a equipe preenche formulários longos (escuta,
PIA), e uma sessão curta demais faz perder trabalho.

Alternativas consideradas:

- 15 minutos de inatividade: mais seguro, mas corta quem passa um tempo lendo ou preenchendo um
  registro longo sem salvar.
- 60 minutos ou mais: confortável, mas deixa a sessão aberta tempo demais num computador
  compartilhado.
- Bloqueio permanente até o Coordenador desbloquear: barra ataque de força bruta, mas permite que
  qualquer um tranque contas alheias e gera trabalho manual.
- Mensagem de erro que diz se o login existe ou se a conta está bloqueada: mais clara, mas revela
  quais logins existem.

## Decisão

- Sessão expira após **30 minutos** sem requisição (`server.servlet.session.timeout=30m`).
- **5** tentativas erradas seguidas bloqueiam a conta por **15 minutos**
  (`sgi.acesso.tentativas-maximas`, `sgi.acesso.duracao-bloqueio`). Um acerto zera o contador; o
  bloqueio termina sozinho. Durante o bloqueio, nem a senha certa entra. O Coordenador desbloqueia na
  hora ao redefinir a senha.
- Toda falha (login inexistente, senha errada, conta desativada ou bloqueada) responde o mesmo 401,
  com a mesma mensagem, que avisa a regra do bloqueio. Login inexistente também confere um hash
  Argon2, para o tempo de resposta não revelar quais logins existem. Toda falha é registrada como
  `LOGIN_FALHO` na auditoria, com o login informado.
- Cookie de sessão HttpOnly, `SameSite=Lax`, só por cookie (sem `jsessionid` na URL) e `Secure` no
  perfil `prod`.

## Consequências

- Os valores mudam por propriedade, sem código.
- Alguém pode bloquear por 15 minutos a conta de outra pessoa errando a senha de propósito; o custo é
  baixo e o Coordenador resolve na hora com a redefinição de senha.
- Quem errar a senha não sabe se o motivo é o login, a senha ou o bloqueio; a mensagem explica a
  regra para a pessoa deduzir.
