# ADR 0072 — Telas da agenda de visitas e agenda do dia na página inicial

**Status:** Aceito (2026-11-21) · Issue #34

## Contexto

A equipe agenda visitas e consulta a agenda principalmente pelo celular, em campo (ADR 0035). A
página inicial "Hoje" já previa mostrar a agenda do dia quando houvesse visitas. Ainda não existe a
fila da lista de espera no frontend (issue #28), então o agendamento não pode partir da ficha do
candidato.

## Decisão

- Módulo `modulos/visitas`, rota `/visitas`, item "Visitas domiciliares" (curto "Visitas") logo
  depois da lista de espera no menu, com `VISITAS_LER`.
- A agenda mostra uma semana, de segunda a domingo, escolhida por `?semana=AAAA-MM-DD` (qualquer dia
  dela), com botões de semana anterior, próxima e "Esta semana". Cada dia é um bloco com o título do
  dia; dias sem visita ficam discretos, para a semana caber numa rolagem curta no celular. Uma
  semana por vez fica bem abaixo do limite de 62 dias da API.
- Cada visita mostra a hora em destaque numa coluna fixa, como numa agenda de papel, com a pessoa e
  os profissionais ao lado. Visita cancelada aparece riscada. O componente `VisitasDoDia` é o mesmo
  na agenda e na página inicial.
- A hora é lida do texto `dataHora` (hora local do Centro-Dia, ADR 0070), sem passar por `Date`, para
  não aplicar fuso.
- Agendar (`/visitas/nova`, `VISITAS_AGENDAR`): candidato num campo de busca com autocompletar sobre
  os candidatos ativos (a busca ignora acentos e só aceita um candidato escolhido na lista), data a
  partir de hoje, hora e profissionais ativos (seleção múltipla). Depois de agendar, a agenda abre
  na semana da visita.
- A ficha da visita (`/visitas/:id`) mostra data, hora, pessoa, status e profissionais.
- Página inicial "Hoje": quem tem `VISITAS_LER` vê as visitas do dia abaixo da data, com o link
  "Ver agenda" e, sem visitas, um convite para agendar (com `VISITAS_AGENDAR`). Quem não vê visitas
  continua com a mensagem de boas-vindas.

## Consequências

- Quando a fila (#28) existir, a ficha do candidato pode ganhar um atalho para agendar com o
  candidato já escolhido.
- A lista de candidatos vem inteira para o autocompletar. Com centenas de candidatos, isso é leve; se
  passar de alguns milhares, a busca precisa ir para o backend.
