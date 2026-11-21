# ADR 0130 — Exportação da lista de espera em PDF e Excel

**Status:** Provisório (PENDENTE-CESJO P8, P17) (2026-11-21) · Issue #32

## Contexto

A seção 5.2 do documento de requisitos pede exportar a lista de espera em PDF e Excel. A issue #32
pede que a exportação respeite os filtros da tela, tenha identidade institucional e seja auditada. A
base comum está no módulo Relatórios (PR #80, ADRs 0040 a 0044): cada módulo monta uma
`TabelaExportacao`, cria a sua permissão e expõe o seu `GET`. A matriz da seção 6 dá ao Consulta só
"dados básicos" da lista de espera (P8), e não diz nada sobre exportar (P17).

Alternativas consideradas para o Consulta:

- **Exportar só os dados básicos**: o mesmo recorte da tela, mas uma planilha com a fila inteira
  sai do sistema e circula fora do controle de acesso.
- **Não exportar**: o Consulta continua vendo a fila na tela, e a extração em massa fica com quem
  trabalha com os candidatos.

## Decisão

- Permissão nova `LISTA_ESPERA_EXPORTAR`, com o marcador `PENDENTE-CESJO(P8)` na linha do `CONSULTA`
  em `acesso.Perfil`:

| Permissão | Coordenador | Equipe Técnica | Administrativo | Consulta |
|---|---|---|---|---|
| `LISTA_ESPERA_EXPORTAR` | sim | sim | sim | não (P8, P17) |

- `GET /api/lista-espera/exportacao?formato=pdf|xlsx` aceita os mesmos filtros da fila (`nome`,
  `tipoDeficiencia`, `origem` e `situacao`) e usa a mesma consulta, em `FilaListaEspera.todos`, sem
  paginação e na ordem de `OrdemDaFila`.
- Colunas: posição, nome, tipo de deficiência, origem do encaminhamento, data de cadastro, situação e
  cadastro (completo ou incompleto). Não saem telefone, responsável, data de nascimento nem
  documentos, que não estão na fila.
- Os filtros usados saem no cabeçalho do arquivo, com os rótulos da tela. O título é "Lista de
  espera", e a auditoria registra `EXPORTAR` na entidade `lista_espera`, pela base (ADR 0043).
- Frontend: o botão "Exportar fila" (`app-botao-exportar`) fica no cabeçalho da fila para quem tem a
  permissão, e manda os filtros da última busca.

## Consequências

- Se a equipe quiser o contato na exportação (por exemplo, para ligar para a fila), entram colunas
  em `ExportacaoListaEspera`. Isso pede uma decisão sobre quem pode exportar dado de contato.
- Se o Consulta precisar exportar, ele ganha `LISTA_ESPERA_EXPORTAR`, e `ExportacaoListaEspera`
  passa a cortar as colunas fora dos dados básicos, como `ItemFila.basico()` faz na tela.
