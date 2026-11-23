# ADR 0218 — Limites da importação e tratamento de planilhas malformadas

**Status:** Aceito (2026-11-23) · Issue #33

## Contexto

A importação lê a planilha inteira na memória com o Apache POI. Um arquivo grande, um `.xlsx`
corrompido ou um arquivo compactado feito para explodir ao descompactar (zip bomb) podem derrubar ou
travar o servidor. Quem importa precisa de uma mensagem clara do que fazer quando o arquivo não serve.

Alternativas consideradas:

- **Leitura em streaming (SAX)**: aguenta arquivos grandes, mas complica a leitura por título de
  coluna e de datas, para um volume que o Centro-Dia não tem.
- **Limite de tamanho e de linhas com leitura simples**: a fila e o cadastro de um Centro-Dia cabem
  com folga em poucas centenas de linhas.

## Decisão

- Tamanho máximo de 2 MB (`LeitorPlanilha.TAMANHO_MAXIMO`), conferido antes de ler os bytes do envio, e
  até 1000 linhas de dados (`LINHAS_MAXIMAS`). O limite geral do multipart (10 MB, ADR 0027) continua
  valendo antes disso.
- Só `.xlsx`: o tipo é conferido pela assinatura do arquivo (`FileMagic`), não pelo nome. Arquivo do
  Excel antigo (`.xls`) ou protegido por senha, vazio, corrompido, sem cabeçalho, sem linhas de dados,
  sem coluna obrigatória ou acima dos limites responde 422 com a mensagem do que fazer, e nada é lido
  além disso. As proteções do POI contra zip bomb (razão mínima de compressão) ficam ligadas, e a falha
  vira a mesma mensagem de arquivo ilegível.
- Fórmulas valem pelo resultado já calculado e salvo no arquivo; célula com erro do Excel vira o texto
  `#ERRO`, que aparece na pré-visualização e falha nas colunas de data, telefone e opções.
- A tela confere a extensão e o tamanho antes de enviar, com as mesmas mensagens.

## Consequências

- Uma lista maior que 1000 linhas é importada em mais de um arquivo.
- Se o volume crescer muito, a leitura pode passar para streaming sem mudar os modelos.
