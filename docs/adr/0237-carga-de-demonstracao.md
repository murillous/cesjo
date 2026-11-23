# ADR 0237 — Carga de dados fictícios para demonstração

**Status:** Aceito (2026-11-23)

## Contexto

Para apresentar o sistema à equipe CESJO e a parceiros, as telas precisam de um volume de dados que
encha listas, fichas, painéis e relatórios. O banco de desenvolvimento começa vazio, e a única massa
que existe é a do teste de desempenho ([ADR 0230](0230-volume-esperado-e-massa-simulada.md)), que não
serve para isso:

- os nomes são "Pessoa Fictícia 123 Silva" e os telefones são sequenciais;
- todas as colunas cifradas guardam o mesmo valor, então toda ficha mostra o mesmo texto;
- o volume é de estresse (10 vezes cinco anos de uso), não de uma apresentação;
- ela só roda dentro do teste, num banco descartável.

Alternativas consideradas:

- **Popular pelas APIs** (um script que entra com uma conta e chama os endpoints): respeita todas as
  regras, mas os serviços carimbam a data de agora em `criado_em`, nos históricos e na auditoria. Não
  dá para simular dois anos de uso, e os painéis por período ficariam vazios.
- **Script SQL avulso**: não consegue cifrar os textos com a chave local nem guardar os arquivos
  cifrados no diretório de arquivos.
- **Um módulo da aplicação, ativo só num perfil Spring, que grava direto no banco com o `Cifrador` e o
  módulo de arquivos**: datas livres ao longo de dois anos, cada texto cifrado em separado e documentos
  que abrem de verdade.

## Decisão

- Novo módulo `demonstracao`, com um `ApplicationRunner` ativo só no perfil `demo`. Ele roda antes da
  criação da primeira conta ([ADR 0014](0014-primeira-conta-de-coordenador.md)) e só grava quando o banco
  não tem nenhuma pessoa nem conta. Ele se recusa a subir junto com o perfil `prod`.
- Volume de cerca de **dois anos de uso** de um Centro-Dia com 60 vagas:

  | Dado | Quantidade |
  |---|---:|
  | Pessoas cadastradas na lista de espera | 220 |
  | Inseridas no serviço (56 ativas, 94 desligadas) | 150 |
  | Na fila (34 aguardando, 6 em avaliação com visita agendada) | 40 |
  | Saíram da lista sem inserção | 30 |
  | Colaboradores (3 desligados) | 15 |
  | Atividades e atendimentos | cerca de 390 |
  | Contatos e comunicações | cerca de 240 |
  | Parceiros, eventos institucionais e doações | 12 / cerca de 80 / 120 |

- **Uma conta por perfil** (`coordenador`, `equipe.tecnica`, `administrativo`, `consulta`), todas com a
  senha `demonstracao` (ou a de `sgi.demonstracao.senha`) e sem troca obrigatória no primeiro acesso.
- Os dados são coerentes entre si: histórico da fila na ordem certa, avaliação antes da inserção, PIA
  com as etapas em sequência e aprovado pelo coordenador, atividades só com quem estava ativo na data,
  e auditoria com a conta e o horário de cada registro, mais as entradas e saídas nos dias úteis.
- Nomes de pessoas são combinações aleatórias de nomes comuns. Bairros, cidade, CEP (prefixo 99) e
  instituições parceiras são fictícios. CPF tem dígito verificador válido, mas é sorteado.
- Laudos, comprovantes e parte dos ofícios são PDFs de uma página guardados pelo módulo de arquivos.
- O sorteio tem semente fixa, então a mesma data gera sempre a mesma massa.

## Consequências

- Para apresentar: `./mvnw spring-boot:run -Dspring-boot.run.profiles=demo` com o banco vazio. Para
  recomeçar, apague o volume do Postgres local.
- A carga grava direto nas tabelas e não passa pelas regras dos serviços. Toda migração que mudar uma
  tabela usada pela carga precisa ajustar o `GeradorDemonstracao`. O teste `CargaDemonstracaoTests` fica
  no `verify` normal e falha quando a carga deixa de gravar, de decifrar ou de abrir as telas.
- O teste reaproveita o banco isolado do teste de desempenho (`BancoDoDesempenho`, agora público).
- Os arquivos guardados pela carga geram registros de auditoria com a data da subida, sem conta, como
  qualquer envio sem requisição autenticada.
