# ADR 0033 — O que é auditado nas notificações

**Status:** Aceito (2026-11-20)

## Contexto

O RNF 3.3 pede registro de toda criação, alteração e exclusão (seção 7 do planejamento). Notificação
não tem dado sensível (ADR 0031), então listar e contar não entram na regra de leitura sensível.
A criação normalmente nasce de uma operação de negócio que já é auditada pelo seu módulo, o que
poderia tornar o registro redundante.

Alternativas consideradas: auditar só a operação de origem; auditar também a criação e a leitura;
registrar "marcar todas" como um único evento sem identificador.

## Decisão

Pela API `Auditoria`:

- Criação: `EventoAuditoria.criacao("notificacao", id)`, uma por notificação criada (uma por perfil em
  `notificarPerfis`). A conta é a da requisição, ou nenhuma quando a criação vem de tarefa agendada.
- Marcar como lida: `EventoAuditoria.alteracao("notificacao", id, {"lida_em"})`, uma por notificação
  efetivamente marcada pela conta. "Marcar todas" gera um evento por notificação marcada. Marcar de
  novo uma já lida não gera evento.
- Listar e contar não são auditados.

## Consequências

- O log mostra quem criou e quem leu cada aviso, o que ajuda a saber se a vaga aberta foi vista.
- "Marcar todas" com muitas notificações gera muitos eventos; com o volume esperado do Centro-Dia, isso
  é aceitável.
