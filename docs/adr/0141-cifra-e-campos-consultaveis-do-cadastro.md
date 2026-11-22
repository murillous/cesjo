# ADR 0141 — O que é cifrado e o que fica consultável no cadastro completo

**Status:** Aceito (2026-11-22) · Issue #40

## Contexto

O cadastro completo (ADR 0140) tem documentos, endereço, dados socioeconômicos, crença religiosa e
saúde. O ADR 0004 manda cifrar na aplicação o texto livre sensível e deixar em claro os campos
categóricos, protegidos por perfil, auditoria de leitura e disco cifrado, para relatórios agregarem
em SQL. O dashboard (#56) vai contar usuários por tipo de deficiência, gênero, faixa etária e turno.

Alternativas consideradas: cifrar tudo (o dashboard não conta nada em SQL); cifrar só a saúde
(documentos ficariam legíveis num vazamento do banco); a regra do ADR 0004 aplicada campo a campo.

## Decisão

- **Cifrados** com `TextoCifradoConverter` (colunas `text`, formato v1 do ADR 0025): CPF, NIS, RG,
  medicamentos de uso contínuo e outras informações de saúde.
- **Em claro**: gênero, escolaridade, endereço (CEP, logradouro, número, complemento, bairro, cidade),
  renda familiar, benefícios e crença religiosa. A crença é categórica, com as opções do RF007
  (católica, evangélica, testemunha de Jeová, matriz africana, outras), e segue o ADR 0004 e a seção
  1 do planejamento: em claro, mas lida e editada só com `ATENDIDOS_DADOS_SENSIVEIS`, com leitura
  auditada (ADR 0142).
- Para o dashboard: gênero e bairro têm índice em `cadastro_complementar`; a faixa etária continua
  calculada da `pessoa.data_nascimento`, em claro (ADR 0080); tipo de deficiência e turno já estão em
  `pessoa` e `insercao`. A contagem é um `join` de `insercao`, `pessoa` e `cadastro_complementar`,
  como a `ConsultaAtendidos` já faz.

## Consequências

- Documentos e textos de saúde não são pesquisáveis nem ordenáveis.
- Quem ler as tabelas direto (relatórios, dashboard) não deve expor a crença sem conferir
  `ATENDIDOS_DADOS_SENSIVEIS`.
