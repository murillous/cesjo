# ADR 0030 — Notificação por perfil ou por conta, com leitura registrada por conta

**Status:** Aceito (2026-11-20)

## Contexto

O RF003 pede notificações internas, e o planejamento (seção 5) esboça a tabela `notificacao` com
`perfil_destino` ou `conta_destino` e uma coluna `lida_em`. O primeiro uso é avisar Coordenador e
Administrativo de vaga aberta na lista de espera (#31): uma notificação para um perfil inteiro.

Com uma única coluna `lida_em`, a primeira pessoa do perfil que lesse marcaria a notificação como lida
para todas as outras, e as demais nunca saberiam da vaga. Alternativas consideradas:

- `lida_em` na própria notificação: simples, mas só serve para notificação de conta.
- Criar uma linha por conta do perfil no momento do envio (fan-out): cada conta tem o seu `lida_em`,
  mas o módulo de notificações teria de listar as contas do perfil, o que cria dependência da tabela
  de contas, e uma conta criada ou movida de perfil depois do envio não receberia nada.
- Notificação imutável mais uma tabela de leitura `(notificacao_id, conta_id, lida_em)`: uma linha por
  conta que leu, igual para notificação de perfil e de conta.

## Decisão

- `notificacao` guarda o destino (exatamente um entre `perfil_destino` e `conta_destino`, garantido
  por `CHECK`), `tipo`, `mensagem`, `referencia` e as colunas padrão de criação e atualização. Não tem
  `lida_em` e não muda depois de criada.
- `notificacao_leitura` tem chave `(notificacao_id, conta_id)` e `lida_em`. Uma conta ler não afeta as
  outras. Por ser tabela de ligação, não tem `id` próprio nem as colunas de criação e atualização: a
  linha inteira é o registro de quem leu e quando.
- Uma conta vê as notificações dirigidas a ela e as dirigidas ao seu perfil atual. Quem entra no perfil
  depois do envio também vê; quem sai deixa de ver.
- `conta_destino` e `conta_id` não têm chave estrangeira para `conta_acesso`: a tabela de contas é do
  módulo acesso, e as migrações de cada módulo ficam independentes.
- `tipo` é um código em maiúsculas com sublinhado (por exemplo `VAGA_ABERTA`), definido como constante
  pelo módulo que cria a notificação. `referencia` é um caminho interno do frontend começando por `/`,
  ou nulo.
- Não há expiração automática. Como a mensagem não leva dado pessoal (ADR 0031), guardar o histórico
  não cria risco de LGPD; uma limpeza periódica pode entrar depois se o volume pedir.

## Consequências

- A contagem de não lidas é uma consulta só (`NOT EXISTS` na tabela de leitura), com índices por
  destino e data.
- O módulo não precisa saber quais contas existem num perfil.
- Marcar como lida é um `INSERT ... ON CONFLICT DO NOTHING`, idempotente mesmo com cliques repetidos.
- Uma conta removida deixa linhas órfãs em `notificacao_leitura`; isso é inofensivo, porque elas só são
  lidas junto com a própria conta.
