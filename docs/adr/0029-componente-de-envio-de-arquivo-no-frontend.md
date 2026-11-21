# ADR 0029 — Componente de envio de arquivo no frontend

**Status:** Aceito (2026-11-20) · Issue #18

## Contexto

A issue #18 não tem tela própria, mas a lista de espera (#27) e os módulos seguintes vão anexar laudos
e comprovantes em formulários. Sem uma peça comum, cada formulário reimplementaria a escolha do
arquivo, a validação, o progresso e as mensagens de erro.

Alternativas consideradas:

- **Quando enviar**: guardar o `File` e enviar junto com o formulário (cada módulo precisaria de um
  endpoint multipart próprio e repetiria a validação); enviar na hora da escolha para
  `POST /api/arquivos` e entregar ao formulário só o `id`.
- **Como o formulário recebe o valor**: só por eventos de saída; como campo de formulário
  (`ControlValueAccessor`), que funciona com `formControlName` e validação de obrigatório.
- **Validação no cliente**: só extensão e tamanho; as mesmas regras do backend, incluindo a assinatura
  dos bytes.

## Decisão

- Em `frontend/src/app/compartilhado/arquivos/`:
  - `regras-arquivo.ts`: limite de 10 MB (`TAMANHO_MAXIMO_BYTES`, espelho de
    `sgi.arquivos.tamanho-maximo`), assinaturas de PDF, JPG e PNG iguais às do enum `TipoArquivo` do
    backend, validação e formatação de tamanho em pt-BR;
  - `ArquivosApi`: `enviar` (multipart na parte `arquivo`, com progresso), `baixar` (lê o nome do
    `Content-Disposition`) e `baixarESalvar`;
  - `EnvioArquivo` (`app-envio-arquivo`): campo de formulário cujo valor é o `id` do arquivo guardado
    ou `null`, com as saídas `enviado` e `removido`.
- O arquivo é conferido no cliente e enviado assim que escolhido. O componente mostra nome, tamanho,
  progresso e o resultado; erros do backend aparecem com o `detail` do `ProblemDetail`, e 403 e 413
  têm mensagem própria.
- Acessibilidade: o `input type="file"` fica oculto e fora da ordem de tabulação; quem abre o seletor é
  um botão comum, alcançável por teclado, com nome acessível que inclui o rótulo do campo e a dica de
  tipos e tamanho por `aria-describedby`. O andamento é anunciado por `aria-live` e o erro por
  `role="alert"`. Arrastar e soltar é um atalho a mais, nunca o único caminho.
- Responsivo, mobile first: no celular tudo fica empilhado e o botão ocupa a largura toda; a partir do
  tablet o botão volta à largura do texto, com a dica ao lado.
- O componente não checa permissão: quem o usa decide se ele aparece, com `Sessao.temPermissao`, e o
  backend continua sendo a autoridade.

## Consequências

- Um formulário anexa documento com `<app-envio-arquivo rotulo="Laudo médico" formControlName="laudoId" />`
  e guarda só o id na sua própria API.
- Um arquivo escolhido e depois abandonado (o formulário não é salvo) fica guardado sem dono. A
  limpeza desses arquivos entra com a regra de retenção (ADR 0026).
- Mudar o limite exige mudar a propriedade do backend e a constante do cliente.
- A aplicação precisa de `provideHttpClient` na configuração; ele entra com a primeira tela que usa a
  API.
