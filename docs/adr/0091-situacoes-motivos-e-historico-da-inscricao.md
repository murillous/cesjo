# ADR 0091 — Situações, motivos de saída e histórico da inscrição

**Status:** Provisório (PENDENTE-CESJO P4) (2026-11-21) · Issue #29

## Contexto

O RF002 pede mudar a situação do candidato com data e motivo e ver o histórico das mudanças. A
pergunta 4 (quais motivos de saída existem e se o candidato chamado que não comparece volta para a
fila) não tem resposta. A inserção no serviço (#39) também muda a situação, para `INSERIDO`, por uma
API Java própria.

Alternativas consideradas:

- **Uma situação por motivo** (`DESISTENTE`, `NAO_LOCALIZADO`...): cada motivo novo mudaria a
  máquina de estados e os filtros da fila.
- **Saída definitiva**: mais simples, mas obriga a cadastrar de novo quem retoma o contato, e o
  candidato perderia a posição original.
- **Histórico só no log de auditoria**: o log guarda só nomes de campos, sem data da mudança nem
  motivo, e só o Coordenador o lê.

## Decisão

- Situações (`SituacaoInscricao`): `AGUARDANDO`, `INSERIDO` e `SAIU`. `INSERIDO` usa o mesmo nome da
  issue #39 e só é atribuído pela inserção no serviço, nunca por esta tela.
- Motivos de saída (`MotivoSaida`), com o marcador `PENDENTE-CESJO(P4)`: `DESISTENCIA`,
  `NAO_LOCALIZADO`, `FORA_DO_PERFIL` e `OUTRO`. O motivo `OUTRO` exige observação.
- `POST /api/lista-espera/inscricoes/{id}/situacao` (`LISTA_ESPERA_EDITAR`) com `situacao`, `data`,
  `motivoSaida` e `observacao`:
  - `AGUARDANDO` para `SAIU`: motivo obrigatório. A inscrição guarda `data_saida` e `motivo_saida`.
  - `SAIU` para `AGUARDANDO`: volta para a posição que a data de cadastro dá, com observação
    obrigatória. Esta é a escolha provisória para "o candidato chamado volta para a fila?".
  - A data não pode ser futura nem anterior à data de cadastro. Qualquer outra transição responde 422.
- Tabela `historico_situacao_inscricao` (migração `V7`), só de inserção: situação anterior e nova,
  data, motivo, observação (cifrada, ADR 0004), quem registrou e quando. O cadastro grava a primeira
  linha (entrada na fila), e a migração cria essa linha para as inscrições que já existiam.
- Auditoria: a mudança registra `ALTERAR` na inscrição (`situacao`, `motivo_saida`, `data_saida`) e
  `CRIAR` na linha do histórico. A leitura da ficha com o histórico não é auditada: a observação de
  saída não está entre os registros sensíveis da seção 7.
- A ficha (`GET /api/lista-espera/inscricoes/{id}`) traz a posição na fila e o histórico. No
  frontend, `/lista-espera/:id` mostra dados, documentos, posição, situação e a linha do tempo.

## Consequências

- Se a equipe definir outros motivos, muda `MotivoSaida`, a restrição `ck_*_motivo_saida` da `V7`
  (numa migração nova) e a lista `MOTIVOS_SAIDA` do frontend.
- Se a equipe decidir que quem sai não volta, ou volta para o fim da fila, muda o caso
  `AGUARDANDO` em `ServicoListaEspera.mudarSituacao`.
- A API da issue #39 que marca a inscrição como inserida precisa gravar também uma linha no
  histórico, para a ficha mostrar a inserção.
