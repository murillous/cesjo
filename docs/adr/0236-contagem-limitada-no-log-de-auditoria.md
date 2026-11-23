# ADR 0236 — Contagem limitada na listagem do log de auditoria

**Status:** Aceito (2026-11-23) · Issue #25

## Contexto

A listagem do log de auditoria (ADR 0024) devolvia uma página do Spring Data, que conta o total exato
a cada página. Com os índices do ADR 0235, as consultas filtradas ficaram rápidas, mas a tela abre sem
filtro, e contar o log inteiro lê a tabela toda: cerca de 160 ms sozinha na massa de 2 milhões de
linhas. Sob a carga de 20 usuários, o máximo dessa consulta passou de 3 s em três execuções seguidas
(3,4 s, 3,9 s e 3,3 s). Nenhum índice resolve isso, e o custo cresce junto com o log, que nunca é
expurgado (ADR 0022).

Alternativas consideradas:

- **Total estimado pelas estatísticas do Postgres** (`pg_class.reltuples`): é rápido, mas só serve
  sem filtro e pode errar bastante logo depois de muitas inserções.
- **Abrir a tela com um período padrão**: a API continua igual, mas a consulta sem filtro continua
  lenta para quem limpar os filtros.
- **Paginação por cursor, sem total**: é a mais barata, mas a tela perde o paginador com o número de
  páginas.
- **Contar só até um teto**.

## Decisão

- A listagem conta os registros do filtro só até 10.000
  (`ConsultaAuditoriaController.TETO_CONTAGEM`). A contagem é
  `select count(*) from (select 1 from auditoria where ... limit 10001)`, que para de ler assim que
  passa do teto. A página em si continua usando o índice da ordenação.
- A resposta mantém o formato das outras listagens (`content` e `page`) e ganha `page.totalExato`.
  Acima do teto, `totalElements` é 10.000, `totalPages` é calculado sobre ele e `totalExato` é falso.
  As páginas além desse limite continuam respondendo; a tela é que não oferece navegação até elas.
- A tela mostra "Mais de 10.000 registros", pede um filtro para ver o total exato e escreve "1 a 20 de
  mais de 10.000" no paginador.
- O filtro passa a ser o registro `FiltroAuditoria`, que monta a `Specification` da página e o SQL da
  contagem a partir dos mesmos valores.

## Consequências

- O custo da listagem sem filtro não cresce mais com o log. Os tempos estão em `docs/desempenho.md`.
- Quem precisa do total exato de um período grande, por exemplo para um relatório, filtra até ficar
  abaixo do teto ou consulta o banco.
- Mudar o teto é mudar uma constante. A tela usa o número que vem do servidor.
