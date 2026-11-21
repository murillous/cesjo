# ADR 0024 — Consulta do log de auditoria: filtros, período e tela

**Status:** Aceito (2026-11-20)

## Contexto

O Coordenador consulta o log por período, conta e entidade (RNF 3.3). O backend guarda instantes
em UTC, e o dia do usuário é o dia do navegador. O nome técnico da entidade (`conta_acesso`) não é
algo que o Coordenador saiba digitar. No celular, uma tabela de nove colunas não cabe.

Alternativas consideradas para o período: datas (`LocalDate`) convertidas no servidor com um fuso
fixo; instantes calculados pelo frontend. Para conta: busca parcial (`contém`) ou login exato. Para
entidade: texto livre ou lista. Para a tela no celular: tabela com rolagem horizontal ou cartões.

## Decisão

- `GET /api/auditoria`, protegido por `AUDITORIA_LER`, com os parâmetros opcionais `de` (instante
  ISO-8601, inclusivo), `ate` (instante, exclusivo), `login` (igual, sem diferenciar maiúsculas),
  `entidade` (igual) e `acao` (um valor de `AcaoAuditoria`). `de` igual ou depois de `ate` responde
  422. A paginação segue o padrão do projeto; a ordem é fixa, do mais recente para o mais antigo,
  com o id como desempate, e o parâmetro `sort` é ignorado.
- `GET /api/auditoria/entidades`, com a mesma permissão, devolve as entidades que já aparecem no
  log, para o filtro virar uma lista.
- O frontend escolhe datas e converte para instantes no fuso do navegador: `de` é o início do dia
  inicial e `ate` é o início do dia seguinte ao final.
- A tela fica no módulo `modulos/auditoria`, com a permissão `AUDITORIA_LER` no registro de módulos
  e item de menu. No celular cada registro é um cartão; a partir do breakpoint de tablet vira tabela,
  que rola dentro do próprio contêiner se não couber.

## Consequências

- O servidor não precisa saber o fuso do usuário, e a consulta por instantes usa o índice de
  `ocorrido_em` direto.
- Login exato evita que a busca vire uma varredura por pedaços de login, mas o Coordenador precisa
  saber o login certo; se isso atrapalhar, troca-se por busca parcial com índice próprio.
- A lista de entidades é um `select distinct` na tabela; com o volume esperado do CESJO é barato, e
  o índice de `entidade` ajuda.
