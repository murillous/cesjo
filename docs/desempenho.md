# Desempenho das consultas (RNF 3.2)

Resultado do teste de desempenho da issue #25. O RNF 3.2 pede no máximo 3 segundos de resposta para
consultas, com vários usuários ao mesmo tempo. A issue fixa o critério: com uma massa de pelo menos
10 vezes o volume esperado, listagens, relatórios e dashboards respondem em até 3 segundos com 20
usuários simultâneos.

**Resultado: critério atendido.** Na máquina de desenvolvimento, sem outra carga, com os índices da
`V60` e a contagem limitada da auditoria, o pior tempo de todas as consultas foi 1,2 s (lista de
espera). A auditoria, que era o gargalo, ficou abaixo de 0,8 s com qualquer filtro. Antes dessas
mudanças, o pior tempo era 1,9 s (auditoria filtrada por login), e 2,7 s na execução mais lenta.

Decisões: [ADR 0230](adr/0230-volume-esperado-e-massa-simulada.md) (volume e massa),
[ADR 0231](adr/0231-como-o-teste-de-desempenho-mede.md) (como mede),
[ADR 0232](adr/0232-teste-de-desempenho-fora-do-verify.md) (como roda),
[ADR 0233](adr/0233-relatorios-e-dashboards-numa-transacao-de-leitura.md) (correção feita),
[ADR 0234](adr/0234-indice-proposto-para-o-log-de-auditoria.md) (índice proposto, substituído),
[ADR 0235](adr/0235-indices-das-consultas-lentas.md) (índices) e
[ADR 0236](adr/0236-contagem-limitada-no-log-de-auditoria.md) (contagem limitada da auditoria).

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
- Parâmetros opcionais: `-Ddesempenho.fator=10` (vezes o volume esperado), `-Ddesempenho.usuarios=20`
  e `-Ddesempenho.rodadas=5`. Para um teste rápido: `-Ddesempenho.fator=1 -Ddesempenho.rodadas=1`.
- Para ver o plano de cada consulta lenta, acrescente
  `"-Dspring.datasource.hikari.connection-init-sql=LOAD 'auto_explain'; SET auto_explain.log_min_duration = 15; SET auto_explain.log_analyze = on"`:
  o Postgres escreve no log do teste o plano de toda consulta acima de 15 ms.
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

Com os índices da `V60` (ADR 0235) e a contagem limitada da auditoria (ADR 0236), com a máquina sem
outra carga. Duração da carga: 16 s. Nenhuma requisição passou de 1,3 s.

| Grupo | Consulta | Sozinha (ms) | p50 (ms) | p95 (ms) | Máximo (ms) |
|---|---|---:|---:|---:|---:|
| Listagem | Lista de espera | 138 | 330 | 529 | 1.240 |
| Listagem | Lista de espera, busca por nome e situação | 25 | 85 | 208 | 637 |
| Listagem | Lista de espera, página 50 | 123 | 323 | 515 | 630 |
| Listagem | Usuários atendidos | 6 | 58 | 121 | 301 |
| Listagem | Usuários atendidos, filtros da ficha completa | 8 | 46 | 200 | 387 |
| Listagem | Usuários atendidos, busca por nome | 7 | 41 | 126 | 237 |
| Listagem | Atividades | 26 | 73 | 214 | 334 |
| Listagem | Atividades, último ano e tipo | 19 | 76 | 293 | 467 |
| Listagem | Atividades de um usuário | 18 | 66 | 197 | 307 |
| Listagem | Atividades de um profissional | 8 | 54 | 172 | 477 |
| Listagem | Comunicações | 12 | 60 | 263 | 620 |
| Listagem | Comunicações, período e contraparte | 23 | 89 | 297 | 387 |
| Listagem | Auditoria | 7 | 56 | 208 | 305 |
| Listagem | Auditoria, login e página 100 | 8 | 55 | 185 | 380 |
| Listagem | Auditoria, entidade, ação e período | 7 | 57 | 189 | 749 |
| Relatório | Mensal, mês corrente | 51 | 166 | 345 | 717 |
| Relatório | Mensal, 2 anos atrás | 35 | 127 | 286 | 477 |
| Relatório | Quadrimestral, quadrimestre corrente | 50 | 178 | 345 | 498 |
| Relatório | Anual, ano passado | 57 | 193 | 335 | 501 |
| Relatório | Transparência, 12 meses | 51 | 188 | 356 | 699 |
| Relatório | Intervenção (CREAS), 12 meses | 37 | 144 | 326 | 681 |
| Dashboard | Perfil dos atendidos | 6 | 48 | 132 | 365 |
| Dashboard | Atendimento, mês corrente | 6 | 48 | 142 | 229 |
| Dashboard | Atendimento, 5 anos | 26 | 106 | 277 | 382 |

