# ADR 0165 — Telas de ofícios, declarações e encaminhamentos

**Status:** Aceito (2026-11-22) · Issue #49

## Contexto

Os ofícios, declarações e encaminhamentos entram nas telas de contatos e comunicações da issue #48
(ADR 0163). A tela precisa pedir a direção só nos tipos que a têm, aceitar um anexo opcional e listar
por instituição e período. O campo de envio que já existe (`app-envio-arquivo`) manda para
`POST /api/arquivos`, que pede `DOCUMENTOS_ENVIAR` (ver ADR 0164).

## Decisão

- Formulário: os seis tipos aparecem nos mesmos cartões com ícone, em duas linhas de três no tablet.
  Ao escolher ofício, declaração ou encaminhamento, aparece a pergunta "Foi enviado ou recebido?", com
  dois rádios que usam o gênero do tipo ("Expedido pelo Centro-Dia" ou "Recebido de fora"). Ao trocar
  para um tipo sem direção, a resposta é apagada.
- Anexo no registro: o botão "Escolher arquivo" abre o seletor do navegador, e o arquivo é conferido no
  cliente com as regras de `regras-arquivo.ts`. Ele é enviado logo depois de salvar o registro. Se o
  envio falhar, o registro continua salvo e o aviso pede para anexar de novo na ficha.
- Anexo na ficha: a seção "Anexo" mostra o nome e o tamanho, com os botões "Baixar anexo", "Trocar
  anexo" (ou "Anexar arquivo") e "Remover anexo".
- Lista: filtro "Pessoa ou instituição" por trecho do nome e filtro "Enviados ou recebidos", ao lado
  dos filtros de tipo e período. Cada linha diz o tipo com a direção ("Ofício expedido") e mostra um
  clipe quando há anexo, com o texto "com anexo" para o leitor de tela.

## Consequências

- O seletor de arquivo da comunicação é próprio, mais simples que `app-envio-arquivo` (sem barra de
  progresso). Se os anexos ficarem grandes, vale generalizar o campo compartilhado para aceitar outra
  URL de envio.
