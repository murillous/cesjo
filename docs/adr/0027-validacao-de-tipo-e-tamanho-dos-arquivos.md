# ADR 0027 — Validação de tipo e tamanho dos arquivos enviados

**Status:** Aceito (2026-11-20) · Issue #18

## Contexto

O planejamento aceita só PDF, JPG e PNG, conferidos pelo conteúdo, com limite inicial de 10 MB. O
`Content-Type` e a extensão vêm do cliente e não provam nada. Faltava decidir como reconhecer o tipo,
o que fazer com o nome, como os limites se relacionam e que resposta a API dá.

Alternativas consideradas:

- **Reconhecimento**: biblioteca de detecção (Apache Tika, dependência grande para três tipos);
  assinatura dos primeiros bytes, conferida no próprio código.
- **Nome com extensão que não confere com o conteúdo** (por exemplo `programa.exe` com conteúdo PDF):
  recusar (atrapalha quem só errou o nome); aceitar como veio (o download sairia com extensão
  enganosa); acrescentar a extensão do tipo reconhecido.
- **Resposta ao limite**: 413 (só o multipart do servidor consegue responder antes de ler o corpo);
  422 de regra de negócio, igual para a API HTTP e para a API Java.

## Decisão

- O tipo é reconhecido pela assinatura: PDF começa com `%PDF-`, JPG com `FF D8 FF`, PNG com
  `89 50 4E 47 0D 0A 1A 0A`. O enum `arquivos.TipoArquivo` é o único lugar dessa regra. O tipo MIME
  gravado e devolvido no download é o do tipo reconhecido, nunca o informado.
- O nome guardado perde caminho e caracteres de controle, fica com no máximo 255 caracteres e sempre
  termina numa extensão do tipo reconhecido; se não termina, a extensão é acrescentada
  (`programa.exe` vira `programa.exe.pdf`). Nome vazio vira `arquivo.<extensão>`.
- O limite é `sgi.arquivos.tamanho-maximo`, padrão `10MB` (10 MiB, 10.485.760 bytes), conferido no
  serviço para qualquer chamador. O multipart do Spring usa o mesmo valor em `max-file-size`
  (`${sgi.arquivos.tamanho-maximo}`) e 11 MB em `max-request-size`, para caber o envelope multipart.
- Arquivo vazio, acima do limite ou de tipo não aceito gera `ArquivoRejeitadoException` (subclasse de
  `RegraNegocioException`): 422 em `ProblemDetail`, com mensagem em português. Quando o servidor barra
  o corpo antes do controlador, a resposta é o 413 "Arquivo muito grande" que o tratador global já
  traduz.
- O download sai com `Content-Disposition: attachment`, `Cache-Control: no-store` e
  `X-Content-Type-Options: nosniff`, para o navegador não abrir nem adivinhar o tipo.

## Consequências

- Aceitar outro tipo é acrescentar uma constante no enum e na restrição da tabela `arquivo`.
- A assinatura prova o formato declarado, não que o arquivo é inofensivo: um PDF com script continua
  sendo PDF. Por isso o download é sempre anexo, nunca exibido no próprio domínio.
- O cliente vê 422 ou 413 conforme onde o limite foi pego; o frontend valida os 10 MB antes de enviar
  (ADR 0029), então os dois casos são raros.
