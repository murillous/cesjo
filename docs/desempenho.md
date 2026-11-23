# Desempenho das consultas (RNF 3.2)

Resultado do teste de desempenho da issue #25. O RNF 3.2 pede no máximo 3 segundos de resposta para
consultas, com vários usuários ao mesmo tempo. A issue fixa o critério: com uma massa de pelo menos
10 vezes o volume esperado, listagens, relatórios e dashboards respondem em até 3 segundos com 20
usuários simultâneos.

**Resultado: critério atendido.** Na máquina de desenvolvimento, sem outra carga, o pior tempo de
todas as consultas foi 1,9 s (auditoria filtrada por login), e 2,7 s na execução mais lenta das
que passaram. Fora a auditoria, nenhuma consulta passou de 1 s. Com o índice proposto no
[ADR 0234](adr/0234-indice-proposto-para-o-log-de-auditoria.md), a auditoria por login cai para
0,3 s, e o pior tempo de todas as consultas fica em 1,5 s (auditoria sem filtro).

Decisões: [ADR 0230](adr/0230-volume-esperado-e-massa-simulada.md) (volume e massa),
[ADR 0231](adr/0231-como-o-teste-de-desempenho-mede.md) (como mede),
[ADR 0232](adr/0232-teste-de-desempenho-fora-do-verify.md) (como roda),
[ADR 0233](adr/0233-relatorios-e-dashboards-numa-transacao-de-leitura.md) (correção feita) e
[ADR 0234](adr/0234-indice-proposto-para-o-log-de-auditoria.md) (índice proposto).

## Como rodar de novo

```sh
cd backend
./mvnw verify -Pdesempenho
```

- Leva de 3 a 5 minutos: cerca de 1 minuto para subir o contexto, 1 a 3 minutos para gravar a massa e
  cerca de 30 segundos de carga.
- Usa um Postgres próprio, novo a cada execução: contêiner `postgres:18-alpine` com Docker, ou
  Postgres nativo do embedded-postgres sem Docker. Não toca no banco de desenvolvimento.
- A tabela de resultado sai no console e em `backend/target/desempenho/resultado.md`.
- Parâmetros opcionais: `-Ddesempenho.fator=10` (vezes o volume esperado), `-Ddesempenho.usuarios=20`,
  `-Ddesempenho.rodadas=5` e `-Ddesempenho.indices-propostos=true` (cria o índice do ADR 0234 só no
  banco do teste). Para um teste rápido: `-Ddesempenho.fator=1 -Ddesempenho.rodadas=1`.
- No GitHub: aba Actions, workflow **Desempenho**, botão *Run workflow*. O resultado aparece no resumo
  da execução e no artefato `resultado-desempenho`.
- Rode numa máquina sem outros builds ou testes em paralelo. Com a CPU ocupada por outros processos,
  os tempos sobem muito (veja "Execução com a máquina ocupada").

## Massa

Volume esperado de 5 anos de uso (provisório, P40), multiplicado por 10. Linhas gravadas:

| Tabela | Linhas |
|---|---:|
| `pessoa` | 6.000 |
| `inscricao_lista_espera` | 6.000 (1.500 na fila) |
| `historico_situacao_inscricao` | 10.500 |
| `insercao` | 2.000 (600 ativas) |
| `desligamento` | 1.400 |
| `cadastro_complementar` | 2.000 |
| `documento_pessoa` | 6.000 |
| `violacao_direito` | 3.000 |
| `colaborador` / `vinculo_colaborador` | 300 / 300 |
| `atividade` | 60.000 |
| `atividade_participante` | 240.000 |
| `atividade_profissional` | 90.000 |
| `comunicacao` | 60.000 |
| `parceiro` / `articulacao_parceria` | 400 / 5.000 |
| `evento_institucional` / `doacao` | 5.000 / 10.000 |
| `auditoria` | 2.000.000 (cerca de 800 MB com índices) |

As datas se espalham pelos últimos 5 anos. As colunas cifradas têm um valor cifrado de verdade,
repetido. Depois da gravação, o teste roda `vacuum analyze`.

## Máquina

