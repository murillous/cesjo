# ADR 0217 — Auditoria da importação de planilhas com a ação IMPORTAR

**Status:** Aceito (2026-11-23) · Issue #33

## Contexto

A importação grava muitos registros de uma vez. O log de auditoria (ADR 0020) precisa mostrar quem
importou e quando, além de cada registro criado. As ações existentes são criação, alteração, exclusão,
leitura sensível, exportação e login.

Alternativas consideradas:

- **Só os CRIAR de cada registro**: não dá para achar no log a operação de importação, nem distinguir
  um cadastro pela tela de um importado.
- **Um CRIAR numa entidade "importação"**: mistura uma operação com a criação de um registro que não
  existe em tabela nenhuma.
- **Ação nova `IMPORTAR`**: espelha a `EXPORTAR`; precisa de migração para a restrição da tabela.

## Decisão

- `AcaoAuditoria.IMPORTAR` e `EventoAuditoria.importacao(entidade)`, sem `entidade_id` e sem campos,
  como a exportação (ADR 0023). A migração `V59__auditoria_acao_importar.sql` troca a restrição
  `auditoria_acao_valida`.
- A confirmação registra um `IMPORTAR` com a entidade `planilha_lista_espera`, na mesma transação que
  grava as linhas. Cada pessoa, inscrição e histórico criado continua com o seu `CRIAR`, pelo mesmo
  caminho do cadastro pela tela.
- Baixar o modelo, ver as colunas e pré-visualizar não são auditados: não gravam nada, e a
  pré-visualização só mostra o que a própria pessoa enviou.
- A tela do log ganha o rótulo "Importação de planilha" para a ação.

## Consequências

- Quem revisa o log filtra por `IMPORTAR` e acha cada importação; os registros criados ficam no mesmo
  minuto, pela mesma conta.
- O número de linhas importadas não vai para o log, que guarda só nomes de campos (ADR 0020).
