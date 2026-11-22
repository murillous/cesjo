# ADR 0172 — Parceiros e ações de articulação

**Status:** Provisório (PENDENTE-CESJO P10) (2026-11-22) · Issue #53

## Contexto

O RF013 pede que o sistema controle a "articulação e fortalecimento de parcerias". O planejamento
esboça `parceiro` (nome, tipo, contato, situação) e `articulacao_parceria` (parceiro, data,
descrição). A pergunta 10 (o que registrar sobre cada parceria: termo, vigência, contatos, ações)
segue sem resposta. As reuniões e eventos do RF012 (issue #52) têm ligação opcional a um parceiro.

Alternativas consideradas:

- Só nome e contato, como no esboço: fica pobre para uma parceria formal com termo e prazo.
- Contatos como lista de pessoas (nome, cargo, telefone, e-mail): mais estrutura do que a equipe
  pediu até agora; um campo de texto cobre o uso inicial.
- Excluir parceiro: perde o histórico das ações e dos eventos ligados. Fica a situação ativo/inativo.
- Tipo como texto livre: impede contar parceiros por tipo nos relatórios.

## Decisão

- Tabelas `parceiro` e `articulacao_parceria` no módulo `institucional` (migração `V56`).
- Campos do parceiro (provisórios, pergunta 10): nome (obrigatório, até 150), tipo de instituição
  (obrigatório), termo ou instrumento (até 200), início e fim da vigência (opcionais, fim não antes do
  início), contato (texto livre, até 500), observações (até 2000) e situação ativo/inativo. Parceiro
  novo nasce ativo; não há exclusão.
- Tipo é o enum `TipoParceiro`: `ORGAO_PUBLICO`, `ORGANIZACAO_SOCIEDADE_CIVIL`, `EMPRESA`,
  `INSTITUICAO_ENSINO` e `OUTRO`, com a mesma lista no `ck_parceiro_tipo` e no `rotulos.ts` do
  frontend.
- Os dados do parceiro são de instituições, não de pessoas atendidas, então não são cifrados.
- Ação de articulação: data (não futura) e descrição (até 2000), ligadas ao parceiro. Registro só de
  inclusão por enquanto, sem edição nem exclusão.
- API: `GET/POST /api/parceiros`, `GET/PUT /api/parceiros/{id}` (lista paginada em ordem de nome,
  filtros opcionais `ativo` e `nome`), `GET/POST /api/parceiros/{id}/articulacoes` (da mais recente
  à mais antiga).
- Auditoria: criação do parceiro e da ação; alteração do parceiro com só os campos que mudaram.
- O marcador `PENDENTE-CESJO(P10)` fica em `DadosParceiro`, `TipoParceiro`, na `V56` e no
  `rotulos.ts` do módulo parcerias.

## Consequências

- Eventos institucionais (#52) podem apontar para `parceiro.id`.
- Se a equipe pedir mais dados (vários contatos, anexo do termo), entram como colunas ou tabela nova
  sem mudar o que já existe.
- Ação registrada errada não pode ser corrigida pela tela; se for preciso, entra a edição.
