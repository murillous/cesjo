# ADR 0015 — Matriz de perfis e permissões da Fundação

**Status:** Provisório (PENDENTE-CESJO P8, P17) (2026-11-20) · Issue #14

## Contexto

A seção 6 do planejamento traz a matriz inicial de perfis por área, com pontos marcados com (?) a
confirmar com a equipe CESJO. A pergunta 8 da seção 9 trata do acesso do Administrativo a laudos e
crença religiosa e do que são os "dados básicos" do perfil Consulta. Os demais pontos com (?) não
tinham pergunta própria; entraram como pergunta 17.

Na Fundação só existem as permissões das áreas de contas, auditoria, documentos anexados e
notificações. As permissões das outras áreas entram com os seus módulos, mas a suposição provisória
de cada ponto com (?) precisa ficar escrita desde já, para os módulos seguirem a mesma linha.

Alternativas consideradas: esperar as respostas (trava a Fundação); liberar os pontos com (?) (vai
contra o ADR 0006, que manda escolher o mais conservador).

## Decisão

Matriz da Fundação, em `acesso.Perfil`:

| Permissão | Coordenador | Equipe Técnica | Administrativo | Consulta |
|---|---|---|---|---|
| `CONTAS_GERENCIAR` | sim | não | não | não |
| `AUDITORIA_LER` | sim | não | não | não |
| `DOCUMENTOS_LER` | sim | sim | sim (P8) | não |
| `DOCUMENTOS_ENVIAR` | sim | não | sim (P8) | não |
| `NOTIFICACOES_LER` | sim | sim | sim | sim |

- O Coordenador envia documentos, embora a linha "Laudos e comprovantes" diga "ler", porque tem
  acesso total à lista de espera, cujo cadastro (RF001) inclui os anexos.
- Administrativo com laudos (P8): fica com enviar e ler. A opção mais conservadora seria não ler, mas
  o Administrativo tem acesso total à lista de espera e precisa anexar e conferir os documentos do
  cadastro. É a exceção ao critério conservador, e a primeira a rever quando a resposta chegar.

Suposições para as áreas que ainda não têm permissões (aplicadas quando o módulo entrar), todas pelo
lado conservador:

| Ponto com (?) | Suposição provisória | Pergunta |
|---|---|---|
| Crença religiosa e dados de saúde, Administrativo | não vê | P8 |
| "Dados básicos" do Consulta | só nome, situação, turno e datas | P8 |
| Atividades (RF010), Administrativo | sem acesso | P17 |
| Gestão institucional, Equipe Técnica | sem acesso | P17 |
| Gestão institucional, Administrativo | sem acesso | P17 |
| Relatórios e dashboards, Equipe Técnica | sem acesso | P17 |
| Relatórios e dashboards, Administrativo | sem acesso | P17 |
| Importação de planilhas, Administrativo | sem acesso | P17 |

## Consequências

- A matriz fica num lugar só (`acesso.Perfil`), com o marcador `PENDENTE-CESJO(P8)` na linha do
  Administrativo; os módulos futuros marcam as suas permissões provisórias no mesmo enum.
- Quando a equipe responder, muda o conjunto de permissões do perfil e entra um ADR que substitui
  este.
