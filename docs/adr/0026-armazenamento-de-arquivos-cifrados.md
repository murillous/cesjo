# ADR 0026 — Armazenamento dos arquivos cifrados e tabela `arquivo`

**Status:** Aceito (2026-11-20) · Issue #18

## Contexto

O planejamento (seções 5 e 7) pede arquivos guardados cifrados num volume próprio, nunca no banco nem
no repositório, com metadados e SHA-256 no banco. Faltava decidir como o arquivo fica no disco, o que
entra na tabela e como banco e disco ficam coerentes.

Alternativas consideradas:

- **Conteúdo**: no banco como `bytea` (o planejamento proíbe, e o backup do banco cresceria); no disco.
- **Nome no disco**: o nome original (vaza dado pessoal no sistema de arquivos e abre caminho para
  ataques de caminho); o UUID do registro.
- **Nome original no banco**: em claro (o nome de um laudo costuma ter o nome da pessoa); cifrado
  com o conversor do ADR 0025.
- **Coerência banco e disco**: gravar o banco primeiro (um arquivo pode faltar no disco com o registro
  já gravado); gravar o disco primeiro e apagar o arquivo se a transação for desfeita (sobra no máximo
  um arquivo órfão se o processo cair no meio, sem registro apontando para ele).

## Decisão

- O conteúdo é cifrado com o `Cifrador` (formato binário do ADR 0025) e gravado em
  `sgi.arquivos.diretorio/<dois primeiros caracteres do id>/<id>`, sem extensão. A gravação é atômica:
  escreve num temporário na mesma pasta e renomeia.
- O diretório vem de `sgi.arquivos.diretorio`. Em desenvolvimento o padrão é `dados/arquivos`, relativo
  ao diretório do backend e ignorado pelo git. No perfil `prod`, a propriedade é
  `${SGI_ARQUIVOS_DIRETORIO}`, sem padrão: sem a variável a aplicação não sobe. Na subida, o diretório é
  criado se faltar, e a aplicação falha se não puder escrever nele.
- Tabela `arquivo` (migração `V3`): `id`, `nome_original` (cifrado, `text`), `tipo_mime`, `tamanho`
  (bytes do conteúdo em claro), `sha256` (do conteúdo em claro, hexadecimal minúsculo),
  `caminho_armazenamento` (relativo ao diretório, único), `chave_id` (chave que cifrou o conteúdo),
  `enviado_por` e `enviado_em`. Restrições de banco conferem tipo aceito, tamanho positivo e formato
  do SHA-256.
- O registro é imutável. Por isso `enviado_por` e `enviado_em` substituem as colunas `criado_*` e não
  há colunas `atualizado_*` (seção 5 do planejamento).
- `enviado_por` não tem chave estrangeira para `conta_acesso`, porque a tabela vem de outra migração
  (`V2`, issues #13 a #15) feita em paralelo, e o envio pela API Java pode não ter conta (fica nulo).
  A chave estrangeira pode entrar numa migração posterior, depois da junção da Fundação.
- O disco é gravado antes do banco, e uma sincronização da transação apaga o arquivo se ela for
  desfeita.
- Na leitura, o SHA-256 do conteúdo decifrado é comparado com o do banco; se não conferir, a leitura
  falha. O GCM já detecta adulteração do arquivo; a comparação pega troca de arquivo entre registros.

## Consequências

- O backup precisa levar o banco e o diretório de arquivos juntos, e as chaves ficam fora dos dois.
- Um arquivo órfão no disco (queda do processo entre gravar e confirmar) não aparece para ninguém;
  uma rotina de limpeza pode ser feita com a operação (épico Operação).
- Exclusão de arquivo não faz parte desta issue; entra quando um módulo precisar, junto com a regra
  de retenção (pergunta 14 da seção 9 do planejamento).
