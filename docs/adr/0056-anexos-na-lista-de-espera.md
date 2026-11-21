# ADR 0056 — Anexos na lista de espera e cadastro incompleto

**Status:** Aceito (2026-11-21) · Issue #27

## Contexto

O RF001 exige laudo médico e comprovante de residência. Na prática o encaminhamento nem sempre chega
com os dois, e a issue #27 pede que o candidato sem eles apareça como cadastro incompleto, não que
seja recusado. O envio do arquivo já existe em `POST /api/arquivos` e no componente
`app-envio-arquivo` (ADR 0029), que devolve só o `id`.

Alternativas consideradas:

- **Exigir os dois no cadastro**: impede registrar quem já está esperando e mudaria a ordem da fila
  pela demora do documento.
- **Endpoint multipart próprio**: repetiria a validação do módulo Arquivos (ADR 0029 já decidiu que o
  formulário envia na hora e guarda o id).
- **Download pelo endpoint genérico `/api/arquivos/{id}`**: qualquer conta com `DOCUMENTOS_LER`
  baixaria pelo id, sem passar pela inscrição (ADR 0028 já previa o endpoint do módulo dono).

## Decisão

- `POST /api/lista-espera/inscricoes` aceita `laudoMedicoArquivoId` e
  `comprovanteResidenciaArquivoId`, opcionais. Com algum deles, a conta precisa também de
  `DOCUMENTOS_ENVIAR` (403 sem ela). Cadastro e anexos ficam na mesma transação: arquivo inexistente
  ou já anexado desfaz tudo com 422.
- `PUT /api/lista-espera/inscricoes/{id}/documentos/{tipo}` com `{ "arquivoId": ... }` anexa ou
  substitui depois do cadastro. Exige `LISTA_ESPERA_LER` e `DOCUMENTOS_ENVIAR`. É o caminho da
  edição até a issue #29 ter a ficha completa.
- `GET /api/lista-espera/inscricoes/{id}/documentos/{tipo}` baixa o documento como anexo, com
  `Cache-Control: no-store`. Exige `LISTA_ESPERA_LER` e `DOCUMENTOS_LER`, e a leitura é auditada como
  dado sensível (ADR 0055).
- A resposta da inscrição traz `documentos` (tipo, nome, tipo MIME, tamanho e data, sem conteúdo),
  `documentosPendentes` e `cadastroCompleto`. A regra fica em `DocumentosObrigatorios`, no módulo
  `listaespera`: completo é ter os dois tipos. O cadastro incompleto continua na fila, na mesma
  posição.
- Frontend: o formulário de cadastro mostra dois `app-envio-arquivo` para quem tem
  `DOCUMENTOS_ENVIAR`; a página `/lista-espera/:id/documentos` mostra a situação do cadastro, baixa e
  anexa ou substitui cada documento, conforme as permissões.

## Consequências

- A fila (#28) e a ficha (#29) mostram o aviso de cadastro incompleto a partir de `cadastroCompleto`.
- Se a equipe decidir que o cadastro incompleto não ocupa posição, a mudança fica na regra de
  ordenação da fila, sem mexer nos anexos.
- O endpoint genérico `/api/arquivos/{id}` continua aberto para `DOCUMENTOS_LER`; restringi-lo fica
  para quando todos os documentos tiverem endpoint do módulo dono.
