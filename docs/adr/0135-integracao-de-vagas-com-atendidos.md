# ADR 0135 — Vagas ocupadas pelos atendidos ativos e aviso de vaga no desligamento

**Status:** Aceito (2026-11-22) · Issue #94

## Contexto

Os PRs #83 (#39), #89 (#30), #92 (#31) e o da #42 foram feitos em paralelo. Depois dos merges, três
ligações ficaram de fora, como os ADRs 0093, 0095 e 0111 já previam:

- a ocupação de vagas (`listaespera.OcupacaoDeVagas`) ainda usava a implementação padrão, que conta
  inscrições `INSERIDO` e não enxerga os desligamentos;
- o evento `UsuarioDesligado` era publicado e ninguém ouvia, então a vaga aberta por desligamento não
  gerava o aviso do RF003;
- `ListaEspera.registrarInsercao` mudava a inscrição para `INSERIDO` sem gravar a linha em
  `historico_situacao_inscricao` (ADR 0091), e a ficha do candidato não mostrava a inserção.

Alternativas consideradas:

- **Manter a implementação padrão com `@ConditionalOnMissingBean`**: a condição em classe de
  configuração da aplicação depende da ordem em que os beans são lidos, e o padrão fica sem uso assim
  que Atendidos fornece a sua. Código morto que pode voltar a valer por engano.
- **Ouvinte assíncrono ou depois do commit**: não há registro de publicações (ADR 0111), então um
  aviso perdido não volta. O ouvinte síncrono desfaz a notificação junto com o desligamento.
- **Gravar o histórico em Atendidos**: a tabela é da Lista de espera; a fronteira do módulo seria
  quebrada.

## Decisão

- `atendidos.interno.OcupacaoPelosAtendidosAtivos` implementa `OcupacaoDeVagas` com
  `UsuariosAtendidos.contarAtivos()`. A implementação padrão (`OcupacaoPelasInscricoesInseridas` e
  `VagasConfig`) sai do código. A direção de dependência continua Atendidos para Lista de espera.
- `atendidos.interno.AvisoDeVagaNoDesligamento` ouve `UsuarioDesligado` com `@EventListener` e chama
  `AberturaDeVaga.vagasLiberadas(1)` na transação do desligamento, depois de a inserção estar
  desligada.
- `ListaEspera.registrarInsercao(inscricaoId, dataInsercao)` ganha a data da inserção e grava o
  histórico (situação anterior, `INSERIDO`, data da inserção, autor), com a criação auditada como as
  outras linhas do histórico.

## Consequências

- O painel de vagas e o aviso de vaga aberta passam a contar inserções ativas: desligar libera a vaga
  na hora e, se as disponíveis saírem de zero, Coordenador e Administrativo são avisados (ADR 0095).
- Testes da Lista de espera que precisam de vaga ocupada criam uma inserção ativa, não só uma
  inscrição `INSERIDO`.
- O teste `VagaLiberadaNoDesligamentoTests` cobre o fluxo inserir, desligar e notificar.
