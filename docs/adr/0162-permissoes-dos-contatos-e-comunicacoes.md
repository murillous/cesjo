# ADR 0162 — Permissões dos contatos e comunicações

**Status:** Aceito (2026-11-22) · Issue #48

## Contexto

Na matriz da seção 6 do planejamento, "Contatos e comunicações (RF011)" é "total" para Coordenador,
Equipe Técnica e Administrativo e "não" para o Consulta. Não há ponto com (?).

Alternativas consideradas: uma permissão só (simples, mas a leitura e o registro não se separam se a
equipe pedir um perfil só de leitura); permissões por tipo (sem pedido da equipe, só aumenta a matriz).

## Decisão

Duas permissões novas em `acesso.Perfil`, como nos outros módulos:

| Permissão | Coordenador | Equipe Técnica | Administrativo | Consulta |
|---|---|---|---|---|
| `COMUNICACOES_LER` (listar e abrir a ficha com a descrição) | sim | sim | sim | não |
| `COMUNICACOES_REGISTRAR` (registrar e corrigir) | sim | sim | sim | não |

O frontend mostra o item "Contatos e comunicações" no menu para quem tem `COMUNICACOES_LER` e os
botões de registro e correção para quem tem `COMUNICACOES_REGISTRAR`.

## Consequências

- Os ofícios, declarações e encaminhamentos (issue #49) usam as mesmas permissões, inclusive para
  anexar e baixar o arquivo.
