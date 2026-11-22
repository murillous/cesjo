# ADR 0142 — Permissões do cadastro completo e da crença e saúde

**Status:** Provisório (PENDENTE-CESJO P8) (2026-11-22) · Issue #40

## Contexto

A matriz da seção 6 dá "ler e editar" ao Coordenador e à Equipe Técnica em "Usuários atendidos" e em
"Crença religiosa e dados de saúde", e "dados básicos" ao Administrativo e ao Consulta. O ADR 0015
assume que o Administrativo não vê crença nem dados de saúde (P8). Hoje o Administrativo e o Consulta
só têm `ATENDIDOS_LER_BASICO` (ADR 0082).

Alternativas consideradas: usar só `ATENDIDOS_LER` para tudo (a crença ficaria presa à ficha
completa, e dar a ficha ao Administrativo exporia a crença); uma permissão para editar e outra para
crença e saúde, separadas da leitura da ficha.

## Decisão

- `ATENDIDOS_EDITAR`: completar e corrigir o cadastro, sem crença nem saúde. Coordenador e Equipe
  Técnica.
- `ATENDIDOS_DADOS_SENSIVEIS`: ler e editar crença religiosa e saúde. Coordenador e Equipe Técnica.
- A ficha completa traz `dadosSensiveis` só para quem tem a permissão; para os outros vem nulo. No
  `PUT`, mandar `dadosSensiveis` sem a permissão responde 403; omitir deixa a crença e a saúde como
  estão.
- Ler a crença ou a saúde preenchidas registra leitura sensível de `cadastro_complementar` (ADR
  0023). CPF, NIS e RG não registram leitura: são cifrados no banco, mas não são dado sensível da
  LGPD, e a ficha completa já é só de quem tem `ATENDIDOS_LER`.
- Administrativo e Consulta seguem com a visão básica, sem filtros por gênero e bairro.

## Consequências

- Se a equipe decidir que o Administrativo vê a ficha sem crença e saúde, basta dar
  `ATENDIDOS_LER` (e, se for o caso, `ATENDIDOS_EDITAR`) ao perfil, no ponto com
  `PENDENTE-CESJO(P8)` em `acesso.Perfil`; a crença continua protegida pela permissão própria.
