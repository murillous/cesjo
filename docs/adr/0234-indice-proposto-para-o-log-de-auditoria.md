# ADR 0234 — Índice proposto para a consulta do log de auditoria por login

**Status:** Substituído por ADR 0235 (2026-11-23) · Issue #25

## Contexto

No teste de desempenho (ADR 0231), a consulta do log de auditoria é a mais lenta: o log é a maior
tabela (2 milhões de linhas e cerca de 800 MB na massa de 10 vezes, ADR 0230) e a listagem paginada
conta o total de registros a cada página. Todas as consultas ficaram abaixo de 3 segundos, mas a
auditoria filtrada por login tem o maior tempo (máximo de 2,7 segundos sob carga, p50 de 1,5 s).

O `explain (analyze, buffers)` na massa mostrou:

- **Filtro por login**: o índice `auditoria_login_idx (lower(conta_login), ocorrido_em desc)` acha
  as 100 mil linhas do login, mas a contagem lê a tabela (Bitmap Heap Scan, 42 mil páginas, cerca de
  285 ms), porque o índice de expressão não guarda as colunas que a consulta pede. A página em si,
  pelo mesmo índice, leva cerca de 1 ms.
- **Sem filtro**: a contagem de 2 milhões de linhas leva cerca de 160 ms (Parallel Seq Scan). Nenhum
  índice muda isso; o custo cresce com o log.
- **Entidade, ação e período**: cerca de 100 ms para contar e 200 ms para a página quando a
  combinação tem poucas linhas, porque o Postgres percorre o índice de data filtrando entidade e ação.

A issue #25 não pode criar migração. Esta decisão registra a proposta para uma issue futura.

## Decisão

Proposta, sem migração nesta issue:

```sql
CREATE INDEX auditoria_login_cobertura_idx
    ON auditoria (lower(conta_login), ocorrido_em DESC, id DESC) INCLUDE (conta_login);
DROP INDEX auditoria_login_idx;
```

- O novo índice cobre a contagem e a página do filtro por login (Index Only Scan, sem ler a tabela)
  e substitui o atual. Medido na massa: a contagem cai de cerca de 285 ms para 15 a 25 ms; sob a
  carga de 20 usuários, a auditoria por login cai de p50 1,5 s e máximo 2,7 s para p50 130 ms e
  máximo 334 ms (`-Ddesempenho.indices-propostos=true`, `docs/desempenho.md`).
- Se o log crescer muito além da massa testada, a próxima opção é não contar o total exato na
  listagem da auditoria (mostrar "mais de N" ou paginar por cursor), o que muda a API e a tela.

## Consequências

- Enquanto a proposta não entra, a meta de 3 segundos continua atendida com folga na massa de 10
  vezes, segundo o teste.
- O índice novo ocupa um pouco mais de espaço que o atual e deixa cada inserção no log um pouco mais
  cara, o que é irrelevante no volume esperado.
- Quem implementar deve rodar `./mvnw verify -Pdesempenho` antes e depois e atualizar
  `docs/desempenho.md`.
