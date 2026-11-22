# ADR 0136 — Candidato em avaliação pode ser inserido no serviço

**Status:** Aceito (2026-11-22) · Issue #94

## Contexto

A inserção no serviço (#39, ADR 0080) só aceitava inscrição `AGUARDANDO`. Em paralelo, o agendamento
da visita domiciliar (#34, ADR 0049) passou a mudar o candidato para `EM_AVALIACAO`, e o ADR 0094
define `SituacaoInscricao.ATIVAS` (aguardando e em avaliação) como quem segue na fila. Juntos, os dois
bloqueavam o caminho normal do RF004 ao RF007: o candidato avaliado na visita não podia ser inserido.

Alternativas consideradas:

- **Voltar o candidato para aguardando ao fim da avaliação**: passo manual a mais, sem ganho, e o
  histórico ficaria com mudanças que não aconteceram.
- **Aceitar só `EM_AVALIACAO`**: obrigaria a visita antes de qualquer inserção, regra que o documento
  de requisitos não impõe ao sistema.

## Decisão

- A inserção aceita qualquer situação ativa (`SituacaoInscricao.ativa()`): aguardando ou em
  avaliação. A regra vale em `ServicoInsercao.inserir`, `ServicoCandidatos.registrarInsercao` e
  `InscricaoListaEspera.registrarInsercao`.
- No frontend, a ficha do candidato mostra "Inserir no serviço" e a tela de inserção mostra o
  formulário para as situações de `SITUACOES_ATIVAS`.

## Consequências

- O histórico da inscrição guarda de qual situação o candidato saiu para `INSERIDO` (ADR 0135).
- Se a equipe quiser exigir a visita antes da inserção, a regra muda nesses três pontos.
