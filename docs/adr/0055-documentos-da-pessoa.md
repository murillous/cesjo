# ADR 0055 — Documentos da pessoa no módulo Pessoas

**Status:** Provisório (PENDENTE-CESJO P14) (2026-11-21) · Issue #27

## Contexto

O RF001 pede o laudo médico e o comprovante de residência de cada candidato. Os mesmos documentos
servem depois para o usuário atendido (RF007), então não pertencem à inscrição na lista de espera. O
planejamento (seção 5) prevê `documento_pessoa` (pessoa_id, tipo, arquivo_id), e o módulo Arquivos
(ADR 0026 e ADR 0028) já guarda o conteúdo cifrado e devolve só o `id`.

Alternativas consideradas:

- **Dono da tabela**: a lista de espera (o atendido precisaria copiar ou ler tabela de outro módulo);
  o módulo Pessoas, dono da pessoa.
- **Quantos por tipo**: histórico de todos os envios (a tela teria de escolher qual vale); um por
  tipo, com o envio novo substituindo o anterior.
- **Arquivo substituído**: apagar o arquivo antigo (perde a prova do que foi apresentado, e a regra
  de retenção da pergunta 14 não está decidida); manter o arquivo no módulo Arquivos, sem ligação.

## Decisão

- O módulo `pessoas` é dono de `documento_pessoa` (migração `V6`): `pessoa_id`, `tipo`,
  `arquivo_id` (chave estrangeira para `arquivo`) e as colunas `criado_*`, `atualizado_*` e `versao`.
- Tipos no enum público `TipoDocumento`: `LAUDO_MEDICO` e `COMPROVANTE_RESIDENCIA`. O tipo "outro"
  do planejamento entra quando algum módulo precisar.
- Um documento por pessoa e tipo (`uk_documento_pessoa_tipo`). Anexar de novo troca o `arquivo_id`
  da mesma linha e é auditado como alteração do campo `arquivo_id`; o primeiro anexo é auditado como
  criação. O arquivo antigo continua guardado no módulo Arquivos.
- Um arquivo liga a um documento só (`uk_documento_pessoa_arquivo`): reaproveitar o id de um arquivo
  já anexado, inclusive de outra pessoa, é recusado com 422.
- A API `Pessoas` ganha `anexarDocumento(pessoaId, tipo, arquivoId)`, `documentos(pessoaId)` (só
  metadados, sem auditoria) e `lerDocumento(pessoaId, tipo)`, que registra
  `leituraSensivel("documento_pessoa", id)`. O módulo Arquivos registra também a leitura do arquivo
  (ADR 0028), então um download gera dois registros.
- Como no resto da API `Pessoas`, a permissão é de quem chama.

## Consequências

- O cadastro do usuário atendido (#39) já encontra os documentos da pessoa, sem cópia.
- Arquivos substituídos ficam guardados sem ligação até existir regra de retenção (pergunta 14).
- Quem quiser o histórico de envios consulta o log de auditoria, que guarda as trocas.
