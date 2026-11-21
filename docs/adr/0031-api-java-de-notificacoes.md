# ADR 0031 — API Java síncrona para outros módulos criarem notificações

**Status:** Aceito (2026-11-20)

## Contexto

Outros módulos precisam criar notificações: a lista de espera avisa vaga aberta (#31) para Coordenador
e Administrativo, e outros avisos virão. O planejamento (seção 3) prevê eventos do Spring Modulith para
ligar módulos sem dependência direta.

Alternativas consideradas:

- Evento de domínio (`NotificacaoSolicitada`) que o módulo de notificações escuta: desacopla, mas a
  notificação fica fora da transação de quem pede (ou exige o registro de publicações do Modulith, que
  ainda não está no projeto) e o erro de conteúdo inválido aparece longe de quem o causou.
- Interface pública no pacote raiz do módulo, chamada direto: dependência explícita e simples, dentro
  da mesma transação.

## Decisão

- `notificacoes.Notificacoes` é a API pública, com `notificarPerfil(Perfil, NovaNotificacao)`,
  `notificarPerfis(Set<Perfil>, NovaNotificacao)` (uma notificação por perfil) e
  `notificarConta(UUID, NovaNotificacao)`. Cada método devolve o identificador criado.
- `notificacoes.NovaNotificacao` é um record com `tipo`, `mensagem` e `referencia`, que valida o
  formato na construção e lança `IllegalArgumentException` quando o chamador erra: tipo fora de
  `MAIUSCULAS_COM_SUBLINHADO`, mensagem vazia ou acima de 500 caracteres, referência que não é caminho
  interno (`/...`, sem `//`) ou acima de 300 caracteres.
- As chamadas entram na transação de quem chama (`@Transactional` padrão): se a operação de negócio for
  desfeita, a notificação também é.
- A mensagem nunca leva dado pessoal (nem nome de pessoa, nem saúde, crença, violação ou observação). Ela
  é vista por todo o perfil, não é cifrada e não expira. O detalhe fica na tela da `referencia`, que
  checa a permissão de quem abre. A regra está no Javadoc de `NovaNotificacao`; revisão de código cobra.
- Um módulo que precise publicar evento em vez de chamar direto pode ter um ouvinte no próprio módulo
  que chama esta API.

## Consequências

- O módulo que notifica depende de `notificacoes`, e `notificacoes` só depende de `acesso` (para
  `Perfil` e `ContaAtual`) e de `auditoria`. O acesso não pode notificar sem criar ciclo; se precisar,
  muda para evento.
- Notificar é barato (um `INSERT` e um registro de auditoria por destino).
