# ADR 0044 — Serviço e botão de exportação no frontend

**Status:** Aceito (2026-11-21) · Issue #55

## Contexto

As telas de lista de espera e de relatórios vão ter um botão para exportar o que está na tela. O
download vem por `HttpClient` (o cookie de sessão e o token CSRF já passam por ele), e o navegador
precisa salvar o arquivo com o nome que o backend sugere. Com `responseType: 'blob'`, o
`ProblemDetail` de erro também chega como Blob, e `mensagemDeErro` não o lê.

## Decisão

- Em `frontend/src/app/compartilhado/exportacao/`:
  - `ExportacaoApi.baixar(url, formato, parametros)` faz o `GET` com `formato` e os filtros
    preenchidos (vazio, `null` e `undefined` não vão), lê o nome do `Content-Disposition` com
    `nomeDoContentDisposition` (o mesmo do módulo Arquivos) e converte o erro em Blob de volta para
    objeto; `baixarESalvar` também entrega o arquivo ao navegador.
  - `BotaoExportar` (`app-botao-exportar`), com as entradas `url`, `parametros` e `rotulo` e a saída
    `exportado`: um botão "Exportar" abre a escolha entre "PDF, para imprimir" e "Planilha Excel".
    Enquanto gera, o botão fica desabilitado com um indicador e o andamento é anunciado por
    `aria-live`; o erro aparece com `role="alert"`, com mensagem própria para 403.
- Mobile first: no celular o botão ocupa a largura toda; a partir do tablet volta à largura do texto,
  com o andamento ao lado.
- O componente não checa permissão: quem usa decide se ele aparece, com `Sessao.temPermissao`.

## Consequências

- A #32 e os relatórios só põem `<app-botao-exportar url="..." [parametros]="filtros()" />` na tela.
- `salvarNoNavegador` passou a ser exportado por `arquivos-api.ts` para as duas peças usarem o mesmo
  código.