- AMD Ryzen 5 4600G (6 núcleos, 12 processadores lógicos), 16 GB de RAM, Windows 11.
- Java 25.0.4.1, heap máximo de 4 GB.
- Postgres 18.7 nativo (embedded-postgres, sem Docker), configuração padrão (`shared_buffers` de
  128 MB), na mesma máquina da aplicação.
- Pool de conexões padrão da aplicação: Hikari com 10 conexões.

## Carga

- 20 usuários simultâneos (threads), autenticados como Coordenador, sem pausa entre requisições:
  sempre 20 requisições em andamento.
- Cada usuário faz 5 rodadas de todas as 24 consultas, em ordem aleatória: 100 requisições por
  consulta, 2.400 no total.
- Antes, 3 rodadas de aquecimento sem medição e a mediana de 5 execuções de cada consulta sozinha.
- Requisições por MockMvc: passam por Spring MVC, segurança, serviços, Hibernate e Postgres, mas não
  pela rede.

## Resultado (2026-11-23)

Sem o índice proposto, com a máquina sem outra carga. Duração da carga: 33 s.

| Grupo | Consulta | Sozinha (ms) | p50 (ms) | p95 (ms) | Máximo (ms) |
|---|---|---:|---:|---:|---:|
| Listagem | Lista de espera | 129 | 343 | 492 | 548 |
| Listagem | Lista de espera, busca por nome e situação | 23 | 141 | 299 | 419 |
| Listagem | Lista de espera, página 50 | 121 | 367 | 557 | 684 |
| Listagem | Usuários atendidos | 10 | 133 | 351 | 390 |
| Listagem | Usuários atendidos, filtros da ficha completa | 7 | 119 | 265 | 369 |
| Listagem | Usuários atendidos, busca por nome | 6 | 132 | 256 | 437 |
| Listagem | Atividades | 23 | 143 | 289 | 393 |
| Listagem | Atividades, último ano e tipo | 19 | 124 | 297 | 369 |
| Listagem | Atividades de um usuário | 16 | 126 | 340 | 477 |
| Listagem | Atividades de um profissional | 7 | 149 | 348 | 434 |
| Listagem | Comunicações | 14 | 157 | 349 | 477 |
| Listagem | Comunicações, período e contraparte | 21 | 151 | 298 | 418 |
| Listagem | Auditoria | 118 | 645 | 909 | 1.399 |
| Listagem | Auditoria, login e página 100 | 175 | 1.101 | 1.531 | 1.865 |
| Listagem | Auditoria, entidade, ação e período | 79 | 554 | 840 | 961 |
| Relatório | Mensal, mês corrente | 46 | 199 | 386 | 480 |
| Relatório | Mensal, 2 anos atrás | 33 | 189 | 318 | 427 |
| Relatório | Quadrimestral, quadrimestre corrente | 49 | 208 | 371 | 524 |
| Relatório | Anual, ano passado | 54 | 232 | 388 | 453 |
| Relatório | Transparência, 12 meses | 49 | 226 | 429 | 530 |
| Relatório | Intervenção (CREAS), 12 meses | 113 | 463 | 646 | 863 |
| Dashboard | Perfil dos atendidos | 5 | 126 | 236 | 434 |
| Dashboard | Atendimento, mês corrente | 5 | 113 | 244 | 355 |
| Dashboard | Atendimento, 5 anos | 25 | 191 | 300 | 414 |

Os tempos variam entre execuções na mesma máquina. Em quatro execuções com a máquina sem outra carga
(antes de o relatório de intervenção entrar na lista), o máximo da auditoria por login ficou entre
1,8 s e 2,7 s, e o das consultas fora da auditoria não passou de 1,3 s.

"Sozinha" é a mediana da consulta sem carga. A diferença para o p50 sob carga é quase toda espera:
pelo pool de 10 conexões e pela CPU, ocupada pelas consultas da auditoria.

### Com o índice proposto (ADR 0234)

Mesma massa e carga, com o índice `auditoria_login_cobertura_idx` criado só no banco do teste
(`-Ddesempenho.indices-propostos=true`), ainda sem o relatório de intervenção na lista. Duração
da carga: 25 s.

