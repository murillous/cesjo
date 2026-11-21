# ADR 0093 — Contagem de vagas ocupadas atrás de uma interface

**Status:** Aceito (2026-11-21) · Issue #30

## Contexto

Vagas disponíveis são a capacidade menos as inserções ativas (RF003). As inserções e os
desligamentos são do módulo de usuários atendidos (RF007, RF008), que ainda não existe. Hoje o único
sinal de ocupação é a inscrição na situação `INSERIDO` (ADR 0091, issue #39).

Alternativas consideradas:

- **Esperar o módulo de atendidos**: a issue #30 ficaria sem o número principal.
- **Contar as inscrições `INSERIDO` direto no serviço de vagas**: funciona hoje, mas a troca pela
  conta certa espalharia mudanças pelo serviço, e um desligamento nunca liberaria a vaga.
- **Interface pública na lista de espera, com uma implementação provisória**.

## Decisão

- A interface pública `listaespera.OcupacaoDeVagas` (`vagasOcupadas()`) é o único lugar de onde o
  painel e o aviso de vaga aberta (ADR 0094) tiram a ocupação.
- A implementação de hoje, `OcupacaoPelasInscricoesInseridas`, conta as inscrições `INSERIDO`.
- Quando o módulo de atendidos entrar, ele implementa `OcupacaoDeVagas` com as inserções ativas, e a
  implementação provisória é apagada. Fica um único bean, e a lista de espera continua sem depender do
  módulo de atendidos.

## Consequências

- Até lá, o desligamento de um usuário não libera vaga, porque a inscrição continua `INSERIDO`.
- O painel pode mostrar menos vagas do que a realidade. Quem inserir pessoas sem passar pelo sistema
  (antes da #39) não conta como ocupada.
