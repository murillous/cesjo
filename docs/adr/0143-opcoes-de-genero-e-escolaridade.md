# ADR 0143 — Opções de gênero e de escolaridade

**Status:** Provisório (PENDENTE-CESJO P21) (2026-11-22) · Issue #40

## Contexto

O RF007 pede gênero, mas não diz as opções; escolaridade entrou no cadastro completo pela P6. As
duas listas viram contagens em relatórios e no dashboard (#56), então precisam ser fechadas.

Alternativas consideradas: texto livre (não agrupa); listas longas, como as do IBGE ou do CadÚnico
(mais do que a equipe pediu); listas curtas, com "não informado" quando vazio.

## Decisão

- Gênero (`pessoas.Genero`): feminino, masculino e outro. Vazio é "não informado".
- Escolaridade (`pessoas.Escolaridade`): sem escolaridade, fundamental incompleto e completo, médio
  incompleto e completo, superior incompleto e completo. Vazio é "não informada".
- As listas ficam no enum, na restrição da V40 e em `rotulos.ts` do módulo atendidos (frontend).

## Consequências

- Mudar uma opção pede migração nova da restrição `ck_cadastro_genero` ou `ck_cadastro_escolaridade`,
  além do enum e do `rotulos.ts`.
