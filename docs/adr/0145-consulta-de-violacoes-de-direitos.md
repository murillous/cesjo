# ADR 0145 — Consulta de violações de direitos

**Status:** Provisório (PENDENTE-CESJO P19) (2026-11-22) · Issue #44

## Contexto

O registro das violações (ADR 0112) guarda a categoria em claro e a descrição cifrada, e deixou a
leitura para a issue #44. O item 35 do backlog pede lista por usuário e geral, com filtro por
categoria e período, e "toda leitura de descrição auditada". A matriz da seção 6 dá leitura a
Coordenador e Equipe Técnica (`VIOLACOES_LER`, ADR 0112). A seção 7 manda auditar "toda leitura de
registro com dado sensível", e a equipe não disse se ver só a categoria numa lista já conta como
leitura de dado sensível.

Alternativas consideradas:

- **Descrição na própria lista**: cada busca viraria dezenas de leituras sensíveis no log, e a
  descrição apareceria na tela de quem só queria ver as categorias.
- **Auditar cada linha listada**: o log encheria de leituras que não mostram o relato, e ficaria
  difícil achar quem leu de fato uma descrição.
- **Lista sem descrição, com leitura sob pedido e auditada**: o relato só sai quando alguém pede,
  e cada pedido fica no log.

## Decisão

- API, todas com `VIOLACOES_LER`:
  - `GET /api/atendidos/{id}/violacoes?tipo&de&ate`: violações da pessoa da inserção (de todas as
    inserções dela), sem paginação;
  - `GET /api/violacoes?tipo&de&ate&page&size`: lista geral paginada;
  - `GET /api/violacoes/{id}`: a violação com a descrição. Cada resposta registra `LER_SENSIVEL` em
    `violacao_direito`.
- As listas devolvem categoria, âmbito, data de identificação, nome da pessoa e a inserção mais
  recente dela (a ativa primeiro), para o link da ficha. Não devolvem a descrição.
- O período é pela data de identificação, com as duas pontas incluídas. Início depois do fim
  responde 422. A ordem é da identificação mais recente para a mais antiga.
- As listas não geram evento de auditoria (`PENDENTE-CESJO(P19)` em `ServicoViolacoes`).
- A consulta é um SQL só (`ConsultaViolacoes`), que junta `pessoa` para o nome, como a listagem de
  atendidos (ADR 0083).
- Frontend: um componente `app-painel-violacoes` com os filtros e a lista. Ele aparece em dois
  lugares: no bloco "Violações de direitos" da ficha do usuário atendido e na página nova
  `/violacoes`, que entra no menu como "Violações de direitos" para quem tem `VIOLACOES_LER`. A
  descrição só é buscada no botão "Ler descrição". Fechar descarta o texto, e abrir de novo é outra
  leitura auditada.

## Consequências

- O log mostra exatamente quem abriu cada relato, e só isso.
- Se a equipe decidir que ver a categoria numa lista é leitura sensível, o ponto marcado com
  `PENDENTE-CESJO(P19)` passa a registrar um evento por linha devolvida, ou um evento da consulta.
- Editar e excluir violações ("total" do Coordenador) continuam fora, como no ADR 0112.