| Consulta | Sozinha (ms) | p50 (ms) | p95 (ms) | Máximo (ms) |
|---|---:|---:|---:|---:|
| Auditoria | 169 | 757 | 1.285 | 1.517 |
| Auditoria, login e página 100 | 17 | 130 | 264 | 334 |
| Auditoria, entidade, ação e período | 83 | 639 | 908 | 1.083 |
| Relatório mensal, mês corrente | 46 | 193 | 309 | 404 |
| Lista de espera | 128 | 340 | 469 | 663 |

As outras consultas ficaram entre 100 e 350 ms de p50 e abaixo de 750 ms de máximo.

### Antes da correção do ADR 0233

Na primeira versão, cada contagem dos relatórios pegava uma conexão do pool. Com a mesma massa e a
mesma carga, o p50 dos relatórios sob carga era de cerca de 500 ms (mensal: 509 ms; anual: 543 ms;
transparência: 534 ms), contra cerca de 250 ms depois. Sozinhos, os relatórios já levavam cerca de
50 ms: o tempo a mais era espera na fila do pool.

### Execução com a máquina ocupada

Numa execução com outros builds e testes rodando ao mesmo tempo na máquina (CPU em 100%, dezenas de
processos do Postgres de outros testes), a carga levou 80 s em vez de 30 a 40 s. A auditoria
passou de 3 s (máximo de 7,5 s filtrada por login, 5,0 s sem filtro e 4,4 s por entidade); as outras
consultas ficaram abaixo de 2,6 s. O teste falhou, como deve. Esse número não vale como resultado,
mas mostra que a auditoria é a primeira consulta a estourar quando a CPU falta.

## Achados

1. **Log de auditoria é o gargalo.** A listagem conta o total de registros a cada página. Na massa,
   o `explain (analyze, buffers)` mostrou:
   - filtro por login: o índice `auditoria_login_idx` acha as linhas, mas a contagem lê 42 mil
     páginas da tabela (cerca de 285 ms). Com o índice proposto, a contagem passa a Index Only Scan
     (cerca de 15 a 25 ms), e a página continua em cerca de 1 ms;
   - sem filtro: contar 2 milhões de linhas leva cerca de 160 ms (Parallel Seq Scan). Cresce com o
     log; índice não resolve. Se o log crescer muito além disso, a saída é não contar o total exato
     nessa listagem (ADR 0234);
   - entidade, ação e período: 100 a 200 ms quando a combinação tem poucas linhas.

   **Proposta (sem migração nesta issue):**

   ```sql
   CREATE INDEX auditoria_login_cobertura_idx
       ON auditoria (lower(conta_login), ocorrido_em DESC, id DESC) INCLUDE (conta_login);
   DROP INDEX auditoria_login_idx;
   ```

2. **Relatórios e dashboards esperavam pelo pool.** Corrigido: cada relatório e painel roda numa
   transação só de leitura, com uma conexão (ADR 0233).
3. **A lista de espera pagina em memória.** `FilaListaEspera` lê todas as inscrições do filtro e
   os nomes, para calcular a posição de cada candidato (ADR 0057). Com 6.000 inscrições, leva cerca
   de 130 ms sozinha e cresce de forma linear. Não é problema no volume testado; passa a ser se a
   lista histórica chegar a dezenas de milhares de inscrições.
4. **As outras listagens** (usuários atendidos, atividades, comunicações) filtram e paginam no
   banco e ficam em 6 a 30 ms sozinhas, com qualquer filtro.

## Limites do teste

- A carga é sem pausa e com todos os usuários como Coordenador, consultando inclusive a auditoria.
  O uso real é mais leve: só o Coordenador lê a auditoria, e cada pessoa lê a tela antes de clicar.
- Aplicação e banco rodam na mesma máquina, sem rede. A VPS de produção (ADR 0005) deve ter menos
  processadores que a máquina do teste. Antes da entrega, rode o teste lá ou numa máquina do mesmo
  porte.
- Exportações em PDF e Excel ficam de fora (downloads sob demanda).
