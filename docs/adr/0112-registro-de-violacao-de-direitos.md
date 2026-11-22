# ADR 0112 — Registro de violação de direitos

**Status:** Aceito (2026-11-21) · Issue #43

## Contexto

O RF009 pede categorizar e registrar as violações de direitos identificadas, em 12 categorias. Duas
delas se dividem em intra e extrafamiliar: abuso sexual e violência física ou psicológica. O item 34
do backlog acrescenta data de identificação e descrição cifrada, com acesso só para Coordenador e
Equipe Técnica. A matriz da seção 6 diz "Violações de direitos (RF009)": Coordenador "total", Equipe
Técnica "registrar e ler", Administrativo e Consulta "não". Não há ponto com (?) nessa linha.

## Decisão

- Tabela `violacao_direito` (migração V36) no módulo Atendidos, ligada à pessoa (`pessoa_id`), como
  no esboço da seção 5: `tipo`, `ambito`, `data_identificacao`, `descricao` cifrada com
  `TextoCifradoConverter` e as colunas de autoria (`criado_por` é quem registrou).
- Categorias no enum público `atendidos.TipoViolacao`, na ordem do RF009, com `exigeAmbito()` em
  `ABUSO_SEXUAL` e `VIOLENCIA_FISICA_PSICOLOGICA`. O âmbito (`INTRAFAMILIAR`, `EXTRAFAMILIAR`) é
  obrigatório nessas duas e proibido nas outras, no backend (`NovaViolacao`) e no banco (restrição
  `ck_violacao_ambito`). "Atitudes discriminatórias no seio familiar" já é intrafamiliar pela própria
  categoria e não pede âmbito.
- Data de identificação não futura; descrição obrigatória, até 4000 caracteres.
- API: `POST /api/atendidos/{id}/violacoes`, para o usuário atendido ativo ou desligado. A resposta
  não devolve a descrição: ler a descrição tem rota própria e auditada (ADR 0145).
- Permissões novas: `VIOLACOES_REGISTRAR` e `VIOLACOES_LER`, para Coordenador e Equipe Técnica.
  Editar e excluir ("total" do Coordenador) ficam para quando a equipe pedir.
- Auditoria: criação de `violacao_direito`.
- Frontend: botão "Registrar violação de direitos" na ficha e tela `/atendidos/:id/violacoes/nova`,
  com as 12 categorias como opções grandes e o âmbito aparecendo só quando a categoria pede.

## Consequências

- A categoria não é cifrada: a listagem filtra por ela (ADR 0145). Só a descrição é cifrada.
- Ligar a violação à pessoa, e não à inserção, mantém o histórico se a pessoa voltar ao serviço por
  outra inserção.
