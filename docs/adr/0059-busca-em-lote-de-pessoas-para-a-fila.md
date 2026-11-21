# ADR 0059 — Busca em lote de pessoas para montar a fila

**Status:** Aceito (2026-11-21) · Issue #28

## Contexto

A fila junta dados da inscrição (módulo `listaespera`: data, origem, situação) e da pessoa (módulo
`pessoas`: nome, tipo de deficiência, documentos). A busca por nome e o filtro por tipo são sobre a
pessoa, e a paginação precisa vir depois desses filtros. O ADR 0037 deixou duas saídas: uma busca em
lote na API `Pessoas` ou uma consulta própria.

Alternativas consideradas:

- **Consulta SQL na lista de espera juntando `inscricao_lista_espera` e `pessoa`**: uma ida ao banco
  e paginação no banco, mas a lista de espera passaria a depender das tabelas de outro módulo. O teste
  do Spring Modulith não pega dependência por SQL, então a fronteira quebraria em silêncio.
- **Visão de leitura (`view`) mantida pelo módulo Pessoas**: preserva a fronteira e pagina no banco,
  mas cria um contrato de banco entre módulos cedo demais para o volume esperado.
- **Busca em lote pela API Java**: a lista de espera manda os `pessoa_id` das inscrições e recebe só
  os que atendem ao filtro.

## Decisão

- A API `Pessoas` ganha `resumos(Collection<UUID> ids, FiltroPessoas filtro)`, que devolve um mapa
  `id -> ResumoPessoa` (nome, tipo de deficiência e tipos de documento anexados), só com as pessoas
  que atendem ao filtro de nome e tipo. A consulta vai em lotes de 1000 ids e não é auditada, porque
  não expõe contato nem documento.
- `FilaListaEspera` (módulo `listaespera`) busca as inscrições com os filtros de origem e situação
  na ordem de `OrdemDaFila`, pede os resumos em lote, descarta as inscrições sem resumo e pagina em
  memória.

## Consequências

- A fronteira entre os módulos continua só por API Java, verificada pelo Spring Modulith.
- Cada listagem lê todas as inscrições que passam nos filtros da própria lista de espera. Para o
  porte de um Centro-Dia (centenas de candidatos) isso cabe no limite de 3 segundos do RNF. Se a fila
  passar de alguns milhares, a troca é para a visão de leitura do módulo Pessoas, sem mudar a API HTTP.
