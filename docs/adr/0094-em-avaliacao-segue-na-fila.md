# ADR 0094 — Candidato em avaliação segue na fila

**Status:** Provisório (PENDENTE-CESJO P4) (2026-11-21) · Issues #29 e #34

## Contexto

O agendamento da visita domiciliar (#34, ADR 0049) muda a inscrição para `EM_AVALIACAO` e define
`SituacaoInscricao.ATIVAS` (aguardando e em avaliação) como as situações de quem continua candidato.
A fila (#28) e a ficha (#29) calculavam a posição só entre as inscrições `AGUARDANDO`, e a mudança de
situação (ADR 0091) só deixava sair da fila quem estava aguardando. As branches das issues #29, #34
e #39 mexem no mesmo enum, então os nomes precisam ser os mesmos.

Alternativas consideradas:

- **Em avaliação sem posição**: a fila andaria a cada visita agendada, e o candidato perderia o lugar
  se a avaliação não terminasse em inserção.
- **Em avaliação com posição, mas fora da contagem de quem pode sair**: o candidato avaliado e fora
  do perfil não teria como sair da lista.

## Decisão

- `SituacaoInscricao` tem `AGUARDANDO`, `EM_AVALIACAO`, `INSERIDO` e `SAIU`, com os mesmos nomes e
  rótulos das issues #34 (`EM_AVALIACAO`, `ATIVAS`) e #39 (`INSERIDO`).
- A posição na fila é calculada entre as `ATIVAS`, na ordem de `OrdemDaFila`. Um candidato em
  avaliação mantém o lugar que tinha quando aguardava.
- A ficha deixa sair da fila (`SAIU`) a partir de qualquer situação ativa. `EM_AVALIACAO` não é
  escolhido na ficha; só o agendamento da visita o atribui (ADR 0049).
- No frontend, `SITUACOES_ATIVAS` espelha `ATIVAS`, e o filtro de situação da fila inclui "Em
  avaliação".

## Consequências

- O merge com as issues #34 e #39 junta as constantes do enum sem mudar regra.
- O tamanho da fila no painel de vagas (#30) e o próximo candidato do aviso de vaga (#31) contam as
  situações ativas.
- Se a equipe decidir que a avaliação tira o candidato da fila, muda só `SituacaoInscricao.ATIVAS`.
