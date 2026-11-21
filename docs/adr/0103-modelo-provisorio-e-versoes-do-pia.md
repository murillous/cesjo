# ADR 0103 — Modelo provisório do PIA, conteúdo em JSON cifrado e versões

**Status:** Provisório (PENDENTE-CESJO P5) (2026-11-21) · Issue #37

## Contexto

O RF006 pede criar, editar e controlar o status do PIA (Plano de Atendimento Individual e Familiar),
mas a estrutura do plano não foi definida: a pergunta 5 (objetivos, metas, ações, prazos,
responsáveis? existe modelo em papel? há revisão periódica?) não foi respondida. O conteúdo fala da
pessoa e da família, então é sensível (seção 7). As versões anteriores precisam ser preservadas.

Alternativas consideradas: só seções de texto livre (simples, mas não deixa acompanhar objetivo por
objetivo); tabelas próprias para objetivos e metas (cada mudança do modelo pela equipe CESJO viraria
migração, e cada coluna precisaria de cifra própria); guardar o histórico de cada edição (o
critério fala em versões do plano, não em rascunhos).

## Decisão

- O módulo `atendimento` é dono da tabela `pia` (migração `V31`). Cada linha é uma versão do PIA de
  uma pessoa: `numero_versao` (1, 2, 3...), `status`, `conteudo` e `versao_anterior_id`.
- `conteudo` é o JSON de `ConteudoPia`, cifrado inteiro pelo `TextoCifradoConverter`. A estrutura
  provisória (pergunta 5):
  - `diagnostico`: texto livre;
  - `objetivos`: até 30, na ordem de prioridade, cada um com `descricao` (obrigatória), `metas`,
    `acoes`, `prazo` (data) e `responsavel` (profissional, família ou outro serviço, em texto);
  - `observacoes`: texto livre.
  Trocar a estrutura muda só `ConteudoPia` e a tela, sem migração. O JSON é gravado por um
  conversor próprio, para que o formato guardado não mude com a configuração da API HTTP.
- `POST /api/pias` elabora a primeira versão da pessoa (`ELABORADO`); se a pessoa já tem PIA,
  responde 422. `PUT /api/pias/{id}` edita só a versão mais nova e só enquanto está `ELABORADO`.
  Versões anteriores não mudam. A nova versão (a partir de um PIA apresentado ou aprovado) entra com
  a issue #38.
- `GET /api/pias?pessoaId=` lista as versões sem conteúdo e sem registro no log. O conteúdo sai só em
  `GET /api/pias/{id}`, registrado como `LER_SENSIVEL`.
- Auditoria: criação e alteração do campo `conteudo` (edição sem mudança não registra).

## Consequências

- Relatórios não conseguem contar objetivos em SQL, porque o conteúdo é cifrado. Se a equipe
  precisar disso, os campos agregáveis saem do JSON para colunas próprias.
- Quando o modelo oficial chegar, os PIAs já gravados continuam legíveis se os campos novos forem
  opcionais; uma mudança incompatível precisa de conversão dos JSONs existentes.
