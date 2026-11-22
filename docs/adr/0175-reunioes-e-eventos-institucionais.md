# ADR 0175 — Reuniões e eventos institucionais

**Status:** Aceito (2026-11-22) · Issue #52

## Contexto

O RF012 pede o registro de atividades institucionais de sete tipos: reuniões entre coordenação e
equipe técnica, formações multiprofissionais, reuniões com a direção geral, monitoramento do órgão
gestor, visitas institucionais (realizadas ou recebidas), eventos (realizados ou participados) e
reuniões com parceiros. O planejamento esboça `evento_institucional` com tipo, direção (quando
couber), data, descrição e parceiro opcional. A issue pede também os participantes.

Alternativas consideradas:

- Participantes como lista de colaboradores: deixa de fora os participantes de fora (técnicos do
  CREAS, da direção, de parceiros), que são a maioria nas visitas e eventos.
- Direção livre para todos os tipos: permitiria "reunião com a direção recebida", que o RF012 não
  prevê.
- Aceitar datas futuras para agendar reuniões: o RF012 fala em registrar o que aconteceu, e a
  indicação "realizado" não faz sentido no futuro.
- Descrição em texto aberto no banco: reuniões entre coordenação e equipe técnica costumam discutir
  casos de usuários atendidos.

## Decisão

- Tabela `evento_institucional` no módulo `institucional` (migração `V57`).
- Tipo é o enum `TipoEventoInstitucional`, com os sete tipos na ordem do RF012. A direção é o enum
  `DirecaoEvento` (`REALIZADO`, `RECEBIDO`, `PARTICIPADO`), obrigatória e restrita conforme o tipo:
  visita aceita `REALIZADO` ou `RECEBIDO`; evento aceita `REALIZADO` ou `PARTICIPADO`; os demais
  tipos não têm direção. A regra fica no próprio enum (`aceita`) e é repetida no
  `ck_evento_direcao`.
- Data obrigatória e não futura. Descrição obrigatória (até 4000 caracteres), cifrada na aplicação
  (formato v1 do ADR 0025). Participantes em texto livre opcional (até 2000), sem cifra.
- Parceiro opcional (`parceiro_id`, ADR 0172), que precisa existir; pode ser parceiro inativo, para
  registrar algo antigo. A resposta da API traz o nome do parceiro.
- API: `GET /api/eventos-institucionais` (paginada, do mais recente ao mais antigo, filtros
  opcionais `tipo`, `de`, `ate` e `parceiroId`), `GET /{id}`, `POST` (201) e `PUT /{id}`. Sem
  exclusão.
- Auditoria: criação e alteração com só os nomes dos campos que mudaram. A leitura não é auditada:
  o log de leituras fica para escuta, PIA, violações e laudos (seção 7 do planejamento).

## Consequências

- Os relatórios do RF016 podem contar reuniões e eventos por tipo e período pelos índices
  `ix_evento_tipo_data` e `ix_evento_data`.
- A descrição cifrada não pode ser buscada por texto no banco; a busca é por tipo, período e
  parceiro.
- Mudar a lista de tipos exige migração nova e mudar o `rotulos.ts` do módulo eventos.
