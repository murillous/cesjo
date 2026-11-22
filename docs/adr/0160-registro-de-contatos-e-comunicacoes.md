# ADR 0160 — Registro de contatos e comunicações

**Status:** Aceito (2026-11-22) · Issue #48 · Provisório no ponto P30

## Contexto

O RF011 pede que o sistema registre comunicações de cinco tipos. A issue #48 cobre os três primeiros:
contatos telefônicos com cuidadores familiares, contatos telefônicos institucionais e atendimento a
público não acompanhado, com data, pessoa ou instituição e assunto. Os ofícios, declarações e
encaminhamentos ficam para a issue #49, na mesma tabela. O planejamento (seção 5) esboça a tabela
`comunicacao` no módulo `atividades`, com `pessoa_id` opcional.

O módulo `atividades` já tem as atividades do RF010 em PRs abertos (#103 e #105). Para não conflitar,
as comunicações têm classes, migrações e telas próprias, sem depender daquele código.

Alternativas consideradas para "pessoa ou instituição":

- **Ligar a um usuário atendido (`pessoa_id`) ou a um cadastro de instituições**: permite ver os
  contatos na ficha do usuário, mas o atendimento a público não acompanhado é justamente de quem não
  está no sistema, o contato institucional não tem cadastro de instituições ainda (os parceiros do
  RF013 não existem), e a API pública de usuários ativos ainda está em PR aberto.
- **Texto livre (`contraparte`)**: atende os três tipos do mesmo jeito, permite busca por nome e não
  depende de outro módulo. Ligar ao usuário depois é uma coluna nova opcional.

## Decisão

- Tabela `comunicacao` (migração V50) no módulo `atividades`: `tipo`, `data_comunicacao`,
  `contraparte` (pessoa ou instituição, texto livre, até 200 caracteres), `assunto` (até 200),
  `descricao` opcional, mais as colunas de controle e `versao`. **Provisório (P30)**: sem
  `pessoa_id`.
- Tipos no enum público `atividades.TipoComunicacao`: `CONTATO_CUIDADOR_FAMILIAR`,
  `CONTATO_INSTITUCIONAL` e `ATENDIMENTO_PUBLICO_NAO_ACOMPANHADO`, com a mesma lista numa restrição
  `CHECK`.
- Contraparte e assunto ficam em claro, para a listagem e a busca, e a tela avisa para não pôr dado
  sigiloso no assunto. A descrição é cifrada com o `TextoCifradoConverter`. A leitura da ficha não é
  auditada: o log de leitura sensível fica para escuta, PIA, violação e laudo (seção 7).
- Só se registra o que já aconteceu: a data não pode ser futura. **Provisório (P30).**
- API: `GET /api/comunicacoes` (paginada, da mais recente para a mais antiga, sem a descrição, com
  filtros opcionais `tipo`, `inicio` e `fim`), `GET /api/comunicacoes/{id}`, `POST` e `PUT /{id}` para
  corrigir. Criação e correção vão para a auditoria; a correção guarda só os nomes das colunas que
  mudaram e, sem mudança, não registra nada. Não há exclusão.

## Consequências

- Se a equipe quiser os contatos com cuidadores na ficha do usuário atendido, entra uma coluna
  `pessoa_id` opcional e um campo de escolha no formulário, sem mexer nos registros antigos.
- A busca por pessoa ou instituição é por texto: a mesma instituição escrita de dois jeitos aparece
  como duas. Um cadastro de instituições (parceiros, RF013) pode substituir o texto depois.
