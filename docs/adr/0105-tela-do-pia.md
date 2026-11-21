# ADR 0105 — Tela de elaboração e leitura do PIA

**Status:** Aceito (2026-11-21) · Issue #37

## Contexto

O PIA é longo e lido em reuniões com a família e a equipe; também é editado pelo celular. A
estrutura é provisória (ADR 0103).

## Decisão

- O PIA fica no atendimento da pessoa (ADR 0102): a seção "Plano de atendimento (PIA)" lista as
  versões, da mais nova para a mais antiga, com a atual em destaque e o status de cada uma. Sem
  PIA, quem tem `PIA_EDITAR` vê "Elaborar PIA".
- `/atendimento/pessoas/:pessoaId/pia/novo` elabora; `/atendimento/pias/:id` abre uma versão
  (leitura auditada), com o aviso de sigilo.
- A leitura mostra o plano como documento: diagnóstico, objetivos numerados (a ordem é a
  prioridade) com metas, ações, prazo e responsável, e observações.
- No formulário, cada objetivo é um bloco com borda à esquerda, com botão de remover; "Acrescentar
  objetivo" fica no fim da lista. No celular os campos ficam um embaixo do outro.
- "Editar PIA" só aparece na versão mais nova em elaboração. Versão substituída avisa que é só para
  consulta.

## Consequências

- O formulário reflete `ConteudoPia`. Quando o modelo oficial chegar, os dois mudam juntos.
