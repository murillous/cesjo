# ADR 0083 — Listagem e ficha dos usuários atendidos

**Status:** Provisório (PENDENTE-CESJO P8) (2026-11-21) · Issue #41

## Contexto

O item 32 do backlog pede a listagem paginada dos usuários atendidos com filtros por turno, tipo de
deficiência, faixa etária, gênero e bairro, a ficha reunindo inscrição, visitas, escutas e PIA, e só
dados básicos para o perfil Consulta. O ADR 0082 já deu ao Administrativo e ao Consulta só
`ATENDIDOS_LER_BASICO`, e o ADR 0015 assumiu que dados básicos são nome, situação, turno e datas (P8).

Os filtros misturam dados de dois módulos: turno e situação são da inserção (Atendidos); tipo de
deficiência, data de nascimento e nome são da pessoa (Pessoas). Alternativas consideradas:

- Pedir ao módulo Pessoas os ids que passam no filtro e cruzar com as inserções em memória: respeita a
  fronteira, mas a ordem por nome e a paginação ficam no Java, lendo tudo a cada página.
- Copiar nome, tipo e nascimento para a tabela `insercao`: duplica dado pessoal e precisa de
  sincronização a cada edição do cadastro (#40).
- Uma consulta SQL só de leitura, própria do módulo Atendidos, que junta `insercao` e `pessoa`, como o
  planejamento já prevê para os relatórios (seção 3).

## Decisão

- `GET /api/atendidos` (exige `ATENDIDOS_LER_BASICO`): página em ordem de nome, com o id de desempate.
  Filtros opcionais: `nome` (parte do nome, sem diferenciar maiúsculas, `%` e `_` procurados
  literalmente), `turno`, `situacao` (sem ele vêm ativos e desligados; a tela começa em ativos),
  `tipoDeficiencia` e `faixaEtaria` (`JOVEM` de 18 a 29, `ADULTO` de 30 a 59, calculada da data de
  nascimento no dia de Brasília).
- A consulta fica em `atendidos.interno.ConsultaAtendidos`: um SQL só de leitura sobre `insercao` e
  `pessoa`. É a única leitura direta de tabela de outro módulo; toda gravação continua pelas APIs
  Java (ADR 0081).
- `GET /api/atendidos/{id}`: a ficha completa traz a inserção (turno, data, situação), a pessoa (tipo
  de deficiência, nascimento, idade, faixa etária, telefone), o nome do cuidador e a inscrição de
  origem (data de cadastro e encaminhamento), lidos pelas APIs `Pessoas` e `ListaEspera`.
- Visão por permissão, num lugar só (`atendidos.interno.VisoesAtendido`):
  - com `ATENDIDOS_LER` (Coordenador e Equipe Técnica), a visão completa;
  - só com `ATENDIDOS_LER_BASICO` (Administrativo e Consulta), um tipo à parte com id, nome, situação,
    turno e data de inserção. Os filtros por tipo de deficiência e faixa etária respondem 403, porque
    o resultado revelaria esses dados. O campo `completo` diz ao frontend qual visão chegou.
- A leitura da listagem e da ficha não é auditada: não há dado sensível nelas no sentido do ADR 0023
  (escuta, PIA, violação, laudo).
- Frontend: `/atendidos` com os filtros (os de dado completo só aparecem para quem pode usá-los) e
  `/atendidos/{id}` com a ficha. Depois da inserção (#39), a tela abre a ficha nova.

## Consequências

- Gênero e bairro ainda não existem no cadastro da pessoa: entram como filtros quando o cadastro
  completo (#40, pergunta P6) criar esses campos, na mesma consulta.
- Visitas, escutas e PIA entram na ficha quando o módulo de atendimento (M2) existir. A escuta e o PIA
  são dados sensíveis: a leitura deles na ficha terá de ser auditada.
- Se a resposta da P8 mudar o que são dados básicos, muda só o `VisoesAtendido.Basico` e o
  `FiltroAtendidos.usaDadoCompleto`, marcados com `PENDENTE-CESJO(P8)`.
- Se a consulta SQL ficar lenta com muitos registros, os índices de `pessoa` e `insercao` resolvem;
  hoje são poucas centenas de pessoas.
