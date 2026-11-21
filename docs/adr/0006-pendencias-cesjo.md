# ADR 0006 — Decisões provisórias marcadas como `PENDENTE-CESJO`

**Status:** Aceito (2026-11-20)

## Contexto

Várias regras dependem das 16 perguntas da seção 9 do [planejamento](../planejamento.md) que a
equipe CESJO ainda não respondeu. Esperar todas as respostas trava a entrega; decidir sem deixar
rastro faz a escolha provisória virar definitiva por esquecimento e espalha a regra pelo código.

Alternativas consideradas: bloquear as issues `aguardando-cesjo`; manter uma lista única de
pendências num arquivo (vira ponto de conflito entre frentes paralelas e desatualiza).

## Decisão

Quando uma decisão depende de resposta da equipe CESJO:

1. A frente de trabalho escolhe o comportamento mais conservador (menos acesso, menos dado
   guardado, regra mais simples) e segue.
2. A escolha vira um ADR com Status `Provisório (PENDENTE-CESJO Pn)`, em que `Pn` é o número da
   pergunta na seção 9 do planejamento. Se a pergunta ainda não existe, ela é acrescentada lá.
3. A regra fica num lugar só, fácil de trocar: um enum, uma constante, uma propriedade `sgi.*` no
   `application.properties` ou um único método.
4. Esse lugar leva o comentário `PENDENTE-CESJO(Pn): <o que foi assumido> (ADR NNNN)`.

Para achar todas as pendências: `git grep -n "PENDENTE-CESJO"`.

## Consequências

- O trabalho segue sem esperar as respostas, e cada suposição tem dono e motivo escritos.
- Quando a resposta chegar, muda um ponto do código e entra um ADR que substitui o provisório.
- Suposição sem marcador é erro de revisão.
