# ADR 0110 — Desligamento do usuário atendido

**Status:** Provisório (PENDENTE-CESJO P17) (2026-11-21) · Issue #42

## Contexto

O RF008 pede o registro dos desligamentos com motivo: evasão, mudança para outro município,
solicitação da família, alcance dos objetivos e outros. O item 33 do backlog acrescenta data e
observação cifrada; a inserção passa a "desligada" e a vaga liberada dispara a notificação de vaga
(#31). A matriz da seção 6 dá "total" ao Coordenador e "ler e editar" à Equipe Técnica em "Usuários
atendidos e desligamentos", sem dizer se a Equipe Técnica registra desligamento, que libera vaga.

## Decisão

- Tabela `desligamento` (migração V35): `insercao_id` (único: um desligamento por inserção), `data`,
  `motivo` (`EVASAO`, `MUDANCA_MUNICIPIO`, `SOLICITACAO_FAMILIA`, `ALCANCE_OBJETIVOS`, `OUTROS`, enum
  público `atendidos.MotivoDesligamento`), `observacao` cifrada com `TextoCifradoConverter` e as colunas
  de autoria.
- Regras: só inserção `ATIVA`; data nem futura nem anterior à data de inserção; observação de até
  2000 caracteres, opcional, mas obrigatória no motivo "Outros", para o motivo ficar explicado.
- Numa transação só: grava o desligamento, muda a inserção para `DESLIGADA` e publica `VagaLiberada`
  (ADR 0111).
- API: `POST /api/atendidos/{id}/desligamento`, com a permissão nova `ATENDIDOS_DESLIGAR`, só do
  Coordenador. A resposta não traz a observação.
- Auditoria: criação de `desligamento` e alteração de `insercao` (campo `situacao`). A observação é
  lida só na ficha completa (`ATENDIDOS_LER`); quando ela existe e é entregue, a leitura é registrada
  como leitura de dado sensível de `desligamento`.
- A visão básica (ADR 0082 e 0083) ganha a data de desligamento, que é uma das "datas" dos dados
  básicos; motivo e observação ficam só na visão completa.
- Frontend: botão "Registrar desligamento" na ficha e tela `/atendidos/:id/desligar`. A ficha mostra
  o bloco "Desligamento".

## Consequências

- Se a Equipe Técnica precisar desligar, basta acrescentar `ATENDIDOS_DESLIGAR` ao perfil, no ponto
  marcado com `PENDENTE-CESJO(P17)`.
- Desfazer um desligamento lançado por engano não existe ainda; se for preciso, entra por ADR novo
  (por exemplo, com reinserção).
- Depois de desligada, a pessoa só volta ao serviço por uma inscrição nova na fila (ADR 0080).
