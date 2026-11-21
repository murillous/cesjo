# ADR 0057 — Ordem e posição na fila da lista de espera

**Status:** Provisório (PENDENTE-CESJO P1) (2026-11-21) · Issue #28

## Contexto

O RF002 pede a fila em ordem cronológica, com a posição de cada candidato. O documento de
requisitos também diz que o serviço prioriza beneficiários do BPC inseridos no CadÚnico, e a
pergunta 1 (como essa prioridade entra na fila) não tem resposta. O ADR 0037 já decidiu que a
posição não tem coluna e sai da ordem de `data_cadastro`, com `criado_em` como desempate.

Alternativas consideradas:

- **Prioridade BPC/CadÚnico agora**: passar na frente ou servir de desempate. O cadastro ainda não
  tem esses campos, e qualquer escolha mudaria posições já comunicadas às famílias.
- **Posição calculada com os filtros aplicados**: filtrar por origem renumeraria a fila e mostraria
  uma posição que não é a real.
- **Posição para qualquer situação**: só quem aguarda está na fila.

## Decisão

- A ordem fica num lugar só, `listaespera.interno.OrdemDaFila`, com o marcador
  `PENDENTE-CESJO(P1)`: `data_cadastro`, depois `criado_em`, depois `id` (só para a ordem ser estável
  na paginação). A listagem e o cálculo da posição usam a mesma ordem.
- A posição conta de 1 em diante, entre todas as inscrições `AGUARDANDO`, sem olhar os filtros.
  Inscrição em outra situação aparece sem posição.
- `GET /api/lista-espera/inscricoes` devolve a fila paginada (padrão de 20, máximo de 100 por
  página) e aceita `nome` (trecho, sem diferenciar maiúsculas; `%` e `_` valem como texto),
  `tipoDeficiencia`, `origem` e `situacao`.
- A listagem não é auditada: não mostra contato, documento nem campo sensível, e o ADR 0023 registra
  só leituras de dado sensível. Cadastro, anexos e download continuam auditados.

## Consequências

- Se a equipe definir a prioridade, muda `OrdemDaFila` (e entram os campos de BPC e CadÚnico no
  cadastro). A posição de todo mundo é recalculada na hora, sem migração.
- A busca por nome não ignora acentos: "Joao" não acha "João". Dá para resolver com a extensão
  `unaccent` do Postgres quando a busca for usada no dia a dia.
