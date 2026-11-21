# ADR 0003 — Autenticação por sessão com contas locais e Argon2

**Status:** Aceito (2026-11-20) · Registra a linha "Autenticação" da seção 1 do [planejamento](../planejamento.md)

## Contexto

Frontend e API ficam no mesmo domínio (proxy do Caddy em produção, proxy do `ng serve` em
desenvolvimento). Não existe provedor de identidade na organização, e o RNF 3.3 pede controle de
acesso por perfil.

Alternativas consideradas: JWT em `localStorage`; provedor externo (Keycloak, Google).

## Decisão

Login por formulário com sessão do Spring Security no servidor, cookie HttpOnly e proteção CSRF.
Contas locais na tabela `conta_acesso`, senha com Argon2 (`Argon2PasswordEncoder`, que usa o Bouncy
Castle). A primeira conta de Coordenador é criada por variável de ambiente na implantação.

## Consequências

- Sem token no navegador para roubar por XSS; logout invalida a sessão no servidor.
- Toda requisição que altera dados manda o token CSRF.
- Escalar para mais de uma instância exigiria sessão compartilhada (Spring Session); não é
  necessário no volume atual.
