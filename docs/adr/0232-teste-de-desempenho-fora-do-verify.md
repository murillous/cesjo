# ADR 0232 — Teste de desempenho fora do `verify` normal, com perfil Maven e execução manual

**Status:** Aceito (2026-11-23) · Issue #25

## Contexto

O teste de desempenho grava milhões de linhas e leva alguns minutos. Rodar isso a cada push e PR
deixaria a CI lenta e instável, porque o tempo depende da máquina e do que mais estiver rodando nela.
Ao mesmo tempo, o teste precisa ser fácil de repetir quando o volume ou as consultas mudarem.

Alternativas consideradas:

- **Rodar sempre no `./mvnw verify`**: cobre toda mudança, mas deixa a CI vários minutos mais lenta e
  falha por ruído da máquina.
- **Script separado fora do Maven**: não reaproveita o contexto do Spring nem a configuração dos
  testes.
- **Tag JUnit excluída por padrão e um perfil Maven que roda só ela**, com um workflow manual no
  GitHub Actions.

## Decisão

- O teste tem a tag JUnit `desempenho`. O `maven-surefire-plugin` exclui essa tag por padrão
  (propriedade `testes.grupos-excluidos`).
- O perfil `desempenho` inverte: roda só a tag. Comando: `cd backend && ./mvnw verify -Pdesempenho`.
  Os parâmetros `desempenho.fator`, `desempenho.usuarios` e `desempenho.rodadas` mudam a massa e a
  carga (`-Ddesempenho.fator=1` para um teste rápido).
- O workflow `.github/workflows/desempenho.yml` (`workflow_dispatch`) roda o perfil num runner do
  GitHub com Docker e publica a tabela no resumo da execução e como artefato.

## Consequências

- O `./mvnw verify` normal e a CI não mudam de tempo.
- Uma regressão de desempenho só aparece quando alguém roda o perfil. Vale rodar antes de cada
  entrega e depois de mudanças em listagens, relatórios e dashboards.
- O runner do GitHub tem poucos processadores; os tempos de lá não são comparáveis com os de uma
  máquina de desenvolvimento.
