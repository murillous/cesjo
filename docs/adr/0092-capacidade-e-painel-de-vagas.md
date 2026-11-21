# ADR 0092 — Capacidade do serviço e painel de vagas

**Status:** Provisório (PENDENTE-CESJO P3) (2026-11-21) · Issue #30

## Contexto

O RF003 pede que o Coordenador defina a capacidade do serviço e que o sistema mostre as vagas
disponíveis (capacidade menos inserções ativas) e o tamanho da fila. A pergunta 3 (capacidade total
ou separada por turno: matutino, vespertino, integral) não tem resposta. A matriz da seção 6 dá
"editar" ao Coordenador e "ler" aos outros três perfis.

Alternativas consideradas:

- **Capacidade por turno já agora**: três números e uma regra de qual turno cada inserção ocupa, sem
  saber se a equipe trabalha assim. O integral ocupa uma vaga em cada turno ou uma vaga própria?
- **Capacidade como configuração do sistema** (variável de ambiente): sem histórico, e só quem
  administra o servidor poderia mudar.
- **Uma linha que é atualizada**: perde quando e por quem cada capacidade foi definida.

## Decisão

- Uma capacidade total, com o marcador `PENDENTE-CESJO(P3)` em `ServicoVagas.painel`.
- Tabela `capacidade_servico` (migração `V8`), só de inserção: `vagas` (de 0 a 10000),
  `definida_por` e `definida_em`. A vigente é a mais recente. Cada definição é auditada como `CRIAR`.
- Permissões novas:

| Permissão | Coordenador | Equipe Técnica | Administrativo | Consulta |
|---|---|---|---|---|
| `CAPACIDADE_LER` | sim | sim | sim | sim |
| `CAPACIDADE_EDITAR` | sim | não | não | não |

- `GET /api/lista-espera/vagas` (`CAPACIDADE_LER`) devolve `capacidade` (nula enquanto não for
  definida), `ocupadas`, `disponiveis` (capacidade menos ocupadas, nunca negativo, e zero sem
  capacidade), `tamanhoFila` (inscrições ativas: aguardando e em avaliação, ADR 0094) e `definidaEm`.
- `PUT /api/lista-espera/capacidade` (`CAPACIDADE_EDITAR`) com `{ "vagas": n }` grava a nova
  capacidade e devolve o painel.
- Frontend: o painel fica no topo da fila, com as vagas disponíveis em destaque. O Coordenador muda
  a capacidade ali mesmo.

## Consequências

- Se a equipe decidir por turno, entra uma coluna `turno` em `capacidade_servico` (numa migração
  nova), o painel passa a mostrar um número por turno e `OcupacaoDeVagas` (ADR 0093) conta por turno.
- O histórico de capacidades fica no banco e no log de auditoria, mas não tem tela.
