# ADR 0020 — Tabela e conteúdo do registro de auditoria

**Status:** Aceito (2026-11-20)

## Contexto

O RNF 3.3 pede o registro de criação, alteração, exclusão, leitura de dado sensível, exportação,
login e falha de login, sem guardar valores sensíveis. O esboço da seção 5 do planejamento lista as
colunas. Faltava decidir como a consulta mostra quem fez a operação sem que o módulo `auditoria`
leia as tabelas de `acesso` (o `acesso` depende da auditoria, ADR 0009), de onde vem o IP e como
impedir que um valor entre no log por engano, já que `EventoAuditoria` aceita qualquer texto.

Alternativas consideradas para a conta: guardar só `conta_id` e buscar o login em `conta_acesso` na
consulta (cria dependência de `acesso` e perde o nome se a conta mudar de login); guardar o nome
completo (dado pessoal a mais). Para os nomes de campo: confiar só na revisão de código; validar
contra uma lista de campos por entidade (cada módulo teria de registrar a sua).

## Decisão

- Tabela `auditoria` (migração `V1__cria_auditoria.sql`): `id`, `ocorrido_em` (`timestamptz`),
  `conta_id`, `conta_login`, `acao`, `entidade`, `entidade_id`, `campos_alterados` (`text[]`) e `ip`.
  `acao` é texto com `CHECK` nos valores de `AcaoAuditoria`. Não há `criado_por` nem `atualizado_em`:
  a própria linha já é o registro de quem e quando, e ela nunca é alterada (ADR 0022).
- `conta_login` é um retrato do login no momento da operação. Conta e login vêm do evento quando
  ele traz algum dos dois (login, falha de login, logout); senão, de `IdentidadeAtual`. Fora de uma
  requisição autenticada, ficam vazios.
- `ip` é o `getRemoteAddr()` da requisição atual, quando houver; vazio em tarefa agendada.
- Nomes de entidade e de campo precisam ser identificadores (`[A-Za-z][A-Za-z0-9_.]*`, até 100
  caracteres). Texto com espaço, `=` ou outro sinal é recusado com `IllegalArgumentException`, e a
  operação falha. Os campos são gravados em ordem alfabética.
- Login com mais de 100 caracteres é cortado, para que uma falha de login com texto longo não
  derrube o registro. O IP é cortado em 45 caracteres. Identificador de entidade com mais de 100
  caracteres é recusado.
- Índices em `ocorrido_em`, `lower(conta_login)` e `entidade`, que são os filtros da consulta.

## Consequências

- A consulta mostra o login sem ler `acesso`. Se a conta trocar de login, os registros antigos ficam
  com o login antigo, que é o que aconteceu na época.
- A falha de login guarda o texto digitado no campo de login, até 100 caracteres. Se alguém digitar
  a senha no campo de login, ela fica no log; é o mesmo risco de qualquer sistema que registra
  tentativas por login.
- Atrás do Caddy, `getRemoteAddr()` devolve o IP do proxy. A implantação precisa ligar o
  tratamento dos cabeçalhos de proxy (`server.forward-headers-strategy`) para o IP do cliente
  aparecer; isso fica com a issue de implantação.
- A validação dos nomes pega o erro mais comum (passar o valor no lugar do nome), mas não impede um
  identificador que seja ele mesmo um dado. A revisão de código continua valendo.
