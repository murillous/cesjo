# ADR 0212 — Tela do relatório de transparência

**Status:** Aceito (2026-11-23) · Issue #61

## Contexto

O relatório de transparência (ADR 0210) tem período livre, uma linha em reais e é feito para ser
publicado. A área de Relatórios já tem abas (ADR 0197) e o relatório mensal tem uma prévia em tabelas
por grupo com o botão de exportar (ADR 0201).

Alternativas consideradas para o período:

- **Só o ano** (`mat-select`): simples, mas não atende a prestação de contas pela vigência de um termo.
- **Intervalo de datas** (`mat-date-range-input`, como o dashboard de atendimento): cobre ano, semestre
  e vigência de termo com o mesmo controle.

## Decisão

- Aba nova "Relatório de transparência" (`/relatorios/transparencia`) na área de Relatórios, com a
  mesma permissão das outras abas.
- Período em `mat-date-range-input`, sem datas futuras. Abre no ano anterior inteiro, que já fechou.
- A prévia segue o relatório mensal: título, período, hora em que foi gerado, uma tabela por grupo e o
  botão "Baixar relatório" (`app-botao-exportar`), que exporta o mesmo período da prévia.
- Um aviso diz que o relatório só tem números somados, sem nome nem dado de pessoa, e pode ser
  publicado ou enviado. A linha em reais sai como moeda e com destaque de cor.
- Componente, API e estilos em arquivos próprios (`relatorio-transparencia.*`, `transparencia-api.ts`).
  Dos arquivos compartilhados, só `abas.ts` e `relatorios.routes.ts` ganham uma entrada.

## Consequências

- Mudar o conteúdo não mexe na tela: ela mostra as linhas na ordem em que vêm do servidor.
- O SCSS repete a base do relatório mensal. Se um terceiro relatório em tabela aparecer, a base vira um
  estilo compartilhado.
