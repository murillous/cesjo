# ADR 0178 — Registro de doações recebidas e realizadas

**Status:** Provisório (PENDENTE-CESJO P11) (2026-11-22) · Issue #54

## Contexto

O RF015 pede o registro das doações realizadas e recebidas. O planejamento esboça `doacao` com
direção (recebida, realizada), data, contraparte, tipo, descrição e "quantidade ou valor". A pergunta
11 (as doações incluem dinheiro? é preciso emitir recibo ou prestar contas com valores?) segue sem
resposta. A issue pede listagem por período.

Alternativas consideradas:

- Só texto, sem valor: perde o total em dinheiro, que a prestação de contas e o relatório de
  transparência (RF016) devem pedir.
- Valor obrigatório para toda doação (estimativa em reais dos bens): exige avaliar cada cesta ou
  roupa, trabalho que a equipe não pediu.
- Emitir recibo com numeração: depende de modelo e de regra fiscal que a equipe não deu; um recibo
  errado é pior que nenhum.
- Quantidade numérica com unidade: as doações misturam quilos, caixas, peças e unidades; texto livre
  cobre isso sem uma tabela de unidades.

## Decisão

- Tabela `doacao` no módulo `institucional` (migração `V58`).
- Direção é o enum `DirecaoDoacao` (`RECEBIDA`, `REALIZADA`). Data obrigatória e não futura.
  Contraparte obrigatória em texto (até 200): quem doou ao Centro-Dia ou quem recebeu dele.
  Descrição obrigatória (até 2000). Quantidade em texto livre opcional (até 100).
- Tipo é o enum `TipoDoacao` (provisório, pergunta 11): `DINHEIRO`, `ALIMENTOS`, `VESTUARIO`,
  `HIGIENE_LIMPEZA`, `MATERIAL_PEDAGOGICO`, `MOVEIS_EQUIPAMENTOS` e `OUTRO`, com a mesma lista no
  `ck_doacao_tipo` e no `rotulos.ts` do frontend.
- Valor em reais (`NUMERIC(12,2)`, maior que zero) é opcional e só existe na doação em dinheiro
  (provisório, pergunta 11). A regra fica em `DadosDoacao` e é repetida no `ck_doacao_valor`. Sem
  recibo nem prestação de contas por enquanto.
- Os dados não são cifrados: são de doadores e instituições, não de usuários atendidos. Quando a
  contraparte é uma família atendida, a descrição não deve trazer detalhes de saúde.
- API: `GET /api/doacoes` (paginada, da mais recente à mais antiga, filtros opcionais `de`, `ate`,
  `direcao` e `tipo`; período invertido responde 422), `GET /{id}`, `POST` (201) e `PUT /{id}`. Sem
  exclusão.
- Auditoria: criação e alteração com só os nomes dos campos que mudaram.
- O marcador `PENDENTE-CESJO(P11)` fica em `DadosDoacao`, `TipoDoacao`, na `V58` e no `rotulos.ts`
  do módulo doações.

## Consequências

- O total em dinheiro por período pode ser somado pelos relatórios do RF016.
- Se a equipe pedir recibo, ele entra como documento gerado a partir da doação, com numeração
  própria, sem mudar a tabela.
- Se a equipe quiser valor estimado para bens, basta relaxar a regra do valor em `DadosDoacao` e no
  `ck_doacao_valor`.
