# ADR 0215 — Importação da lista de espera de planilha: modelo provisório e fluxo em duas etapas

**Status:** Provisório (PENDENTE-CESJO P13) (2026-11-23) · Issue #33

## Contexto

A seção 5.2 do documento de requisitos pede importar a lista de espera que a equipe mantém hoje em
Excel. A pergunta P13 (se as planilhas existem e um exemplo com dados fictícios) não tem resposta, então
não se sabe quais colunas a planilha real tem. A importação precisa preservar a data de cadastro
original, porque ela define a ordem da fila (ADR 0057), mostrar os erros por linha antes de gravar e
gravar só as linhas válidas depois da confirmação.

Alternativas consideradas para o modelo:

- **Esperar a planilha real**: trava a issue e o M1, e a equipe ainda teria de digitar a fila na mão.
- **Mapeamento de colunas configurável na tela**: flexível, mas é muita tela para um MVP e um passo a
  mais para quem importa uma vez.
- **Modelo fixo, num lugar só do código, com download na tela**: a equipe copia os dados para o modelo;
  quando a planilha real chegar, troca-se uma classe.

Alternativas consideradas para o fluxo:

- **Guardar o arquivo no servidor entre a pré-visualização e a confirmação**: exige tabela ou
  armazenamento temporário, expiração e limpeza.
- **Enviar o arquivo de novo na confirmação**: sem estado no servidor; a confirmação refaz a mesma
  análise e grava o que estiver válido naquele momento.

## Decisão

- Módulo `importacao` (já previsto no planejamento) com a infraestrutura comum: `LeitorPlanilha` (lê a
  primeira aba de um `.xlsx` com o Apache POI), `Coluna`, `ModeloPlanilha`, `LinhaPlanilha` (lê texto,
  data e opção e acumula os erros), `Analise`, `GeradorModelo` (modelo para download) e `Previa`.
- O modelo da lista de espera fica em `importacao.interno.ModeloListaEspera`, com o marcador
  `PENDENTE-CESJO(P13)`. Colunas, com os títulos exatos do cabeçalho:

| Coluna | Obrigatória | Formato |
|---|---|---|
| Nome completo | sim | até 150 caracteres |
| Data de nascimento | não | dd/mm/aaaa, anterior a hoje |
| Tipo de deficiência | sim | Física, Sensorial, Intelectual, Múltipla, TEA, Síndrome de Down (T21) |
| Telefone | sim | DDD e número, 10 ou 11 dígitos, com ou sem máscara |
| Nome do responsável | sim | até 150 caracteres |
| Origem do encaminhamento | sim | CRAS, CREAS, Judiciário, Saúde, Educação, Demanda espontânea |
| Data de cadastro | sim | dd/mm/aaaa, não futura e não anterior ao nascimento |

- O cabeçalho é a primeira linha com valor. As colunas são achadas pelo título, em qualquer ordem, sem
  diferenciar maiúsculas, acentos, espaços extras nem o ` *` que o modelo põe nas obrigatórias. Coluna
  desconhecida é ignorada; coluna obrigatória ausente recusa o arquivo inteiro.
- Datas valem como célula de data do Excel ou como texto `dd/mm/aaaa` (aceita sem zero à esquerda).
  Opções valem pelo rótulo das telas ou pelo código do enum, sem diferenciar maiúsculas nem acentos.
- O modelo para download (`GET /api/importacao/lista-espera/modelo`) tem a aba "Dados", só com o
  cabeçalho e colunas em formato texto, e a aba "Instruções", com formato, valores aceitos e exemplo
  fictício de cada coluna. O exemplo fica fora da aba "Dados" para não ser importado por engano.
  `GET .../colunas` devolve a mesma descrição para a tela.
- Fluxo sem estado no servidor: `POST .../previa` lê e devolve todas as linhas com os valores e os
  erros, sem gravar; `POST .../lista-espera` recebe o mesmo arquivo, refaz a análise e grava só as
  linhas válidas, numa transação, na ordem da planilha. Sem nenhuma linha válida, responde 422.
- Cada linha válida vira inscrição aguardando pela API nova `ListaEspera.inscrever(NovoCandidato)`,
  que usa o mesmo caminho do cadastro pela tela, com a data de cadastro da planilha. A ordem da fila
  não muda (`OrdemDaFila`): linhas com a mesma data ficam na ordem da planilha, pelo `criado_em`.
- Para enviar a mesma planilha duas vezes não duplicar a fila, a linha cujo nome já está na fila
  (aguardando ou em avaliação) ou aparece numa linha anterior da planilha vira erro. O nome é comparado
  sem maiúsculas nem acentos.

## Consequências

- Quando a planilha real chegar, muda só `ModeloListaEspera`: títulos, colunas e conversão. A tela lê
  as colunas do backend e não precisa mudar.
- Duas pessoas homônimas não entram pela importação; a segunda é cadastrada pela tela.
- A confirmação lê o arquivo duas vezes no total. Com o limite de 2 MB e 1000 linhas (ADR 0218) isso é
  barato, e evita guardar dados pessoais em arquivo temporário.
- Importar 1000 linhas faz algumas consultas por linha (cadastro, histórico, auditoria). É uma operação
  rara, feita uma vez na implantação.