### Antes dos índices e da contagem limitada

Mesma massa e carga, sem a `V60` e com a contagem exata da auditoria. Duração da carga: 33 s.

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

### Só com os índices, sem a contagem limitada

Com a `V60` e ainda com a contagem exata, as consultas filtradas da auditoria ficaram rápidas
(máximo de 380 a 706 ms por login e de 292 a 448 ms por entidade, ação e período, em três execuções).
A auditoria sem filtro, porém, piorou: máximo de 3,4 s, 3,9 s e 3,3 s, contra 1,5 s sem os índices
na mesma máquina e no mesmo dia. Contar o log inteiro é uma Parallel Seq Scan de 2 milhões de linhas
que nenhum índice evita. Com as outras consultas mais rápidas, mais contagens dessas rodavam ao mesmo
tempo e disputavam a CPU. Esse foi o motivo da contagem limitada (ADR 0236).

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

1. **O log de auditoria era o gargalo.** A listagem contava o total exato a cada página. O
   `auto_explain` na massa mostrou:
   - filtro por login: o índice antigo achava as linhas, mas a contagem lia a tabela (cerca de
     340 ms). Resolvido com `auditoria_login_cobertura_idx`, que permite Index Only Scan;
   - entidade, ação e período: o índice antigo, `(entidade, ocorrido_em)`, achava 100 mil linhas e
     descartava 67 mil de outras ações (cerca de 120 ms). Resolvido com
     `auditoria_entidade_acao_idx`;
   - sem filtro: contar 2 milhões de linhas é uma Parallel Seq Scan (cerca de 160 ms) e cresce com o
     log. Resolvido contando só até 10.000 (ADR 0236).
   Os dois índices estão na `V60` (ADR 0235). Depois deles, a auditoria fica em menos de 10 ms
   sozinha, com qualquer filtro.
2. **Relatórios e dashboards esperavam pelo pool.** Corrigido: cada relatório e painel roda numa
   transação só de leitura, com uma conexão (ADR 0233).
3. **A lista de espera pagina em memória.** `FilaListaEspera` lê todas as inscrições do filtro e
   os nomes, para calcular a posição de cada candidato (ADR 0057). Com 6.000 inscrições, leva cerca
   de 130 ms sozinha e cresce de forma linear. Não é problema no volume testado; passa a ser se a
   lista histórica chegar a dezenas de milhares de inscrições.
4. **O relatório de intervenção procurava a inserção vigente de cada caso** lendo a tabela inteira
   de inserções, porque não havia índice em `insercao.pessoa_id` (cerca de 110 ms). Resolvido com
   `ix_insercao_pessoa` na `V60`: sozinho, o relatório caiu de 113 ms para 37 ms.
5. **As outras listagens** (usuários atendidos, atividades, comunicações) filtram e paginam no
   banco e ficam em 6 a 30 ms sozinhas, com qualquer filtro. As contagens dos relatórios acima de
   15 ms já usam índice e gastam o tempo lendo as linhas do período. Um índice novo ganharia pouco e
   deixaria as gravações mais caras (ADR 0235).

## Limites do teste

- A carga é sem pausa e com todos os usuários como Coordenador, consultando inclusive a auditoria.
  O uso real é mais leve: só o Coordenador lê a auditoria, e cada pessoa lê a tela antes de clicar.
- Aplicação e banco rodam na mesma máquina, sem rede. A VPS de produção (ADR 0005) deve ter menos
  processadores que a máquina do teste. Antes da entrega, rode o teste lá ou numa máquina do mesmo
  porte.
- Exportações em PDF e Excel ficam de fora (downloads sob demanda).
