# ADR 0013 — Gestão de contas de acesso pelo Coordenador

**Status:** Aceito (2026-11-20) · Issue #15

## Contexto

A seção 4.1 do documento de requisitos dá ao Coordenador a gestão das contas. A issue #15 pede criar,
editar perfil, desativar, redefinir senha, troca de senha obrigatória no primeiro acesso e proteção da
última conta ativa de Coordenador. Faltava decidir o formato da API, o formato do login e o que fazer
quando o Coordenador mexe na própria conta.

Alternativas consideradas: excluir contas em vez de desativar (perde o vínculo com o log de
auditoria); permitir mudar o login (quebra a leitura do log, que guarda o login informado); permitir
que o Coordenador desative a própria conta quando há outro Coordenador (encerraria a sessão no meio da
operação, por engano fácil de cometer).

## Decisão

- API em `/api/contas`, toda com `CONTAS_GERENCIAR`: listar paginado (padrão por nome), obter, criar
  (`{login, nome, perfil}`, responde 201 com a conta e a senha temporária), editar nome e perfil
  (`PUT`), `POST .../desativar`, `POST .../reativar` e `POST .../redefinir-senha`.
- Contas não são excluídas, só desativadas. Reativar zera o bloqueio.
- Login de 3 a 60 caracteres entre letras sem acento, números, ponto, hífen e sublinhado, guardado em
  minúsculas e único. O login não muda depois de criado.
- A última conta ativa de Coordenador não pode ser desativada nem trocar de perfil. As contas ativas
  de Coordenador ficam travadas (`select ... for update`) durante a checagem, para duas operações
  simultâneas não deixarem o sistema sem Coordenador.
- O Coordenador não desativa nem muda o perfil da própria conta; pode mudar o próprio nome.
- A auditoria registra criação e cada alteração com os nomes dos campos (`nome`, `perfil`, `ativa`,
  `senha_hash`, `troca_senha_obrigatoria`), nunca os valores.
- A resposta nunca leva hash nem senha, exceto a senha temporária recém-gerada.

## Consequências

- O histórico de quem fez o quê continua legível, porque nenhuma conta some.
- Sempre sobra pelo menos um Coordenador ativo para administrar o sistema.
- Para corrigir um login digitado errado, o Coordenador desativa a conta e cria outra.
