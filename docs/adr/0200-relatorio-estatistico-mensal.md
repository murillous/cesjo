# ADR 0200 — Relatório estatístico mensal: indicadores numa lista só e período parametrizável

**Status:** Provisório (PENDENTE-CESJO P9) (2026-11-22) · Issue #58

## Contexto

A issue #58 (RF016) pede o relatório estatístico mensal: escolha do mês e quantitativos de lista de
espera, usuários, atendimentos, atividades, comunicações e desligamentos, exportados em PDF e Excel.
A pergunta P9 (modelos atuais dos relatórios) não foi respondida, então o conteúdo exato vai mudar.
A issue #59 vai pedir o mesmo conteúdo por quadrimestre e por ano.

Alternativas consideradas para o conteúdo:

- **Um SQL grande no módulo Estatísticas**: rápido de escrever, mas lê tabelas de seis módulos e
  quebra a regra de ler só pelas APIs Java (ADR 0195).
- **Um método por indicador espalhado pelo serviço**: cada mudança da equipe mexe em vários lugares
  (serviço, exportação, tela).
- **Uma lista de indicadores num lugar só**: cada indicador é uma linha da lista que diz o grupo, o
  nome e a chamada à API do módulo dono do dado. A tela, o PDF e a planilha mostram as linhas na
  ordem da lista.

## Decisão

- `estatisticas.interno.IndicadoresDoRelatorio` é o conteúdo do relatório: uma lista de `Indicador`,
  cada um devolve uma ou mais `LinhaRelatorio(grupo, indicador, quantidade)`. Mudar o relatório é
  mudar essa lista. O marcador `PENDENTE-CESJO(P9)` fica nela.
- Conteúdo provisório, por grupo:
  - Lista de espera: novas inscrições (data de cadastro no período); candidatos na fila no último dia
    (aguardando ou em avaliação, pelo histórico de situação); saídas da lista por motivo.
  - Usuários atendidos: inserções no serviço; usuários atendidos no período (como na taxa da P32);
    usuários ativos no último dia.
  - Atendimentos: os tipos individuais do RF010 (atendimento multiprofissional, atendimento técnico
    individualizado e estudo de caso).
  - Atividades: as atividades em grupo do RF010 (com famílias, com usuários, com usuários e famílias).
  - Contatos e comunicações: os contatos por tipo; ofícios, declarações e encaminhamentos separados em
    expedidos ou enviados e recebidos.
  - Desligamentos: por motivo.
  - Violações de direitos ficam de fora: a issue não pede, e a P33 ainda está aberta.
- APIs novas, todas em SQL agregado nas tabelas do próprio módulo: `ListaEspera.contarInscricoes`,
  `contarNaFilaEm` e `contarSaidasPorMotivo` (em `listaespera.interno.ContagensListaEspera`), e
  `UsuariosAtendidos.contarInsercoes` e `contarAtivosEm` (em `atendidos.interno.ContagensDoPeriodo`).
  Comunicações e atividades usam as APIs que os módulos já tinham (`Comunicacoes`, `Atividades`).
- O período é parametrizável: `PeriodoDoRelatorio(titulo, Periodo)`, com a fábrica `mensal(YearMonth)`.
  O quadrimestral e o anual (issue #59) são novas fábricas e novos endpoints, com a mesma lista.
- Endpoints, com `RELATORIOS_LER`: `GET /api/estatisticas/relatorios/mensal?mes=AAAA-MM` devolve o
  relatório em JSON, sem cache; `GET .../mensal/exportacao?mes=AAAA-MM&formato=pdf|xlsx` exporta pela
  base do módulo Relatórios (ADR 0040), com colunas Grupo, Indicador e Quantidade e o período no
  cabeçalho. A base registra `EXPORTAR` em `relatorio_estatistico` na auditoria (ADR 0043). Ver o
  relatório na tela não é auditado, como os dashboards (ADR 0195).
- O mês corrente pode ser gerado, com o que houver até o dia. Mês futuro responde 422.
- O Consulta, que lê relatórios (ADR 0196), também exporta: o arquivo só tem contagens.

## Consequências

- Quando a equipe mandar o modelo (P9), a mudança fica em `IndicadoresDoRelatorio` e, se precisar de
  um número que nenhuma API dá, num método novo na API do módulo dono.
- Os rótulos de atividades e comunicações ficam em `RotulosAtividade` e `RotulosComunicacao`, porque
  os enums do módulo Atividades não têm rótulo. Se ganharem, esses arquivos somem.
