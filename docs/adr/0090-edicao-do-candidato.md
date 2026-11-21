# ADR 0090 — Edição dos dados do candidato

**Status:** Aceito (2026-11-21) · Issue #29

## Contexto

O RF002 pede editar o candidato. A matriz da seção 6 dá à Equipe Técnica "ler e editar" e ao
Coordenador e ao Administrativo "total" na lista de espera. O ADR 0039 deixou a edição para esta
issue, com permissão própria. Os dados ficam em dois módulos: pessoa e responsável em `pessoas`, e
origem e data de cadastro em `listaespera` (ADR 0037).

Alternativas consideradas:

- **Reaproveitar `LISTA_ESPERA_CADASTRAR`**: a Equipe Técnica não cadastra (ADR 0039), mas edita.
- **Edição campo a campo (PATCH)**: menos dados por pedido, mas o formulário já tem todos os campos e
  a validação é a mesma do cadastro.
- **Bloquear a troca da data de cadastro**: protege a ordem da fila, mas impede corrigir um erro de
  digitação, que hoje só se resolveria no banco.

## Decisão

- Permissão nova `LISTA_ESPERA_EDITAR`, que vale para editar os dados e mudar a situação (ADR 0091):

| Permissão | Coordenador | Equipe Técnica | Administrativo | Consulta |
|---|---|---|---|---|
| `LISTA_ESPERA_EDITAR` | sim | sim | sim | não |

- `PUT /api/lista-espera/inscricoes/{id}` recebe os mesmos campos e regras do cadastro (ADR 0038),
  sem os anexos, que têm endpoint próprio (ADR 0056). Devolve a ficha atualizada.
- A API `Pessoas` ganha `atualizar(id, NovaPessoa)`, que muda a pessoa e o nome do primeiro
  responsável. Cada entidade registra `ALTERAR` na auditoria só com os nomes das colunas que mudaram.
  Sem mudança, não há registro.
- A data de cadastro pode ser corrigida. A troca muda a posição na fila e fica no log como
  alteração de `data_cadastro`. A tela de edição avisa disso.
- Frontend: o formulário de cadastro também serve para a edição, em `/lista-espera/:id/editar`, que
  exige `LISTA_ESPERA_EDITAR`.

## Consequências

- Sem controle de versão no pedido: duas pessoas editando ao mesmo tempo, vale a última. Para o
  número de contas do serviço isso é aceitável; a coluna `versao` já existe se for preciso.
- Quando o RF007 trouxer o cadastro completo da pessoa, a edição dos campos novos segue o mesmo
  caminho pela API `Pessoas`.
