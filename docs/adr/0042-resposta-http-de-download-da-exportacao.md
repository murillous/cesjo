# ADR 0042 — Resposta HTTP de download e parâmetro de formato da exportação

**Status:** Aceito (2026-11-21) · Issue #55

## Contexto

Cada módulo vai ter o próprio endpoint de exportação, com a própria permissão e os próprios filtros.
Sem um padrão, cada um escolheria como receber o formato, que cabeçalhos devolver e como nomear o
arquivo, e o frontend teria de tratar cada caso.

Alternativas consideradas: um endpoint genérico `/api/relatorios/exportar` que recebe a tabela
pronta (o navegador montaria o conteúdo, e o backend não saberia o que está exportando nem quem pode);
endpoints por módulo com um padrão comum de entrada e saída.

## Decisão

- Cada módulo expõe um `GET` próprio (por exemplo `GET /api/lista-espera/exportacao`), com a sua
  permissão em `@PreAuthorize` e os mesmos filtros da listagem, mais o parâmetro `formato`.
- `formato` aceita `pdf` ou `xlsx`, sem diferenciar maiúsculas. O conversor
  `ConversorFormatoExportacao` deixa o controlador receber `@RequestParam FormatoExportacao formato`;
  outro valor responde 400 antes de gerar ou auditar qualquer coisa.
- O controlador devolve `ArquivoExportado.comoDownload()`: status 200, `Content-Type` do formato
  (`application/pdf` ou `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`),
  `Content-Length`, `Content-Disposition: attachment` com `filename` e `filename*` em UTF-8 e
  `Cache-Control: no-store`, porque o conteúdo tem dado pessoal.
- A geração é síncrona, na própria requisição.

## Consequências

- O frontend baixa qualquer exportação do mesmo jeito (ADR 0044).
- A permissão de exportar fica junto do dado, no módulo dono, e é testada lá.
- Uma exportação que demore mais que a requisição suporta precisaria de geração em segundo plano; não
  é o caso para os volumes previstos.
