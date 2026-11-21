# ADR 0023 — Leitura de dado sensível e exportação registradas por chamada explícita

**Status:** Aceito (2026-11-20)

## Contexto

A seção 7 do planejamento pede o registro de toda leitura de registro com dado sensível (escuta,
PIA, violação, laudo) e de toda exportação. `EventoAuditoria.leituraSensivel` e
`EventoAuditoria.exportacao` já existem (ADR 0007). Faltava decidir se há um atalho.

Alternativas consideradas:

- Anotação (por exemplo `@RegistraLeituraSensivel(entidade = "escuta", id = "#id")`) tratada por
  AOP: menos código em cada serviço, mas o identificador vem de uma expressão SpEL em texto, sem
  checagem do compilador, e não pega o caso comum de o id só ser conhecido depois da busca.
- Ouvinte de carga do JPA (`@PostLoad`): registra toda carga, inclusive as internas de listagem e
  relatório, e enche o log de ruído.
- Chamada explícita a `Auditoria.registrar` no serviço que entrega o dado.

## Decisão

Sem atalho. O serviço que entrega o dado sensível chama
`auditoria.registrar(EventoAuditoria.leituraSensivel("<entidade>", id))` depois de carregar o
registro e antes de devolvê-lo. A exportação chama `auditoria.registrar(EventoAuditoria.exportacao("<o que foi exportado>"))`
antes de devolver o arquivo. Os dois funcionam em transação só de leitura e fora de transação
(ADR 0021).

## Consequências

- O registro fica visível no código do serviço e é checado pelo compilador.
- Esquecer a chamada é possível; cada issue de módulo com dado sensível precisa de um teste que
  confira o registro de leitura, como faz `AuditoriaIntegracaoTests`.
- Se a repetição incomodar quando os módulos sensíveis entrarem, uma anotação pode ser criada por um
  ADR novo, em cima da mesma API.
