# ADR 0058 — Dados básicos da fila para o perfil Consulta

**Status:** Provisório (PENDENTE-CESJO P8) (2026-11-21) · Issue #28

## Contexto

A matriz da seção 6 dá ao perfil Consulta "dados básicos" da lista de espera. O planejamento define
dados básicos como nome, situação, turno e datas, sem contato, documentos ou campos sensíveis, mas a
pergunta 8 (o que são as "informações básicas") não tem resposta. O ADR 0039 deixou o Consulta sem
`LISTA_ESPERA_LER`, a permissão da ficha completa.

Alternativas consideradas:

- **Consulta sem a fila até a resposta**: mais conservador, mas contraria a matriz, que dá acesso.
- **Consulta com a fila completa**: mostraria tipo de deficiência, que é dado de saúde, e origem do
  encaminhamento (judiciário, CREAS), que revela a situação da família.
- **Filtrar por tipo ou origem sem mostrar o campo**: o filtro revela o dado do mesmo jeito.

## Decisão

- Permissão nova `LISTA_ESPERA_FILA_LER`: ver a fila só com os dados básicos. Todos os perfis têm.
  O marcador `PENDENTE-CESJO(P8)` fica na linha do `CONSULTA` em `acesso.Perfil`.

| Permissão | Coordenador | Equipe Técnica | Administrativo | Consulta |
|---|---|---|---|---|
| `LISTA_ESPERA_FILA_LER` (fila com dados básicos) | sim | sim | sim | sim (P8) |

- Dados básicos da fila: id, posição, nome, situação e data de cadastro. Ficam de fora tipo de
  deficiência, origem do encaminhamento e se o cadastro está completo. Contato, responsável e
  documentos não aparecem na fila para ninguém.
- Com `LISTA_ESPERA_FILA_LER` e sem `LISTA_ESPERA_LER`, filtrar por `tipoDeficiencia` ou `origem`
  responde 403. Busca por nome e filtro por situação funcionam.
- O corte é feito no controlador, que monta o item completo e o reduz com `ItemFila.basico()`. No
  frontend, o módulo Lista de espera passa a exigir `LISTA_ESPERA_FILA_LER`; a página de documentos
  exige `LISTA_ESPERA_LER`.

## Consequências

- O Consulta vê a fila no menu e não chega à ficha, aos documentos nem ao cadastro.
- Se a equipe ampliar ou reduzir os dados básicos, muda `ItemFila.basico()` e a checagem de filtros
  em `ListaEsperaControlador.listar`. Se decidir que o Consulta não vê a fila, sai
  `LISTA_ESPERA_FILA_LER` da linha do `CONSULTA`.
