# ADR 0205 — Relatório de intervenção (CREAS): casos de violação do período e intervenções

**Status:** Provisório (PENDENTE-CESJO P9) (2026-11-23) · Issue #60

## Contexto

O RF016 lista o relatório "de intervenção (CREAS)" entre os relatórios estatísticos, sem dizer o que
ele contém. A pergunta P9 (o que contém e para quem vai) não foi respondida. O CREAS é o serviço da
assistência social que acompanha as situações de violação de direitos. Um relatório de intervenção
para ele trata dessas situações e do que o Centro-Dia fez em cada uma.

O que o sistema já registra e serve para isso:

- violações de direitos por usuário atendido, com categoria, âmbito, data de identificação e uma
  descrição cifrada, de leitura auditada (RF009, ADR 0112 e ADR 0145);
- atendimentos individuais ligados a um usuário: atendimento multiprofissional, técnico
  individualizado e estudo de caso (RF010, ADR 0154);
- encaminhamentos enviados e recebidos, mas sem ligação com o usuário: a contraparte é texto livre
  (RF011, P30, ADR 0160 e ADR 0164);
- inserções e desligamentos com motivo (RF007, RF008).

Alternativas consideradas:

- **Só contagens, como o relatório mensal**: segue "dados quantitativos" do RF016 ao pé da letra e
  não traz dado pessoal. Mas o CREAS acompanha casos, e uma contagem por categoria não diz a ele de
  quem se trata nem o que foi feito.
- **Um caso por violação, com a descrição**: é o mais completo, mas tira em massa, num arquivo, o
  texto mais sensível do sistema, que hoje só se lê um por vez e com auditoria.
- **Um caso por usuário, sem a descrição, mais um resumo do período**: diz quem teve violação
  identificada, de que categoria, quantos atendimentos teve e como está no serviço. A narrativa do
  caso continua com a equipe técnica, que abre a descrição na ficha quando precisa.

## Decisão

- O relatório é de um período livre (`inicio` e `fim`, as duas datas inclusive), até hoje. A tela
  sugere o mês anterior.
- **Casos**: um por usuário com violação identificada no período (pela data de identificação), em
  ordem de nome. Cada caso traz:
  - as violações do período: categoria, âmbito (quando a categoria tem) e data de identificação, sem a
    descrição;
  - os atendimentos individuais do usuário com data no período e a data do último;
  - a situação no serviço no fim do período, pela inserção mais recente até lá: "Ativo no serviço",
    "Desligado em dd/mm/aaaa: motivo" ou "Sem inserção no serviço".
- **Resumo do período**: usuários com violação e total de violações; violações por categoria (todas
  as 12, com zero); atendimentos individuais aos usuários com violação; usuários com violação sem
  atendimento no período; encaminhamentos enviados e recebidos no período, de todos os usuários,
  porque o encaminhamento não é ligado ao usuário (P30); usuários com violação desligados até o fim
  do período.
- O conteúdo fica num lugar só, `estatisticas.interno.ConteudoDoRelatorioDeIntervencao`, com o
  marcador `PENDENTE-CESJO(P9)`. Lê os módulos pelas APIs Java públicas (ADR 0195), com duas APIs
  novas, cada uma num arquivo próprio para não mexer nas interfaces que outras frentes usam:
  - `atendidos.CasosDeViolacao.casosNoPeriodo` (em `atendidos.interno.ConsultaCasosDeViolacao`):
    SQL nas tabelas do módulo, junta `pessoa` só para o nome e não lê a descrição;
  - `atividades.AtendimentosIndividuais.resumirPorPessoa` (em
    `atividades.interno.ConsultaAtendimentosIndividuais`): quantidade e último atendimento por pessoa.
- Endpoints: `GET /api/estatisticas/relatorios/intervencao?inicio=&fim=` devolve o relatório em
  JSON, sem cache; `GET .../intervencao/exportacao?inicio=&fim=&formato=pdf|xlsx` exporta pela base
  (ADR 0040). Fim depois de hoje ou antes do início responde 422.
- Na exportação, a tabela tem uma linha por caso (Usuário atendido, Violações identificadas,
  Atendimentos no período, Último atendimento, Situação no fim do período). O cabeçalho traz o
  período e os totais do resumo, menos as categorias, que já estão nas linhas.

## Consequências

- Quando a equipe mandar o modelo (P9), a mudança fica em `ConteudoDoRelatorioDeIntervencao` e, se
  precisar de um dado novo, num método das APIs novas.
- Se a equipe quiser os encaminhamentos por usuário, a comunicação precisa ganhar o vínculo com a
  pessoa (P30).
- A descrição da violação não sai em massa. Se o CREAS precisar dela no relatório, a decisão de LGPD
  muda e a leitura em lote precisa de auditoria própria.
