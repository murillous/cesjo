# ADR 0100 — Escuta ativa e qualificada com textos cifrados e leitura auditada

**Status:** Aceito (2026-11-21) · Issue #36

## Contexto

O RF005 registra o que foi colhido na escuta: dinâmica familiar, necessidades imediatas, habilidades
identificadas, potencialidades, data e profissional responsável. Os quatro primeiros são texto livre
sobre a família e a pessoa, dado sensível pela seção 7: cifrado na aplicação e com leitura auditada.

Alternativas consideradas: devolver os textos na lista de escutas da pessoa (toda abertura da página
viraria várias leituras sensíveis no log, sem que ninguém tenha lido nada); exigir os quatro textos
(nem toda escuta cobre todos os temas); escuta sem correção (um erro de digitação ficaria para
sempre).

## Decisão

- O módulo `atendimento` é dono da tabela `escuta` (migração `V30`): `pessoa_id`, `data_escuta`,
  `colaborador_id` (profissional responsável) e os quatro textos, cada um `text` cifrado pelo
  `TextoCifradoConverter`.
- Registro (`POST /api/escutas`, 201): a pessoa precisa existir, a data não pode ser futura (dia em
  `America/Sao_Paulo`), o profissional precisa estar ativo, e pelo menos um dos quatro textos precisa
  estar preenchido (até 10.000 caracteres cada).
- Correção (`PUT /api/escutas/{id}`): mesmas regras. Um profissional trocado precisa estar ativo; o
  de antes pode já ter ficado inativo. A auditoria guarda só os nomes das colunas que mudaram.
- `GET /api/escutas?pessoaId=` lista as escutas da pessoa, da mais recente para a mais antiga, só com
  data e profissional, sem registro no log. Os textos saem só em `GET /api/escutas/{id}`, e cada
  leitura é registrada como `LER_SENSIVEL`.
- `GET /api/escutas/profissionais` devolve os colaboradores ativos e sugere o ligado à conta logada
  (`Colaboradores.obterAtivoPorConta`, ADR 0046).
- A escuta não é excluída.

## Consequências

- O PIA (issue #37) pode citar as escutas da pessoa sem copiar os textos.
- Busca por palavra nos textos da escuta não é possível, porque eles estão cifrados.
