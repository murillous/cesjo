# ADR 0206 — Acesso e auditoria do relatório de intervenção

**Status:** Provisório (PENDENTE-CESJO P9) (2026-11-23) · Issue #60

## Contexto

O relatório de intervenção (ADR 0205) lista pelo nome quem teve violação de direitos e a categoria.
Pela LGPD, é dado pessoal sensível, e o relatório junta num lugar só o que hoje se vê caso a caso. A
matriz da seção 6 dá relatórios ao Coordenador e ao Consulta (`RELATORIOS_LER`, ADR 0196) e
violações ao Coordenador e à Equipe Técnica (`VIOLACOES_LER`). A P9 também pergunta para quem o
relatório vai, e não foi respondida.

Hoje as listas de violações não geram evento de auditoria; só a leitura da descrição é registrada
(P19, ADR 0145). Os relatórios estatísticos registram só a exportação, porque têm só contagens
(ADR 0200).

Alternativas consideradas para o acesso:

- **Só `RELATORIOS_LER`**: abre ao Consulta nomes e violações que a linha "violações" da matriz nega a
  ele.
- **Só `VIOLACOES_LER`**: dá o relatório à Equipe Técnica, que acompanha os casos, mas ela não tem a
  área de relatórios (P17).
- **As duas ao mesmo tempo**: cada parte segue a linha da matriz do seu dado, como o gráfico de
  violações do dashboard (ADR 0199). Hoje só o Coordenador tem as duas.

Alternativas consideradas para a auditoria:

- **Só a exportação, como no mensal**: a prévia na tela mostra os mesmos nomes sem deixar rastro.
- **Leitura sensível na prévia e exportação no download**: cada vez que alguém vê ou baixa a lista de
  nomes, fica registrado quem, quando e de qual período.

## Decisão

- Os dois endpoints pedem `RELATORIOS_LER` e `VIOLACOES_LER`. O marcador `PENDENTE-CESJO(P9)` fica
  em `estatisticas.interno.RelatorioIntervencaoControlador` (`PODE_GERAR`) e, no frontend, na rota
  `intervencao` de `relatorios.routes.ts`.
- Gerar na tela registra `LER_SENSIVEL` em `relatorio_intervencao`, com o período (`AAAA-MM-DD/AAAA-MM-DD`)
  como identificador. Baixar registra `EXPORTAR` em `relatorio_intervencao`, pela base de exportação
  (ADR 0043). O download não registra também a leitura: é um evento por ação.
- Pedido recusado (sem permissão, período inválido) não registra nada.

## Consequências

- Se a equipe disser que a Equipe Técnica faz o relatório para o CREAS, basta trocar a expressão
  `PODE_GERAR` para só `VIOLACOES_LER` (e a rota no frontend), ou dar `RELATORIOS_LER` à Equipe
  Técnica (P17).
- O log de auditoria passa a mostrar quem viu a lista de casos de cada período.
