# ADR 0123 — Auditoria de acessibilidade com axe e Playwright no CI

**Status:** Aceito (2026-11-21)

## Contexto

O RNF 3.1 pede que o sistema funcione em Chrome, Firefox, Edge e Safari no celular, com contraste
WCAG AA e formulários navegáveis pelo teclado (issue #24). Os testes do frontend rodam em jsdom, que
não calcula layout, cor nem foco. Conferir à mão a cada PR não se sustenta.

Alternativas consideradas: Lighthouse CI (uma página por vez, sem login, nota agregada em vez de
falhas apontadas); Pa11y (sem controle fino da sessão e da API); testes contra o backend de verdade
(exige banco e dados no job, e lento para o que se quer medir).

## Decisão

- `@playwright/test` e `@axe-core/playwright`, com versões fixas, em `frontend/e2e/`
  (`npm run test:e2e`). O Playwright sobe o `ng serve` e intercepta `/api/**` no navegador com dados
  fictícios (`e2e/api-simulada.ts`), então o teste não depende do backend.
- `acessibilidade.e2e.ts` roda o axe com as regras WCAG 2.0, 2.1 e 2.2 níveis A e AA em: entrar
  (vazio e com erros), trocar senha, início, lista de espera, cadastro de candidato (vazio e com
  erros), contas, nova conta, log de auditoria e página não encontrada. Qualquer violação falha o
  teste e lista regra, alvo e motivo.
- `teclado.e2e.ts` faz o login só com o teclado e percorre com Tab todos os campos do cadastro de
  candidato, conferindo que cada um recebe foco, que o foco tem indicador visível e não fica atrás da
  barra superior nem da navegação inferior, e que Enter no botão dispara a validação.
- Projetos: Chromium com tela de Pixel 7 e desktop de 1280 px (o Edge usa o mesmo motor), WebKit com
  tela de iPhone 13 (aproximação do Safari) e Firefox desktop. O percurso com Tab não roda no WebKit,
  porque o Safari só leva o Tab a campos de texto sem um ajuste do sistema.
- Um único descarte no axe: `target-size` de um campo "parcialmente encoberto" pela navegação
  inferior presa, na posição de rolagem em que a página abre. O campo rola para fora da barra e, ao
  receber foco, o layout o mantém visível ([ADR 0124](0124-correcoes-de-acessibilidade.md)), o que o
  `teclado.e2e.ts` confere. Tamanho ou espaçamento insuficientes de verdade continuam falhando.
- No CI, o job `acessibilidade` instala os três navegadores e roda a suíte; em falha, publica o
  relatório HTML do Playwright.

## Consequências

- Regressões de contraste, nome acessível, rótulos e foco aparecem no PR que as causou.
- O axe pega por volta de metade dos problemas de WCAG; leitor de tela, teclado virtual e o Safari de
  verdade ficam com a checklist manual ([ADR 0125](0125-checklist-manual-de-navegadores.md)).
- A API simulada precisa acompanhar os contratos do backend; uma tela nova entra na lista de telas e,
  se chamar outra rota, na simulação.
