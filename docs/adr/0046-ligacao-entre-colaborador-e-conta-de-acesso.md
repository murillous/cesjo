# ADR 0046 — Ligação entre colaborador e conta de acesso guardada no colaborador

**Status:** Aceito (2026-11-21) · Issue #50

## Contexto

A issue #50 pede que o colaborador possa ser ligado a uma conta de acesso. O esboço do planejamento
(seção 5) põe a coluna em `conta_acesso.colaborador_id`. O módulo `acesso` é da Fundação e todos os
outros dependem dele; se ele guardasse e validasse o colaborador, passaria a depender de
`institucional`, e o Spring Modulith recusaria o ciclo (`institucional` já usa `acesso`).

Alternativas consideradas: deixar a ligação para depois (o critério de aceite pede agora, e a escuta
quer sugerir o profissional pela conta logada); `conta_acesso.colaborador_id` sem validação (permite
apontar para colaborador inexistente); evento entre módulos (complexo demais para um campo).

## Decisão

- A ligação entra agora, do lado do colaborador: `colaborador.conta_acesso_id`, opcional, com chave
  estrangeira para `conta_acesso` e `UNIQUE` (uma conta liga a no máximo um colaborador, e cada
  colaborador tem no máximo uma conta).
- O módulo `acesso` ganha a API Java pública de leitura `acesso.Contas` (`obter(id)`, devolve
  `ResumoConta` com id, login, nome e situação, sem senha nem perfil).
- Ao criar ou editar o colaborador, a conta precisa existir e não pode estar ligada a outro
  colaborador (422 com mensagem clara). Uma conta desativada pode continuar ligada.
- A ligação é feita no cadastro do colaborador, por quem tem `COLABORADORES_GERENCIAR`. A tela
  escolhe a conta na lista de contas, que exige `CONTAS_GERENCIAR`; hoje os dois são só do
  Coordenador.

## Consequências

- O planejamento fica desatualizado nesse ponto: `conta_acesso` não tem `colaborador_id`. A seção 5
  passa a citar este ADR.
- O módulo `acesso` não sabe nada de colaboradores; quem quer o colaborador da conta usa
  `Colaboradores.obterAtivoPorConta`.
- Se outro perfil ganhar `COLABORADORES_GERENCIAR` sem `CONTAS_GERENCIAR`, a tela precisa de outra
  forma de listar contas (a API HTTP do colaborador aceita o `contaId` de qualquer jeito).
