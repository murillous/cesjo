# ADR 0195 — Módulo Estatísticas e dashboard de perfil dos usuários atendidos

**Status:** Aceito (2026-11-22) · Issue #56

## Contexto

A issue #56 (RF017) pede um dashboard com os usuários atendidos por tipo de deficiência, gênero,
faixa etária e turno, atualizado a cada acesso. O planejamento punha dashboards, relatórios
estatísticos e exportação num módulo só, `relatorios`, que leria as tabelas dos outros módulos por
SQL próprio. A regra combinada para estas issues é mais estrita: os relatórios leem os outros módulos
só pelas APIs Java públicas deles.

Ao ligar o dashboard à API de `atendidos`, o Spring Modulith acusou um ciclo:
`atendidos -> listaespera -> relatorios -> atendidos`. A lista de espera já usa a exportação de
`relatorios` (ADR 0130), e `atendidos` usa a lista de espera.

Alternativas consideradas:

- **Ler as tabelas por SQL em `relatorios`**, como dizia o planejamento: sem ciclo, mas acopla o
  módulo ao esquema de todos os outros, e cada migração deles pode quebrar um relatório em silêncio.
- **Tirar a exportação de `relatorios` para um módulo novo**: renomeia pacotes que a lista de espera
  e os PRs abertos de outros módulos já importam.
- **Módulo novo `estatisticas` para dashboards e relatórios estatísticos**: `relatorios` segue como a
  base de exportação, sem mudar nada para quem já usa, e `estatisticas` depende dos módulos de
  negócio e de `relatorios`. Ninguém depende de `estatisticas`.
- **Contar em `estatisticas` por eventos**, guardando contagens próprias: não há eventos para tudo
  que precisa ser contado, e o painel deixaria de refletir o dado do momento.

## Decisão

- Módulo novo `br.org.cesjo.sgi.estatisticas`, com dashboards (RF017) e relatórios estatísticos
  (RF016). `relatorios` fica só com a exportação. A tabela de módulos do planejamento foi atualizada.
- Cada módulo de negócio expõe na sua API Java a leitura agregada que o painel precisa. Para este
  painel, `UsuariosAtendidos.perfisDosAtivos()` devolve um `PerfilAtendido` por inserção ativa (turno,
  tipo de deficiência, gênero e data de nascimento), sem nome nem id. A consulta é um SQL só, em
  `ConsultaAtendidos`, que já juntava `insercao`, `pessoa` e `cadastro_complementar` (ADR 0083).
- A faixa etária é calculada em `estatisticas` com `FaixaEtaria`, no dia de hoje no fuso do
  Centro-Dia. Quem não tem data de nascimento ou está fora de 18 a 59 anos fica numa barra própria.
- `GET /api/estatisticas/dashboards/perfil` devolve o total de ativos e uma lista de `Contagem`
  (`chave`, `rotulo`, `total`) por gráfico, com todas as categorias do enum, inclusive as com zero, e
  uma barra `SEM_VALOR` no fim só quando houver alguém sem a informação.
- Sem cache: o servidor conta a cada chamada e responde com `Cache-Control: no-store`. A tela busca
  ao abrir e tem um botão "Atualizar".
- Abrir um dashboard não é registrado na auditoria. O RNF 3.3 pede o registro de leitura de registro
  com dado sensível e de exportação; o painel só mostra contagens, sem identificar ninguém. A
  exportação de relatório estatístico (issue #58) é auditada pela base de exportação (ADR 0043).

## Consequências

- Um dashboard novo pede um método agregado na API do módulo dono do dado, e não uma consulta às
  tabelas dele.
- A contagem é feita em memória sobre uma linha por usuário ativo. Com a capacidade do Centro-Dia
  (dezenas de pessoas), cabe com folga na meta de 3 segundos (RNF 3.2). Se crescer muito, a
  agregação desce para o SQL de `ConsultaAtendidos` sem mudar o contrato do painel.
- Se a equipe quiser auditar a abertura dos dashboards, entra uma ação nova em `AcaoAuditoria` e uma
  migração na restrição da coluna `acao`.
