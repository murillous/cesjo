# ADR 0067 — Monitor externo de disponibilidade no GitHub Actions

**Status:** Provisório (PENDENTE-CESJO P15) (2026-11-21)

## Contexto

O RNF 3.4 pede 99% de disponibilidade, e a issue #23 pede um monitor externo do endpoint de saúde,
com alerta. Ainda não se sabe quem administra a VPS nem quem recebe os alertas (pergunta 15), e o
projeto não tem conta em serviço de monitoramento. O monitor precisa ficar fora da VPS: um monitor
na própria máquina cai junto com ela.

Alternativas consideradas: serviço dedicado (UptimeRobot, Better Stack, Healthchecks.io), mais
preciso, mas que exige conta, dono e configuração fora do repositório; Uptime Kuma na própria VPS
(não é externo); Uptime Kuma noutra máquina (mais uma máquina para manter).

## Decisão

- O workflow `.github/workflows/monitoramento.yml` roda a cada 5 minutos (o menor intervalo do
  GitHub) e consulta `GET <SGI_URL_SAUDE>`, esperando `"status":"UP"`. São três tentativas com 20
  segundos entre elas, para uma oscilação isolada não virar alerta.
- PENDENTE-CESJO(P15): a URL vem da variável de repositório `SGI_URL_SAUDE`, por exemplo
  `https://sgi.exemplo.org.br/saude`. Sem a variável, o job é pulado e nada é medido.
- **Alerta:** uma verificação que falha deixa a execução vermelha, e o GitHub envia e-mail de
  "workflow falhou" para quem ativou o agendamento. Quem recebe o alerta configura isso em Settings,
  Notifications, Actions.
- Trocar por um serviço dedicado depois só exige apontá-lo para a mesma URL `/saude`
  ([ADR 0066](0066-saude-publica-em-saude.md)) e apagar a variável.

## Consequências

- Monitoramento externo sem custo e sem conta nova, versionado no repositório.
- O GitHub pode atrasar ou pular execuções agendadas em horários de pico. Uma queda curta pode passar
  despercebida, e o alerta chega com minutos de atraso.
- Workflows agendados só rodam na branch padrão (`main`). Num repositório público, o GitHub desliga o
  agendamento depois de 60 dias sem atividade no repositório, e é preciso religar em Actions.
- O e-mail de falha vai para uma pessoa só (quem ativou o agendamento). Para avisar a equipe toda, ou
  por WhatsApp e Telegram, um serviço dedicado é melhor; essa troca é a primeira coisa a rever quando
  a pergunta 15 for respondida.
