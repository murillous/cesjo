# ADR 0152 — Permissões das atividades (provisório)

**Status:** Aceito (2026-11-22) · Issue #45 · Provisório no ponto P17

## Contexto

Na matriz da seção 6, "Atividades (RF010)" é "total" para o Coordenador e a Equipe Técnica, "ler (?)"
para o Administrativo e "não" para o Consulta. O ponto com (?) é a pergunta P17, ainda sem resposta;
a regra do ADR 0015 é deixar sem acesso o perfil em dúvida.

Alternativas consideradas: dar leitura ao Administrativo já (os atendimentos e estudos de caso da
issue #46 ficam na mesma área e são sensíveis); separar leitura dos tipos coletivos e dos individuais
em permissões diferentes (sem pedido da equipe, só aumenta a matriz).

## Decisão

Duas permissões novas em `acesso.Perfil`:

| Permissão | Coordenador | Equipe Técnica | Administrativo | Consulta |
|---|---|---|---|---|
| `ATIVIDADES_LER` (listar e abrir a ficha) | sim | sim | não | não |
| `ATIVIDADES_REGISTRAR` (registrar, corrigir e ver as opções) | sim | sim | não | não |

- O Administrativo fica sem atividades enquanto a P17 não tiver resposta. **Provisório (P17).**

## Consequências

- Se a equipe responder que o Administrativo lê atividades, basta acrescentar `ATIVIDADES_LER` na
  linha do `ADMINISTRATIVO`; os atendimentos e estudos de caso pedem então uma permissão própria para a
  leitura do texto.
