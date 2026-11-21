# ADR 0010 — API de sessão e segurança web do backend

**Status:** Aceito (2026-11-20) · Detalha o [ADR 0003](0003-autenticacao-por-sessao.md) para a issue #13

## Contexto

O ADR 0003 decidiu por sessão no servidor, cookie HttpOnly e CSRF. Faltava decidir como o Angular
entra e sai, como recebe o token CSRF, o que a API responde quando falta sessão ou permissão e o que
acontece com uma sessão aberta quando a conta muda.

Alternativas consideradas:

- `formLogin` do Spring Security: responde com redirecionamento e página HTML, o que não serve para
  uma SPA sem customizar quase tudo.
- `csrf().spa()` do Spring Security 7: faz o mesmo que a decisão abaixo, mas esconde o repositório de
  token, que o login precisa para trocar o token.
- Guardar só o retrato da conta na sessão: uma conta desativada continuaria logada até a sessão
  expirar, e uma mudança de perfil só valeria no próximo login.

## Decisão

- Endpoints JSON em `/api/sessao`:
  - `GET /api/sessao/csrf` (aberto): entrega o token e grava o cookie `XSRF-TOKEN`;
  - `POST /api/sessao/entrar` (aberto, com CSRF): `{login, senha}`; devolve a sessão ou 401;
  - `GET /api/sessao`: `{id, login, nome, perfil, permissoes[], trocaSenhaObrigatoria}`;
  - `POST /api/sessao/senha`: `{senhaAtual, novaSenha}`, troca a senha da própria conta;
  - `POST /api/sessao/sair`: filtro de logout do Spring Security, responde 204.
- `/actuator/health`, `/error` e, fora de produção, o springdoc ficam abertos; todo o resto exige
  sessão.
- CSRF no padrão de SPA: `CookieCsrfTokenRepository` com cookie `XSRF-TOKEN` legível pelo JavaScript
  (`SameSite=Lax`, caminho `/`) e cabeçalho `X-XSRF-TOKEN`, que é o padrão do `HttpClient` do Angular.
  Valor vindo do cabeçalho é comparado cru; vindo de parâmetro, com a proteção contra BREACH.
- O login cria sessão nova (invalida a anterior, contra fixação de sessão) e troca o token CSRF.
- 401 e 403 saem em `ProblemDetail`, em português, sem redirecionamento. O 403 de CSRF tem uma
  mensagem própria pedindo para recarregar a página.
- O principal da sessão (`ContaPrincipal`) implementa `ContaAutenticada`; as authorities são os nomes
  das permissões do perfil.
- A cada requisição autenticada, um filtro relê a conta: conta desativada ou removida encerra a sessão
  com 401; mudança de nome, perfil ou exigência de troca de senha passa a valer na hora. Custa uma
  consulta por chave primária por requisição, aceitável no volume do CESJO.
- Enquanto a conta precisa trocar a senha, o mesmo filtro responde 403 ("Troca de senha
  obrigatória") a tudo, exceto ver a sessão, obter o token CSRF, trocar a senha e sair. O frontend
  mostra a tela de troca, mas quem garante é o backend.

## Consequências

- O frontend não precisa conhecer o Spring Security: três chamadas JSON e o suporte a XSRF do
  `HttpClient` bastam.
- Desativar uma conta ou mudar o perfil vale imediatamente, inclusive para quem está logado.
- O `csrf()` do Spring Security Test troca o repositório de token do contexto de teste pelo de
  sessão; o teste que prova o cookie de verdade fica num contexto próprio (`CsrfIntegracaoTests`).
