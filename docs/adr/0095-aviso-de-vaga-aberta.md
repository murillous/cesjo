# ADR 0095 — Aviso de vaga aberta

**Status:** Provisório (PENDENTE-CESJO P3) (2026-11-21) · Issue #31

## Contexto

O RF003 pede avisar quando abrir vaga. A issue #31 define o gatilho: as vagas disponíveis passam de
zero para mais de zero, e Coordenador e Administrativo recebem notificação com o próximo candidato
da fila. Os gatilhos são o aumento de capacidade (agora) e o desligamento de um usuário atendido
(quando o módulo de atendidos entrar). A pergunta 3 também pergunta quem deve ser avisado e por qual
canal. A API `Notificacoes` (ADR 0031) proíbe dado pessoal na mensagem.

Alternativas consideradas:

- **Nome do candidato na mensagem**: a notificação não é cifrada e é vista por todas as contas do
  perfil (ADR 0031).
- **Avisar a cada aumento de vaga**: repetiria o aviso enquanto já há vaga aberta, e o aviso perderia
  valor.
- **A lista de espera escutar o evento `UsuarioDesligado` do módulo de atendidos**: ela teria de
  importar a classe do evento, e atendidos já depende da lista de espera. Seria ciclo (ADR 0081).
- **Tarefa periódica que compara as vagas**: atrasa o aviso e precisa guardar o último valor.

## Decisão

- `AvisoDeVagaAberta` (módulo `listaespera`) notifica só na passagem de zero para mais de zero. A
  notificação tem o tipo `VAGA_ABERTA` e vai para os perfis `COORDENADOR` e `ADMINISTRATIVO`, com o
  marcador `PENDENTE-CESJO(P3)` no destino. O canal é só o sistema.
- Mensagem sem nome: "Uma vaga foi aberta no serviço (N vagas disponíveis). Veja o próximo candidato
  da fila." A referência é `/lista-espera/{id}` do primeiro candidato ativo na ordem de
  `OrdemDaFila`, e a ficha checa a permissão de quem abre. Com a fila vazia, a mensagem diz isso e
  a referência é `/lista-espera`.
- **Gatilho por capacidade**: `ServicoVagas.definirCapacidade` compara as disponíveis antes e depois,
  na mesma transação. Reduzir a capacidade nunca avisa.
- **Gatilho por desligamento**: a interface pública `listaespera.AberturaDeVaga`
  (`vagasLiberadas(quantidade)`) é o ponto de escuta preparado. O ouvinte do evento `UsuarioDesligado`
  fica no módulo de atendidos (#42). Ele chama `vagasLiberadas(1)` na mesma transação, depois de o
  desligamento estar aplicado. A lista de espera calcula as disponíveis de antes como as de agora
  menos as liberadas.
- A criação da notificação é auditada pelo módulo Notificações (ADR 0033), e a mudança de capacidade
  pelo ADR 0092.

## Consequências

- Se a equipe pedir outro destino ou canal (e-mail, WhatsApp), muda `AvisoDeVagaAberta.DESTINO` ou
  entra um canal novo atrás da mesma regra.
- Hoje um desligamento não libera vaga na contagem (ADR 0093), então o gatilho por desligamento só
  passa a valer quando atendidos implementar `OcupacaoDeVagas` e chamar `AberturaDeVaga`.
- O aviso entra na transação de quem muda as vagas: se a mudança for desfeita, a notificação também é.
