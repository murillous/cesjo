# ADR 0164 — Ofícios, declarações e encaminhamentos

**Status:** Aceito (2026-11-22) · Issue #49 · Provisório no ponto P31

## Contexto

O RF011 também pede a expedição e o recebimento de ofícios e declarações e os encaminhamentos
enviados e recebidos por instituição. A issue #49 pede anexo opcional e listagem por instituição e
período. Os contatos da issue #48 já estão na tabela `comunicacao` (ADR 0160), e o planejamento
(seção 5) põe nela `direcao` e `arquivo_id`.

Alternativas consideradas para o modelo:

- **Tabela própria para documentos**: separa os registros com e sem anexo, mas duplica data,
  contraparte, assunto, listagem e contagem, e o relatório mensal teria duas fontes.
- **Mesma tabela, com direção e anexo opcionais**: uma listagem, uma contagem. A direção só vale nos
  tipos que a têm, e uma restrição `CHECK` garante isso no banco.

Alternativas consideradas para o anexo:

- **Enviar por `POST /api/arquivos` e ligar pelo id**, como a lista de espera: essa rota exige
  `DOCUMENTOS_ENVIAR`, que a Equipe Técnica não tem, e a matriz dá a ela acesso total às comunicações.
- **Rota própria de anexo na comunicação**, com as permissões da comunicação, que chama a API Java
  `arquivos.Arquivos`. Valida tipo e tamanho, cifra e audita como os outros anexos (ADRs 0026 a 0029).

## Decisão

- Migração V51: tipos novos `OFICIO`, `DECLARACAO` e `ENCAMINHAMENTO` em `atividades.TipoComunicacao`,
  que sabe quais têm direção (`temDirecao()`). Coluna `direcao` (`ENVIADA` ou `RECEBIDA`, enum público
  `atividades.DirecaoComunicacao`), obrigatória nesses três tipos e vazia nos outros, conferida na
  validação (`direcaoDoTipo`) e na restrição `ck_comunicacao_direcao`. Na tela, a direção aparece como
  "expedido" ou "recebido" no ofício e na declaração e como "enviado" ou "recebido" no encaminhamento.
- Coluna `arquivo_id`, opcional, com chave estrangeira para `arquivo`. **Provisório (P31)**: um anexo só
  por registro, em qualquer tipo, e sempre opcional.
  - `POST /api/comunicacoes/{id}/anexo` (multipart, campo `arquivo`) guarda o arquivo pelo módulo
    Arquivos e o liga ao registro, no lugar do anterior. Exige `COMUNICACOES_REGISTRAR`.
  - `DELETE /api/comunicacoes/{id}/anexo` desliga o anexo. O arquivo continua guardado, sem ligação,
    porque o módulo Arquivos não apaga.
  - `GET /api/comunicacoes/{id}/anexo` baixa o anexo. Exige `COMUNICACOES_LER`, e o módulo Arquivos
    registra a leitura sensível.
  - Ligar, trocar ou desligar o anexo registra uma alteração da comunicação, com o campo `arquivo_id`.
- A listagem ganha os filtros `direcao` e `contraparte`, um trecho do nome da pessoa ou instituição,
  sem diferença de maiúsculas e com os curingas do `like` tratados como texto. A resposta diz a direção
  e se há anexo (`temAnexo`). A ficha traz o nome, o tipo e o tamanho do anexo.
- `atividades.Comunicacoes.contarPorDirecao(tipo, inicio, fim)` conta os enviados e os recebidos de um
  tipo com direção, para o relatório mensal (issue #58).

## Consequências

- A busca por instituição é por trecho de texto, sem índice próprio. Com o volume esperado (centenas
  de registros por ano) o índice por data basta. Um cadastro de instituições (parceiros, RF013) pode
  substituir o texto depois.
- Se a equipe pedir vários anexos por registro, `arquivo_id` vira uma tabela de ligação.
- Se a equipe pedir número ou protocolo do ofício, entra uma coluna própria. Hoje o número vai no
  assunto.
