# ADR 0049 — Candidato em avaliação e API Java da lista de espera

**Status:** Provisório (PENDENTE-CESJO P4) (2026-11-21) · Issue #34

## Contexto

O planejamento (item 25) diz que, ao agendar a visita domiciliar, o candidato passa a constar como em
avaliação. O agendamento é do módulo `atendimento`, e a inscrição é do módulo `listaespera`, que
ainda não tinha API pública. A pergunta 4 (saída da lista) não foi respondida: não se sabe se o
candidato em avaliação continua ocupando posição na fila, nem o que acontece quando a visita é
cancelada.

Alternativas consideradas: evento de domínio "visita agendada" escutado pela lista de espera (a
mudança ficaria fora da transação do agendamento, e um erro deixaria a visita sem o candidato em
avaliação); o atendimento gravar a situação direto na tabela (quebra a fronteira do Spring Modulith).

## Decisão

- Nova situação `EM_AVALIACAO` ("Em avaliação") em `SituacaoInscricao`. `AGUARDANDO` e
  `EM_AVALIACAO` formam `SituacaoInscricao.ATIVAS`: são os candidatos que seguem na lista de espera.
- API Java pública `listaespera.ListaEspera`, chamada na transação de quem usa:
  - `listarCandidatosAtivos()`: candidatos ativos na ordem da fila (data de cadastro, depois
    `criado_em`), com o nome lido de `Pessoas.nomes`;
  - `candidatoAtivo(pessoaId)`;
  - `colocarEmAvaliacao(pessoaId)`: muda `AGUARDANDO` para `EM_AVALIACAO` e audita a alteração do
    campo `situacao`. Não faz nada se já estiver em avaliação. Lança `RegraNegocioException` (422)
    se a pessoa não tem inscrição ativa.
- A API `Pessoas` ganha `nomes(ids)`, para listar nomes sem uma consulta por pessoa.
- Provisório (pergunta 4), pelo lado que menos mexe na fila:
  - o candidato em avaliação continua na fila, na mesma ordem;
  - cancelar a visita não devolve o candidato para `AGUARDANDO`. A situação só muda de novo com as
    saídas da lista (issue #29).

## Consequências

- A fila da issue #28 precisa considerar as duas situações ativas e mostrar "Em avaliação".
- Se a equipe decidir que o candidato em avaliação sai da fila, ou volta a aguardar quando a visita é
  cancelada, a regra muda em `SituacaoInscricao.ATIVAS` e em `ServicoCandidatos`, nos pontos marcados
  com `PENDENTE-CESJO(P4)`.
