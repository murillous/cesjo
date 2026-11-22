# ADR 0140 — Campos do cadastro completo do usuário atendido

**Status:** Provisório (PENDENTE-CESJO P6) (2026-11-22) · Issue #40

## Contexto

O RF007 pede "dados pessoais completos" do usuário atendido, além de gênero, faixa etária, tipo de
deficiência, crença religiosa e bairro. A equipe CESJO ainda não disse quais campos entram (pergunta
P6). O cadastro da lista de espera (RF001, ADR 0038) já tem nome, data de nascimento, tipo de
deficiência, telefone e responsável.

Alternativas consideradas:

- **Colunas novas na tabela `pessoa`.** Mistura dados que só existem para quem foi inserido no
  serviço com os do candidato da fila, e cada campo novo mexe numa tabela muito lida.
- **Um JSON cifrado com tudo.** Simples de mudar, mas impede filtros e contagens por gênero e bairro,
  que o dashboard (#56) precisa.
- **Uma tabela 1 para 1 com a pessoa, uma coluna por campo.** Mantém os campos consultáveis em SQL e
  separa o cadastro complementar do cadastro da fila.

## Decisão

- Tabela `cadastro_complementar` (migração V40), do módulo Pessoas, com `pessoa_id` como chave. A
  linha nasce na primeira gravação; sem ela, a API devolve o cadastro vazio.
- Campos provisórios, todos opcionais: gênero, CPF, NIS, RG, escolaridade, CEP, logradouro, número,
  complemento, bairro, cidade, renda familiar mensal, recebe BPC, inscrito no CadÚnico, recebe Bolsa
  Família; e, à parte, crença religiosa, medicamentos de uso contínuo e outras informações de saúde.
- A lista de campos fica num lugar só por camada: o record público `pessoas.CadastroComplementar`
  (e `pessoas.DadosSensiveis` para crença e saúde) no backend, a interface `CadastroComplementar` em
  `atendidos-api.ts` no frontend. Um campo novo entra no record, na entidade
  `CadastroComplementarPessoa`, numa migração e no formulário. A auditoria compara os records campo a
  campo (`CamposAlterados`) e não precisa mudar.
- Regras: CPF com dígitos verificadores (`pessoas.Cpf`), NIS com 11 dígitos, CEP com 8 dígitos; CPF,
  NIS e CEP guardados só com os dígitos; renda não negativa, com duas casas. A data de nascimento
  continua opcional e sem bloqueio fora de 18 a 59 anos (P2, ADR 0038).
- A edição também corrige os dados da pessoa vindos da fila (nome, nascimento, tipo de deficiência,
  telefone), pela API `Pessoas.atualizar`, mantendo o nome do responsável.
- API: `PUT /api/atendidos/{id}/cadastro` (204), numa transação só. A ficha completa
  (`GET /api/atendidos/{id}`) ganha `cadastro` e `dadosSensiveis`; a listagem completa ganha
  `genero` e `bairro`, e os filtros `genero` e `bairro` (trecho do nome), só para a visão completa.
- Auditoria: criação e alteração de `cadastro_complementar`, com os nomes das colunas que mudaram.

## Consequências

- Quando a equipe responder a P6, mudar os campos é localizado: record, entidade, migração e
  formulário, nos pontos com `PENDENTE-CESJO(P6)`.
- O CPF é cifrado com IV aleatório, então não há restrição de CPF único nem busca por CPF. Se for
  preciso, entra um hash determinístico em coluna à parte, por ADR novo.
- O cuidador familiar (P7) não muda: continua escolhido na inserção (ADR 0080).
