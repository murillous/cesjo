# ADR 0155 — Listagem de atividades por período e atividades na ficha do usuário

**Status:** Aceito (2026-11-22) · Issue #47

## Contexto

A issue #47 pede filtros por tipo, período, profissional e usuário na listagem de atividades, e que
as atividades apareçam na ficha do usuário atendido. A listagem do ADR 0150 já devolve a página sem a
descrição.

Alternativas consideradas:

- Um endpoint por filtro (por pessoa, por profissional). Os filtros se combinam, por exemplo as
  atividades de uma psicóloga com um usuário num mês.
- Consulta JPQL com todos os parâmetros opcionais (`:tipo is null or ...`). Fica longa e mais difícil
  de mudar do que uma `Specification` montada só com os filtros informados.
- Na ficha do usuário, chamar um endpoint do módulo Atendidos que juntasse as atividades. Isso criaria
  dependência de Atendidos para Atividades, que já depende de Atendidos (ciclo).

## Decisão

- `GET /api/atividades` aceita `tipo`, `de`, `ate` (datas `AAAA-MM-DD`, as duas inclusive),
  `profissionalId` e `pessoaId`, todos opcionais e somados. `FiltroAtividades` monta a
  `Specification`; profissional e usuário usam `member of` sobre as coleções, servidas pelos índices
  `ix_atividade_profissional_colaborador` e `ix_atividade_participante_pessoa` da `V45`. Período
  invertido responde 422. A ordem continua da mais recente para a mais antiga.
- `GET /api/atividades/filtros` devolve os profissionais e os usuários que aparecem em alguma
  atividade, ativos ou não, para os seletores dos filtros. Exige `ATIVIDADES_LER`.
- A listagem não é auditada: não traz a descrição. A leitura sensível continua só na ficha dos
  atendimentos e estudos de caso (ADR 0154).
- Frontend: a lista de atividades ganha o painel de filtros no padrão da lista de usuários atendidos
  (empilhado no celular, grade de duas colunas no tablet e de três no desktop). `?pessoaId=` na URL
  já abre a lista filtrada.
- A ficha completa do usuário atendido (`/atendidos/:id`) mostra, para quem tem `ATIVIDADES_LER`, as
  cinco atividades mais recentes da pessoa (componente `AtividadesDoUsuario` do módulo atividades) e,
  se houver mais, o link "Ver todas as N atividades" para a lista filtrada.

## Consequências

- Sem migração nova: os índices da `V45` cobrem os filtros.
- Os dashboards (issue #57) contam por `Atividades.contarPorTipo` (ADR 0151), não por esta
  listagem.
