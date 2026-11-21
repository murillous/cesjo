# ADR 0038 — Dados obrigatórios e validação do cadastro na lista de espera

**Status:** Provisório (PENDENTE-CESJO P2) (2026-11-21) · Issue #26

## Contexto

O RF001 lista como obrigatórios: nome da pessoa, nome do responsável, tipo de deficiência, telefone,
origem do encaminhamento e data de cadastro. O serviço atende de 18 a 59 anos, mas a pergunta 2
(exigir data de nascimento e recusar quem está fora da faixa) não tem resposta.

Alternativas consideradas para a idade: exigir a data de nascimento e bloquear fora da faixa (pode
impedir o cadastro de quem ainda vai fazer 18 anos ou de quem o encaminhamento não informou a data);
não ter o campo (perde um dado útil para a triagem).

## Decisão

- Obrigatórios, no backend (`NovaInscricao`) e no frontend: os seis campos do RF001.
- Data de nascimento: opcional, sem bloqueio por idade. Quando informada, precisa ser anterior a hoje.
  O marcador `PENDENTE-CESJO(P2)` fica em `NovaInscricao`.
- Data de cadastro: informada por quem cadastra, sugerida como hoje na tela, nunca futura. Pode ser
  passada para registrar quem já estava na fila em papel, mantendo a ordem.
- Telefone: DDD mais número, 10 ou 11 dígitos, aceito com ou sem máscara e guardado só com os dígitos
  (`pessoas.Telefone`).
- Nomes: até 150 caracteres, sem espaço nas pontas.

## Consequências

- Se a equipe decidir exigir a faixa etária, muda a validação de `NovaInscricao` (e o campo no
  formulário do frontend) e entra uma regra de idade calculada na data de cadastro.
- Cadastros feitos antes disso podem ficar sem data de nascimento; a ficha do candidato (#29) permite
  completar.
