# ADR 0040 — Geração de PDF e Excel no módulo Relatórios

**Status:** Aceito (2026-11-21) · Issue #55

## Contexto

A seção 7 do planejamento pede exportação em PDF gerado de HTML com templates Thymeleaf e
openhtmltopdf, com logo e cores institucionais, e em Excel com Apache POI, toda exportação auditada. A
lista de espera (#32) e os relatórios (#56 a #61) vão exportar. Faltava decidir onde a peça comum
mora, quais versões usar e como o PDF carrega logo, cores e fonte.

Alternativas consideradas para o lugar:

- Pacote em `compartilhado`: é um módulo aberto, mas não tem regra de negócio e não pode depender de
  `auditoria`, porque `auditoria` já depende de `compartilhado.seguranca` (seria um ciclo). A
  exportação precisa auditar.
- Módulo `relatorios`: o planejamento já dá a ele "exportação PDF e Excel" (seção 3). Os módulos de
  negócio passam a depender de `relatorios`, e `relatorios` lê os outros módulos só por SQL próprio,
  sem dependência Java, então não há ciclo.

Alternativas consideradas para o PDF: biblioteca de desenho de página (PDFBox puro, iText), com
layout em código e licença AGPL no caso do iText; HTML com Thymeleaf e openhtmltopdf, como pede o
planejamento, com o layout num modelo que qualquer pessoa da equipe edita.

## Decisão

- A API pública fica no pacote raiz de `relatorios` (`Exportacao`, `TabelaExportacao`,
  `FiltroAplicado`, `FormatoExportacao`, `ArquivoExportado`, ADR 0041 e ADR 0042). A implementação
  fica em `relatorios.interno`.
- Dependências: `org.thymeleaf:thymeleaf` (versão do Spring Boot, 3.1.7), `io.github.openhtmltopdf:openhtmltopdf-pdfbox`
  1.1.108 (o fork mantido do openhtmltopdf, com PDFBox 3) e `org.apache.poi:poi-ooxml` 5.5.1. A POI
  6.0.0 saiu há um mês e muda de versão maior; fica a 5.5.1, estável.
- O motor do Thymeleaf é criado pelo próprio `GeradorPdf`, em modo XML, lendo
  `src/main/resources/relatorios/modelos/`. Não entra o starter web do Thymeleaf: o backend é só API
  REST e não renderiza páginas.
- O modelo `tabela.html` é XHTML bem formado. Todo dado entra por `th:text` (escapado). O logo vem de
  `docs/frontend/assets`, reduzido para 800 px de largura (`relatorios/logo-cesjo.png`, 52 KB), e é
  embutido como data URI. O renderizador só aceita recursos `data:`: nenhum dado exportado faz o
  servidor buscar URL de fora.
- Cores, fuso (`America/Sao_Paulo`) e idioma ficam num lugar só, `relatorios.interno.IdentidadeVisual`,
  com os mesmos valores de `frontend/src/styles/_tokens.scss`: filete do cabeçalho em `primary`,
  cabeçalho da tabela em `primary-dark` com texto branco, linhas alternadas em `background-secondary`.
- O PDF usa a fonte sem serifa padrão do PDF (Helvetica). A Atkinson Hyperlegible Next do frontend
  (ADR 0036) só está no projeto como WOFF2, que o openhtmltopdf não lê.
- A largura das colunas da planilha é estimada pelo número de caracteres (de 8 a 60), sem
  `autoSizeColumn`, que depende das fontes do sistema e pode falhar numa imagem Docker enxuta.

## Consequências

- Um módulo novo exporta montando uma tabela e chamando `Exportacao`; não precisa conhecer Thymeleaf,
  openhtmltopdf nem POI.
- Os módulos que exportam passam a depender de `relatorios`. Se um dia `relatorios` precisar chamar a
  API Java de um desses módulos, a exportação terá de sair para um módulo próprio, para não criar
  ciclo.
- Trocar o visual do PDF é editar `tabela.html` e `IdentidadeVisual`. Usar a fonte institucional no
  PDF exige guardar um TTF no backend e registrá-lo no `PdfRendererBuilder`; fica para quando a
  pergunta P16 for respondida.
- O backend cresce uns 24 MB de dependências (PDFBox, POI com as classes OOXML, Thymeleaf e
  bibliotecas do Apache Commons).
