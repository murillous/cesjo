# ADR 0125 — Checklist manual para Safari no iPhone e leitor de tela

**Status:** Aceito (2026-11-21)

## Contexto

A auditoria automática ([ADR 0123](0123-auditoria-de-acessibilidade-no-ci.md)) roda no WebKit do
Playwright, que não é o Safari do iOS: não tem teclado virtual, barra de endereço que se esconde,
VoiceOver nem os ajustes de acessibilidade do sistema. O RNF 3.1 cita o Safari no celular, e boa
parte das pessoas atendidas e das famílias usa celular.

Alternativas consideradas: serviço de dispositivos na nuvem (BrowserStack, Sauce Labs), com custo e
conta, e que ainda não cobre bem o VoiceOver; não conferir o Safari.

## Decisão

- `docs/frontend/checklist-navegadores.md` lista o que conferir à mão em Safari no iPhone, Chrome no
  Android e Chrome, Edge e Firefox no computador: entrada, lista de espera, cadastro de candidato,
  teclado, zoom de 200%, movimento reduzido e texto maior.
- A checklist é feita antes de cada versão que mexa em telas, e o resultado vai na tabela de registro
  do próprio arquivo.

## Consequências

- O que a automação não alcança tem um roteiro e um registro.
- Depende de alguém com iPhone; enquanto o registro estiver vazio, o critério "conferido em Safari no
  celular" da issue #24 fica em aberto.
