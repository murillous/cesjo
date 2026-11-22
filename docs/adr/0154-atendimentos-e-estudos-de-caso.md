# ADR 0154 — Atendimentos e estudos de caso ligados a um usuário, com leitura auditada (provisório)

**Status:** Aceito (2026-11-22) · Issue #46 · Provisório no ponto P25

## Contexto

O RF010 lista também atendimentos multiprofissionais, atendimentos técnicos individualizados e
estudos de caso. A issue #46 pede que sejam ligados a um usuário, com texto cifrado e leitura
auditada. O relato de um atendimento é dado sensível como a escuta (seção 7): toda leitura vai para o
log.

Alternativas consideradas:

- Tabela própria para atendimentos. Os campos são os mesmos da atividade (ADR 0150), e o dashboard
  e o relatório mensal contam os seis tipos juntos.
- Auditar a leitura de toda atividade. As coletivas não são registro individual, e o log ficaria
  cheio de leituras sem dado sensível.
- Qualquer número de profissionais no atendimento multiprofissional. O nome do tipo diz "mais de um
  profissional", mas a equipe não confirmou (pergunta P25).

## Decisão

- Três valores novos em `TipoAtividade`: `ATENDIMENTO_MULTIPROFISSIONAL`,
  `ATENDIMENTO_TECNICO_INDIVIDUALIZADO` e `ESTUDO_DE_CASO`, na mesma tabela `atividade`. A migração
  `V46` troca a restrição `ck_atividade_tipo`.
- Cada tipo diz se é individual (`TipoAtividade.individual()`) e quantos profissionais exige, no
  mínimo (`TipoAtividade.minimoProfissionais()`).
- Os tipos individuais têm exatamente um participante, o usuário atendido. A descrição já é cifrada
  em todos os tipos (ADR 0150).
- O atendimento multiprofissional exige pelo menos dois profissionais. **Provisório (P25).**
- `GET /api/atividades/{id}` de um tipo individual registra `LER_SENSIVEL` em `atividade`. A
  listagem não traz a descrição e não é auditada.
- Permissões iguais às das atividades (ADR 0152): Coordenador e Equipe Técnica registram e leem.
- Telas: o seletor de tipo separa "Atividades coletivas" de "Atendimentos e estudos de caso". Nos
  individuais, a ficha avisa que o conteúdo é sigiloso e que a abertura fica no log, e confere
  antes de enviar a regra de um usuário e o mínimo de profissionais. Na lista, o tipo leva um
  cadeado.

## Consequências

- `Atividades.contarPorTipo` (ADR 0151) já conta os seis tipos.
- Se o Administrativo passar a ler atividades (P17), a leitura dos tipos individuais precisará de
  uma permissão própria.
