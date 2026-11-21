# ADR 0028 — API HTTP e API Java do módulo Arquivos

**Status:** Aceito (2026-11-20) · Issue #18

## Contexto

A lista de espera (#27) e os módulos seguintes anexam laudos e comprovantes. Eles precisam guardar e
ler arquivos sem passar pelo HTTP, e as regras de validação, cifra e auditoria não podem ser repetidas
em cada módulo. A tela de envio é genérica e também precisa de uma API HTTP.

Alternativas consideradas: só API HTTP, com cada módulo guardando o id devolvido pelo navegador (o
módulo não controla o que anexa); só API Java, sem endpoint próprio (o componente de envio do frontend
não teria para onde enviar); as duas, com a auditoria num lugar só.

## Decisão

- API Java no pacote raiz `arquivos`: interface `Arquivos` (`guardar`, `ler`, `metadados`), os records
  `NovoArquivo`, `MetadadosArquivo` e `ConteudoArquivo`, o enum `TipoArquivo` e as exceções
  `ArquivoRejeitadoException` (422) e `ArquivoNaoEncontradoException` (404). Os outros módulos guardam
  só o `id` do arquivo.
- A auditoria fica na implementação de `Arquivos`, então vale para os dois caminhos: `guardar`
  registra `EventoAuditoria.criacao("arquivo", id)` e `ler` registra
  `EventoAuditoria.leituraSensivel("arquivo", id)`. `metadados` não é auditado, porque não expõe o
  documento.
- A permissão é de quem chama. A API Java não checa perfil, porque cada módulo decide quem anexa e lê
  os seus documentos. A API HTTP checa:
  - `POST /api/arquivos` (multipart, parte `arquivo`) exige `DOCUMENTOS_ENVIAR` e responde 201 com
    `Location` e os metadados (`id`, `nome`, `tipoMime`, `tamanho`, `sha256`, `enviadoEm`);
  - `GET /api/arquivos/{id}` exige `DOCUMENTOS_LER` e devolve o conteúdo como anexo.
- `enviado_por` vem de `compartilhado.seguranca.IdentidadeAtual` (ADR 0009), sem depender do módulo
  `acesso`; fica nulo quando não há requisição autenticada.

## Consequências

- Um módulo novo anexa arquivo com uma chamada a `Arquivos.guardar` e guarda o id na própria tabela.
- Hoje qualquer conta com `DOCUMENTOS_LER` baixa qualquer arquivo pelo id. Quando os módulos ligarem
  arquivos a registros com regra de acesso própria (por exemplo laudos de uma inscrição), o download
  pode passar a ser feito pelo endpoint do módulo dono, e este endpoint genérico pode ficar restrito.
- Leitura pela API Java também aparece no log de auditoria, mesmo quando o módulo chamador já
  registrou a leitura do seu registro.
