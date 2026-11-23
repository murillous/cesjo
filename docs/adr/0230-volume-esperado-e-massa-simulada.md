# ADR 0230 — Volume esperado e massa simulada do teste de desempenho

**Status:** Provisório (PENDENTE-CESJO P40) (2026-11-23) · Issue #25

## Contexto

A issue #25 (RNF 3.2) pede uma massa simulada de pelo menos 10 vezes o volume esperado. O Documento
de Requisitos só diz "tempo de resposta máximo de 3 segundos para consultas" e "suporte a múltiplos
usuários simultâneos", sem número de pessoas, de atividades ou de anos de histórico. O planejamento
fala em "centenas de pessoas atendidas, dezenas de contas de acesso". A capacidade do serviço é
configurável e a equipe não informou o número (P3 só decidiu que é um total, ADR 0092).

Alternativas consideradas:

- **Esperar o número da equipe**: trava a issue sem necessidade, porque o volume é só um parâmetro
  do teste.
- **Usar uma massa arbitrária grande** (por exemplo, um milhão de linhas em cada tabela): não diz
  nada sobre o uso real e deixa a geração lenta.
- **Estimar o volume de 5 anos de uso a partir do porte do serviço e multiplicar por 10**: dá uma
  margem clara e fácil de refazer quando a equipe responder.

## Decisão

O volume esperado é o de **5 anos de uso** de um Centro-Dia com cerca de 60 vagas (30 por turno),
fila de espera de algumas centenas de pessoas e cerca de 20 contas de acesso:

| Dado | Volume esperado (5 anos) | Massa do teste (10 vezes) |
|---|---:|---:|
| Pessoas cadastradas na lista de espera | 600 | 6.000 |
| Inserções no serviço (30% ativas, o resto desligado) | 200 | 2.000 |
| Violações de direitos | 300 | 3.000 |
| Colaboradores | 30 | 300 |
| Atividades e atendimentos (cerca de 100 por mês) | 6.000 | 60.000 |
| Participantes por atividade | 4 | 4 (240.000 linhas) |
| Contatos e comunicações (cerca de 100 por mês) | 6.000 | 60.000 |
| Registros do log de auditoria (cerca de 110 por dia) | 200.000 | 2.000.000 |
| Parceiros / articulações / eventos / doações | 40 / 500 / 500 / 1.000 | 400 / 5.000 / 5.000 / 10.000 |

- Os números ficam num lugar só, `MassaSimulada.ESPERADO` no teste, com o marcador
  `PENDENTE-CESJO(P40)`. O fator 10 é o padrão e pode ser trocado por `-Ddesempenho.fator`.
- A massa é gravada direto no banco com `generate_series`, com ids derivados do número da linha
  (`md5('pessoa' || n)::uuid`). As colunas cifradas recebem um valor cifrado de verdade pelo
  `Cifrador` do projeto, gerado uma vez e repetido em todas as linhas.
- Depois da gravação, o teste roda `vacuum analyze`, como o autovacuum faria num banco em uso.

## Consequências

- Quando a equipe responder a P40, basta mudar `MassaSimulada.ESPERADO` e rodar o teste de novo.
- A massa só tem dados fictícios e não passa pelas regras das APIs (por exemplo, a data de cadastro
  de quem saiu da lista é sempre 60 dias antes da saída). Para medir consultas, isso basta; para
  testar regra de negócio, os testes de integração de cada módulo continuam valendo.
- O log de auditoria é a maior tabela da massa (cerca de 800 MB com índices) e é o que mais pesa
  nos tempos, como mostra o [resultado](../desempenho.md).
