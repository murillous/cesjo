# ADR 0041 — Tabela simples como contrato da exportação

**Status:** Aceito (2026-11-21) · Issue #55

## Contexto

A lista de espera (#32) e os relatórios (#56 a #61) exportam listagens e quadros de números. Cada um
poderia ter o próprio modelo de PDF e o próprio código de planilha, ou todos poderiam entregar o mesmo
formato de dados para uma peça comum. Também faltava decidir como os valores chegam: só texto, ou
com tipo, para a planilha guardar número e data como valor.

## Decisão

- O contrato é `TabelaExportacao(titulo, colunas, linhas, filtros)`: título, rótulos das colunas,
  linhas com uma célula por coluna e a lista de `FiltroAplicado(nome, valor)`. O construtor recusa
  título em branco, tabela sem coluna e linha com número de células diferente das colunas, e copia
  as listas.
- Célula é `null` (vazia), `String`, `Number`, `Boolean`, `LocalDate`, `LocalDateTime` ou `Instant`.
  Enums e códigos chegam já traduzidos para o rótulo da tela. Outro tipo sai como `toString()`.
- No PDF, as células saem no padrão brasileiro: `05/01/2026`, `05/01/2026 09:07` (instantes no fuso
  de Brasília), `Sim`/`Não`, decimais com vírgula e até duas casas, sem separador de milhar (para ano
  e código não virarem `2.026`).
- Na planilha, número e data são gravados como valor, com formato `dd/mm/yyyy` ou `dd/mm/yyyy hh:mm`;
  texto é sempre texto, então um valor que começa com `=` não vira fórmula.
- Layout comum dos dois formatos: título, "Gerado em" com data e hora de Brasília, filtros aplicados
  (ou "Sem filtros: todos os registros.") e a tabela. O PDF repete o cabeçalho da tabela em cada
  página, numera "Página X de Y", mostra "Nenhum registro encontrado." quando não há linha e o total
  de registros no fim; com mais de 6 colunas a página vira paisagem. A planilha congela o cabeçalho e
  liga o filtro automático.
- O nome do arquivo é o título sem acento, em minúsculas e com hífens, até 80 caracteres, mais a data
  de geração: `lista-de-espera-2026-11-21.pdf`.
- Não há limite de linhas nesta base. Quem exporta manda as linhas que a tela consultaria sem
  paginação.

## Consequências

- Uma exportação nova é só montar a tabela; o visual fica igual em todo o sistema.
- Relatórios com gráfico ou com várias seções não cabem numa tabela simples. Quando aparecerem (#56 em
  diante, depois da pergunta P9), entram como um modelo novo ao lado de `tabela.html`, sem quebrar este
  contrato.
- Sem limite de linhas, uma exportação muito grande gera um PDF longo na memória. Para os volumes do
  Centro-Dia (centenas de registros) não pesa; se pesar, um limite entra por ADR novo.
