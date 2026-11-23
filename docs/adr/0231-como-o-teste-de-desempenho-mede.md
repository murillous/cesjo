# ADR 0231 — Como o teste de desempenho mede as consultas

**Status:** Aceito (2026-11-23) · Issue #25

## Contexto

O critério da issue #25 é "listagens, relatórios e dashboards respondem em até 3 segundos com 20
usuários simultâneos". Falta definir o que é "usuário simultâneo", onde a massa fica, como a
requisição é feita e qual número precisa ficar abaixo de 3 segundos.

Alternativas consideradas para a requisição:

- **HTTP real** (`RANDOM_PORT` e um cliente HTTP): mede também o Tomcat e a rede local, mas exige
  login de verdade com senha, sessão e CSRF em cada usuário.
- **MockMvc com `SecurityMockMvcRequestPostProcessors.authentication`**, como os testes dos
  relatórios: passa por todo o Spring MVC, a segurança, os serviços, o Hibernate e o Postgres, sem a
  camada de rede. A rede local e o Tomcat custam pouco perto das consultas.

Alternativas para o banco:

- **O Postgres compartilhado dos outros testes** (ADR 0008): a massa de milhões de linhas ficaria
  misturada com os dados dos outros testes.
- **Um Postgres novo só do teste**: isola a massa e começa sempre do mesmo estado.

## Decisão

- O teste `desempenho.DesempenhoConsultasTests` sobe o contexto completo com um Postgres 18 próprio
  (`BancoDoDesempenho`): contêiner `postgres:18-alpine` quando há Docker, instância nativa do
  embedded-postgres quando não há. O pool de conexões é o padrão da aplicação (Hikari, 10 conexões).
- As requisições são feitas com MockMvc, autenticadas como Coordenador, que vê todas as telas.
- Usuário simultâneo é uma thread que dispara as consultas uma depois da outra, **sem tempo de
  espera** entre elas. Com 20 threads, há sempre 20 requisições em andamento. É mais pesado que o uso
  real, em que cada pessoa lê a tela antes de clicar de novo.
- Cada usuário faz 5 rodadas de todas as consultas, em ordem aleatória (semente fixa por usuário),
  depois de 3 rodadas de aquecimento sem medição. Antes da carga, o teste mede cada consulta sozinha
  (mediana de 5).
- As consultas são as listagens (lista de espera, usuários atendidos, atividades, comunicações e
  auditoria, com e sem filtros e em páginas adiantadas), os relatórios (mensal, quadrimestral, anual
  e de transparência) e os dashboards (perfil e atendimento, num mês e em 5 anos).
- O teste falha se alguma requisição não responder 200 ou se o **tempo máximo** de alguma consulta
  passar de 3 segundos. O p50 e o p95 vão para o resultado, mas o critério é o máximo.
- O resultado sai no console e em `backend/target/desempenho/resultado.md`, com a máquina, a massa e
  os tempos.

## Consequências

- O teste é um limite superior do tempo real: carga sem pausa e banco com 10 vezes o volume.
- Não mede o tempo do navegador nem da rede até a VPS. Se a VPS tiver bem menos processadores que a
  máquina do teste, o resultado precisa ser refeito lá (`docs/desempenho.md` explica como).
- Exportações em PDF e Excel ficam de fora: são downloads sob demanda, não consultas de tela.
