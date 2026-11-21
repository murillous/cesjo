# ADR 0075 — Ações de realização, cancelamento e reagendamento na ficha da visita

**Status:** Aceito (2026-11-21) · Issue #35

## Contexto

A equipe registra o resultado da visita no celular, logo depois de sair da casa da família (ADR
0035). Telas separadas para cada ação aumentariam a navegação, e um diálogo sobre a ficha esconde
os dados da visita no celular.

## Decisão

- As ações ficam na própria ficha (`/visitas/:id`), numa seção "O que aconteceu com a visita?",
  só enquanto a visita está agendada. Uma ação abre por vez, no lugar dos botões, com "Voltar" para
  desistir. No celular os botões ocupam a largura toda.
- "Registrar realização" (com `VISITAS_REGISTRAR`) fica desabilitado antes do dia da visita, com a
  explicação. "Reagendar" e "Cancelar visita" aparecem com `VISITAS_AGENDAR`.
- O reagendamento abre com a hora e os profissionais da visita atual preenchidos; ao concluir, a
  tela vai para a visita nova. As duas visitas têm links uma para a outra.
- As observações de uma visita realizada não carregam com a ficha: o botão "Ler observações" avisa
  que a leitura fica no log de auditoria e só então busca o texto.

## Consequências

- A agenda mostra a visita cancelada riscada e a reagendada no novo dia, sem tela extra.
