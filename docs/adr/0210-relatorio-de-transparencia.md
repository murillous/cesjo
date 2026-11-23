# ADR 0210 — Relatório de transparência: só números agregados, público, por período livre

**Status:** Provisório (PENDENTE-CESJO P9) (2026-11-23) · Issue #61

## Contexto

O RF016 lista o relatório "de transparência" entre os relatórios estatísticos, sem dizer o que ele
contém nem para quem vai. A pergunta P9 (conteúdo dos relatórios e destinatários) não foi respondida.

O relatório de transparência de uma organização da sociedade civil é prestação de contas à sociedade,
aos financiadores e aos conselhos (por exemplo o de assistência social). Ele costuma ser publicado no
site ou mandado por e-mail, então qualquer dado de pessoa nele vazaria para fora do Centro-Dia.

Alternativas consideradas:

- **Reaproveitar o relatório estatístico mensal (ADR 0200) com outro título**: mostra lista de espera,
  desligamentos por motivo e comunicações, que interessam à gestão do serviço mas não à prestação de
  contas, e deixa de fora parcerias, eventos, doações e equipe, que são o que um financiador procura.
- **Relatório com listas nominais** (doadores, parceiros, eventos com descrição): mais completo, mas
  expõe nomes de pessoas e a descrição cifrada das reuniões, que pode citar usuários (ADR 0175).
- **Só números agregados, num conjunto próprio**: atende a prestação de contas sem dado pessoal e
  reaproveita as APIs e a exportação que já existem.

Para o período, o relatório anual é o mais comum, mas os termos de fomento e convênios prestam contas
pela vigência do termo, que nem sempre coincide com o ano civil.

## Decisão

- `estatisticas.interno.IndicadoresDaTransparencia` é o conteúdo: uma lista de itens, cada um devolve
  uma ou mais `LinhaTransparencia(grupo, indicador, valor, unidade)`. A unidade é `QUANTIDADE` ou
  `REAIS`, porque a doação em dinheiro tem valor (ADR 0178). O marcador `PENDENTE-CESJO(P9)` fica
  nessa lista. A `LinhaRelatorio` do relatório mensal não muda.
- Conteúdo provisório, por grupo:
  - Usuários atendidos: atendidos no período (como na P32), inserções no serviço, ativos no último dia.
  - Lista de espera: novas inscrições e pessoas aguardando vaga no último dia (a demanda não atendida).
  - Atendimentos e atividades: os seis tipos do RF010.
  - Parcerias: parcerias vigentes no período e ações de articulação.
  - Reuniões e eventos institucionais: os sete tipos do RF012.
  - Doações recebidas: quantidade por tipo e o valor total recebido em dinheiro.
  - Doações feitas pelo Centro-Dia: total.
  - Equipe no último dia: colaboradores por função.
  - Ficam de fora: violações de direitos (P33), motivos de desligamento, comunicações e qualquer nome,
    descrição, contraparte ou observação.
- O período é livre: `inicio` e `fim`, datas inclusivas. O fim não pode passar de hoje (422); um
  período até hoje sai parcial. Ano civil inteiro vira o título "Relatório de transparência de 2025";
  outro período leva as datas no título.
- Destinatário provisório: o relatório é público. O sistema só gera e exporta; publicar ou enviar é
  feito fora do sistema pela coordenação.
- Endpoints em `TransparenciaControlador`, com `RELATORIOS_LER` (Coordenador e Consulta, ADR 0196):
  `GET /api/estatisticas/relatorios/transparencia?inicio=&fim=` devolve o JSON sem cache;
  `GET .../transparencia/exportacao?inicio=&fim=&formato=pdf|xlsx` exporta pela base do módulo
  Relatórios (ADR 0040), com colunas Grupo, Indicador e Valor e o período no cabeçalho. O valor em
  reais sai como texto "R$ 1.234,50" para o PDF e a planilha mostrarem a unidade. A base registra
  `EXPORTAR` em `relatorio_transparencia` na auditoria (ADR 0043). Ver na tela não é auditado, como
  no relatório mensal.
- Serviço, controlador e tipos ficam em arquivos próprios (`ServicoTransparencia`,
  `TransparenciaControlador`, `RelatorioTransparencia`), separados dos relatórios estatísticos, para
  o quadrimestral e o anual (issue #59) e o CREAS (issue #60) evoluírem sem conflito.

## Consequências

- Quando a equipe responder a P9, a mudança fica em `IndicadoresDaTransparencia` e, se precisar de um
  número que nenhuma API dá, num método novo na API do módulo dono.
- "Parcerias vigentes" usa a situação ativo/inativo de hoje, porque o parceiro não tem histórico de
  situação: um parceiro inativado depois do período some dos relatórios passados.
- Se a equipe quiser o relatório nominal (doadores que autorizaram divulgação, por exemplo), ele vira
  outro relatório, com permissão própria, e este continua público.
