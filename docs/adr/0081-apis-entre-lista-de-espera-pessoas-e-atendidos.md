# ADR 0081 — APIs entre Lista de espera, Pessoas e Atendidos

**Status:** Aceito (2026-11-21) · Issue #39

## Contexto

A inserção no serviço (ADR 0080) mexe em três módulos: lê a inscrição e a tira da fila (Lista de
espera), lê e cadastra responsáveis (Pessoas) e cria a inserção (Atendidos). As vagas disponíveis
(RF003) são a capacidade, que é da Lista de espera, menos as inserções ativas, que são de Atendidos.
O Spring Modulith recusa ciclos entre módulos, então só um dos dois pode chamar o outro.

Alternativas consideradas:

- Atendidos atualiza a tabela `inscricao_lista_espera` direto: quebra a fronteira do módulo.
- Evento de domínio publicado por Atendidos e ouvido pela Lista de espera: tira a dependência de
  Atendidos para Lista de espera, mas a inscrição sairia da fila fora da transação da inserção, e uma
  falha deixaria os dois lados diferentes.
- Chamada síncrona a uma API Java pública da Lista de espera, na mesma transação.

## Decisão

- Atendidos depende de Lista de espera e de Pessoas, nunca o contrário.
- Lista de espera ganha a API pública `ListaEspera`, com `DadosInscricao`:
  - `obter(inscricaoId)`;
  - `registrarInsercao(inscricaoId)`: muda a situação para `INSERIDO`, confere a versão na hora
    (`saveAndFlush`) e audita a alteração. Participa da transação de quem chama.
- `SituacaoInscricao` ganha só `INSERIDO` ("Inserido no serviço"). Os outros motivos de saída
  continuam com a issue #29 (pergunta P4).
- Pessoas ganha `responsaveis(pessoaId)` (o do RF001 primeiro, marcado como `principal`) e
  `cadastrarResponsavel(pessoaId, nome)`, que audita a criação.
- Atendidos expõe `UsuariosAtendidos`, com `contarAtivos()` e `contarAtivosPorTurno()`.
- As vagas disponíveis serão calculadas onde a dependência já existe: em Atendidos, ou num ponto que
  dependa dos dois, lendo a capacidade por uma API da Lista de espera. Lista de espera não pode chamar
  `UsuariosAtendidos`, porque criaria um ciclo.

## Consequências

- A inscrição e a inserção mudam juntas ou não mudam: o teste de integração confere que uma falha
  depois de tirar da fila devolve a inscrição a `AGUARDANDO`.
- A issue da capacidade e das vagas (item 21 do backlog) precisa seguir a direção acima. Se preferir
  calcular as vagas dentro da Lista de espera, a contagem terá de chegar por evento, num ADR novo.
