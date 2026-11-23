# ADR 0220 — Importação dos usuários atendidos de planilha: modelo provisório e inserção ativa

**Status:** Provisório (PENDENTE-CESJO P13) (2026-11-23) · Issue #62

## Contexto

A seção 5.2 pede importar também o cadastro de usuários que já frequentam o Centro-Dia. A pergunta P13
não tem resposta, então a planilha real não é conhecida. No sistema, o usuário atendido é a pessoa com
uma inserção ativa, e toda inserção sai de uma inscrição da lista de espera (ADR 0080: `inscricao_id` é
obrigatório), com um cuidador familiar que é um responsável da pessoa.

Alternativas consideradas:

- **Inserção sem inscrição**: exigiria tornar `inscricao_id` opcional e tratar a falta em todo lugar
  que lê a origem da inserção (relatórios, histórico da fila).
- **Inscrição e inserção na mesma linha, pelas APIs que já existem**: cada linha cadastra a pessoa na
  lista de espera e a insere no serviço, com as mesmas regras da tela. A fila e os relatórios continuam
  coerentes: a pessoa aparece como "inserida", com o histórico da inscrição.
- **Cuidador em coluna própria, além do responsável**: a P7 ainda não diz se o cuidador é sempre o
  responsável do RF001; uma coluna só basta para o MVP.

## Decisão

- Mesmo fluxo e infraestrutura da lista de espera (ADR 0215, ADR 0217 e ADR 0218): `colunas`, `modelo`,
  `previa` e confirmação em `/api/importacao/atendidos`, sem estado no servidor, só as linhas válidas
  gravadas, numa transação. A conversão dos campos do cadastro fica em `CamposCandidato`, usada pelos
  dois modelos.
- O modelo fica em `importacao.interno.ModeloAtendidos`, com o marcador `PENDENTE-CESJO(P13)`. Colunas:
  as da lista de espera, com "Nome do cuidador responsável" no lugar de "Nome do responsável" e
  "Data de cadastro na lista de espera" opcional, mais:

| Coluna | Obrigatória | Formato |
|---|---|---|
| Data de cadastro na lista de espera | não | dd/mm/aaaa; vazia, vale a data de inserção |
| Data de inserção | sim | dd/mm/aaaa, não futura e não anterior ao cadastro na lista |
| Turno | sim | Matutino, Vespertino, Integral |

- Cada linha válida chama `ListaEspera.inscrever` (pessoa, responsável e inscrição, com a data de
  cadastro) e a API nova `UsuariosAtendidos.inserirComOResponsavel(inscricaoId, dataInsercao, turno)`.
  Ela usa o responsável do cadastro como cuidador e passa pelas mesmas regras de `ServicoInsercao`. A
  inscrição sai da fila como "inserida" na data de inserção.
- Recusa a linha cujo nome já é de um usuário ativo, já está na lista de espera (essa pessoa é inserida
  pela fila, para não duplicar a inscrição) ou repete uma linha anterior da planilha.
- Permissão `PLANILHAS_IMPORTAR` (ADR 0216), só do Coordenador, que também é o único com
  `ATENDIDOS_INSERIR`. Auditoria: um `IMPORTAR` com a entidade `planilha_atendidos`, além dos `CRIAR`
  de pessoa, responsável, inscrição, histórico e inserção e do `ALTERAR` da inscrição.
- A importação não confere a capacidade, como a inserção pela tela: as vagas disponíveis caem com as
  inserções ativas importadas (ADR 0081).

## Consequências

- Quando a planilha real chegar, muda só `ModeloAtendidos`. Se ela tiver o cuidador separado do
  responsável (P7), entra uma coluna e o cuidador é cadastrado com `Pessoas.cadastrarResponsavel`.
- Usuários já desligados não entram pela importação; só os que frequentam hoje.
- Os dados do cadastro completo (RF007, ADR 0140), como CPF e endereço, continuam sendo preenchidos na
  ficha do usuário.
