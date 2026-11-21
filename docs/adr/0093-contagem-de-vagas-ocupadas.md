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
- **Lista de espera chamar a API de atendidos**: cria um ciclo entre os módulos.
- **Interface pública na lista de espera, implementada por atendidos, com uma implementação padrão**.

## Decisão

- A interface pública `listaespera.OcupacaoDeVagas` (`vagasOcupadas()`) é o único lugar de onde o
  painel e o aviso de vaga aberta (ADR 0095) tiram a ocupação.
- O módulo de atendidos (issue #39) depende da lista de espera, e a lista de espera não pode chamar
  atendidos (ADR 0081, seria ciclo). Por isso é atendidos quem implementa `OcupacaoDeVagas`, com
  `UsuariosAtendidos.contarAtivos()`.
- Enquanto não houver essa implementação, `VagasConfig` registra a padrão,
  `OcupacaoPelasInscricoesInseridas`, com `@ConditionalOnMissingBean`. Ela conta as inscrições
  `INSERIDO`. Quando atendidos registrar a sua, a padrão deixa de ser criada, sem mudar nada aqui.

## Consequências

- Até a junção com atendidos, o desligamento de um usuário não libera vaga, porque a inscrição continua `INSERIDO`.
- O painel pode mostrar menos vagas do que a realidade. Quem inserir pessoas sem passar pelo sistema
  (antes da #39) não conta como ocupada.
