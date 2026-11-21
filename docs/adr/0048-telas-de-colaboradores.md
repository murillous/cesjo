# ADR 0048 — Telas de listagem e cadastro de colaboradores

**Status:** Aceito (2026-11-21) · Issue #50

## Contexto

O cadastro de colaboradores (ADR 0045) precisa de listagem e formulário no frontend, mobile first
(ADR 0035), com o item no menu. A equipe tem dezenas de pessoas e quem sai fica inativo, então a
lista cresce com o tempo e mistura quem está e quem não está na equipe.

## Decisão

- Módulo `modulos/colaboradores`, rota `/colaboradores`, item "Colaboradores" no menu (curto
  "Equipe" na barra inferior do celular), visível com `COLABORADORES_LER` (ADR 0047).
- A listagem abre filtrada em "Ativos", com as opções "Inativos" e "Todos" num grupo de botões. No
  celular cada colaborador é um bloco (nome e situação em cima, função e conta embaixo); a partir do
  tablet vira tabela de quatro colunas.
- Um formulário só para cadastro (`/colaboradores/novo`) e edição (`/colaboradores/:id`): nome,
  função, conta de acesso opcional e, na edição, o interruptor "Ativo na equipe". Não há botão de
  excluir. Sem `COLABORADORES_GERENCIAR` o formulário fica só para leitura e a rota `novo` não abre.
- A conta de acesso é escolhida numa lista com até 200 contas, lida de `GET /api/contas` só quando a
  conta logada tem `CONTAS_GERENCIAR` (ADR 0046). A conta já ligada aparece sempre, mesmo fora dessa
  lista.
- Rótulos das funções em `rotulos.ts`, neutros quanto a gênero ("Psicólogo(a)").

## Consequências

- Se o número de contas passar de 200, a escolha da conta precisa de busca.
- Se outro perfil ganhar `COLABORADORES_GERENCIAR` sem `CONTAS_GERENCIAR`, a lista de contas fica
  vazia e só dá para manter ou desfazer a ligação atual.
