# ADR 0043 — Auditoria e permissão da exportação

**Status:** Aceito (2026-11-21) · Issue #55

## Contexto

A seção 7 do planejamento manda auditar toda exportação, e o ADR 0023 diz que a exportação chama
`EventoAuditoria.exportacao("<o que foi exportado>")` antes de devolver o arquivo. Com a peça comum,
faltava decidir quem registra (a base ou cada módulo), em que momento e quem pode exportar.

## Decisão

- Quem registra é a implementação de `Exportacao`, não o módulo que chama: toda exportação passa por
  ela, então nenhuma fica sem registro. O módulo informa só o nome do que exporta, no formato de
  entidade da auditoria (por exemplo `lista_espera`).
- O registro é feito depois de gerar o arquivo e antes de devolvê-lo. Falha na geração não deixa
  registro de algo que não saiu. Falha na auditoria lança exceção e o arquivo não sai (ADR 0021).
- O registro guarda conta, IP, ação `EXPORTAR`, entidade e hora, como os demais (ADR 0020). Não guarda
  formato, filtros nem conteúdo: o log nunca leva valores.
- A base não cria permissão nem endpoint. Cada módulo cria a sua permissão de exportar quando a
  exportação dele entrar (por exemplo, a lista de espera na #32), conforme a matriz da seção 6 e o
  ADR 0015, e os pontos em aberto seguem a pergunta P17.

## Consequências

- O teste de integração da base confere o registro; os módulos não precisam repetir esse teste, só o
  da permissão.
- O log diz quem exportou o quê e quando, mas não em que formato nem com que filtros. Se a equipe
  precisar disso, `EventoAuditoria` ganha um campo novo por ADR próprio.
