# Prompt Base para Implementação do MVP CESJO SGI

## 1. Papel

Atue como engenheiro de software full stack responsável por implementar um MVP visual, funcional, simples e executável localmente para o Sistema de Gestão de Informações do CESJO.

Priorize, nesta ordem:

1. funcionamento demonstrável;
2. fidelidade aos requisitos já definidos;
3. simplicidade;
4. integridade dos dados;
5. boa experiência administrativa;
6. facilidade de evolução posterior.

Não transforme o MVP em uma implementação completa do sistema.

## 2. Objetivo do MVP

Entregar uma aplicação local que permita demonstrar o fluxo principal da lista de espera:

```text
Cadastrar beneficiário
        ↓
Criar entrada na lista de espera
        ↓
Visualizar a fila em ordem cronológica
        ↓
Consultar detalhes e posição
```

O MVP deve possuir frontend e backend integrados, persistir os dados em SQLite e continuar funcionando depois de reiniciar a aplicação.

## 3. Documentação obrigatória

Antes de implementar, leia os documentos existentes nesta ordem:

1. `docs/requisitos/requisitos.md`;
2. `docs/requisitos/decisoesProjeto.md`;
3. `docs/requisitos/matrizRastreabilidade.md`;
4. `docs/requisitos/modeloLogico.md`;
5. `docs/requisitos/DER.md`;
6. `docs/requisitos/modeloFisico.md`;
7. `docs/guiaDeEstilo.md`;
8. `docs/requisitos/SGI.md`.

Quando houver divergência:

1. siga primeiro `requisitos.md`;
2. aplique as decisões específicas deste prompt somente ao MVP;
3. registre a divergência encontrada;
4. não invente regra de negócio para escondê-la.

## 4. Decisão temporária de banco de dados

Para este MVP, utilizar **SQLite** no lugar de PostgreSQL.

Esta decisão vale somente para desenvolvimento local e demonstração. Não altera a escolha de PostgreSQL para uma implantação futura.

Regras obrigatórias:

- armazenar o banco em arquivo local configurável;
- usar por padrão `data/cesjo-mvp.db`;
- permitir sobrescrever o caminho pela variável `SQLITE_DB_PATH`;
- adicionar o banco e o diretório de uploads ao `.gitignore`;
- ativar `PRAGMA foreign_keys = ON` em toda nova conexão;
- verificar a ativação das foreign keys ao iniciar a aplicação;
- não utilizar SQLite em implantação pública ou com múltiplas instâncias;
- não utilizar dados pessoais reais no ambiente de demonstração;
- evitar SQL desnecessariamente específico para facilitar a migração futura;
- manter schema e inicialização versionados no repositório.

## 5. Stack do MVP

### Backend

- Java 21.
- Spring Boot 3.5.
- Maven.
- Spring Web.
- Spring Validation.
- Spring Data JPA.
- Driver JDBC para SQLite.
- `hibernate-community-dialects` compatível com a versão do Hibernate gerenciada pelo Spring Boot.
- Dialeto `org.hibernate.community.dialect.SQLiteDialect`.

Não fixe manualmente uma versão do Hibernate diferente da fornecida pelo Spring Boot. A versão de `hibernate-community-dialects` deve permanecer compatível com `hibernate-core`.

Configurações mínimas:

- `spring.jpa.hibernate.ddl-auto=validate`;
- `spring.jpa.open-in-view=false`;
- inicialização por script SQL explícito e idempotente;
- inicialização de cada conexão com `spring.datasource.hikari.connection-init-sql=PRAGMA foreign_keys=ON` ou configuração equivalente validada;
- mensagens de erro sem stack trace ou detalhe interno para o frontend.

O dialeto SQLite do Hibernate é mantido pela comunidade. Validar sua compatibilidade já no bootstrap. Se houver incompatibilidade real, registrar o erro antes de substituir JPA ou criar um dialeto próprio.

### Frontend

- React.
- Vite.
- TypeScript.
- CSS comum com variáveis globais.
- `fetch` como única estratégia de comunicação HTTP.
- Sem gerenciador global de estado neste MVP.
- Sem biblioteca extensa de componentes.
- Sem framework CSS, salvo se já existir no projeto.

### Estrutura

Preferir uma estrutura simples:

```text
backend/
frontend/
data/                  # não versionado
docs/
```

## 6. Escopo funcional incluído

### 6.1 Estrutura visual

Implementar:

- cabeçalho simples com identificação do CESJO SGI;
- navegação para início, lista de espera e novo cadastro;
- layout responsivo;
- feedback de carregamento, sucesso e erro;
- página não encontrada simples;
- estados vazios;
- confirmação antes de abandonar formulário preenchido, quando aplicável.

### 6.2 Página inicial

Exibir somente informações úteis ao MVP:

- quantidade de entradas ativas na lista;
- botão para novo cadastro;
- acesso à lista de espera.

Não criar gráficos, relatórios ou cards sem função.

### 6.3 Cadastro do beneficiário

O formulário deve possuir:

- nome completo;
- data de nascimento;
- nome completo do responsável;
- telefone;
- tipo de deficiência;
- indicador de beneficiário do BPC;
- indicador de inscrição no CadÚnico;
- origem do encaminhamento;
- laudo médico;
- comprovante de residência;
- data de entrada na lista, preenchida inicialmente com a data atual e editável.

Valores permitidos para tipo de deficiência:

- sensorial;
- física;
- TEA;
- intelectual;
- múltipla;
- T21.

Valores permitidos para origem:

- CRAS;
- CREAS;
- Judiciário;
- Saúde;
- Educação;
- demanda espontânea.

Validações mínimas:

- campos obrigatórios não podem ser enviados vazios;
- data de nascimento não pode estar no futuro;
- idade deve estar entre 18 e 59 anos na data de entrada;
- telefone deve aceitar somente um formato coerente e ser normalizado antes da persistência;
- tamanho e extensão dos arquivos devem ser validados no backend;
- mensagens devem identificar o campo com problema sem expor detalhes internos.

### 6.4 Arquivos no MVP

Decisão técnica temporária:

- aceitar PDF, JPEG e PNG;
- limitar cada arquivo a 5 MB;
- armazenar em `data/uploads/`;
- gerar nome interno com UUID;
- nunca utilizar o nome original como caminho físico;
- persistir referência, nome original, tipo e tamanho;
- impedir acesso por caminho arbitrário;
- remover arquivo criado se a transação do cadastro falhar.

Esta estratégia é apenas local. Não a apresentar como solução definitiva de armazenamento ou segurança.

### 6.5 Lista de espera

Exibir tabela com:

- posição;
- nome do beneficiário;
- tipo de deficiência;
- data de entrada;
- BPC;
- CadÚnico;
- origem;
- ação para ver detalhes.

Comportamentos:

- ordenar somente por `data_entrada` crescente;
- usar o identificador da entrada como desempate técnico;
- calcular a posição durante a consulta;
- não armazenar posição no banco;
- permitir busca simples por nome;
- utilizar paginação apenas se ela já for necessária para manter a tela legível;
- apresentar estado vazio e erro de carregamento.

Embora BPC e CadÚnico sejam armazenados e exibidos, não aplicar prioridade automática no MVP. A combinação entre prioridade e cronologia continua pendente de validação do negócio.

### 6.6 Detalhes

Exibir:

- dados cadastrados do beneficiário;
- data de entrada;
- posição atual;
- indicadores BPC e CadÚnico;
- metadados dos documentos, sem expor o caminho físico;
- data e hora de criação do registro.

Não implementar edição, exclusão ou download dos documentos na primeira entrega.

## 7. Regras de persistência

Implementar, no mínimo:

- `ContaAcesso` como ator técnico local do MVP;
- `Beneficiario`;
- `ListaEspera`;
- metadados dos documentos, caso não sejam mantidos diretamente em `Beneficiario`.

Criar um ator técnico local por seed, utilizado pelo backend como responsável pelos cadastros enquanto autenticação estiver fora do escopo. Esse ator não deve possuir login exposto nem credencial utilizável.

Regras:

- um beneficiário pode ter histórico de entradas;
- não pode haver duas entradas ativas para o mesmo beneficiário;
- uma entrada é ativa enquanto `data_saida` for nula;
- não implementar exclusão física;
- criar beneficiário, documentos e entrada na fila em uma única transação;
- usar `INTEGER PRIMARY KEY` de forma compatível com SQLite;
- representar booleanos de forma consistente;
- utilizar constraints e índices para proteger regras que o banco puder garantir.

## 8. Contrato REST mínimo

Implementar contratos equivalentes a:

```text
POST /api/beneficiarios
GET  /api/beneficiarios/{id}
GET  /api/lista-espera
GET  /api/lista-espera/{id}/posicao
GET  /api/dashboard/resumo
```

O cadastro pode utilizar `multipart/form-data`, contendo uma parte JSON com os dados e partes separadas para laudo e comprovante.

Regras do contrato:

- usar DTOs de entrada e saída;
- nunca devolver entidade JPA diretamente;
- nunca devolver caminho físico de arquivo;
- utilizar status HTTP coerentes;
- erros de validação devem possuir formato consistente;
- retornar `404` para recurso inexistente;
- retornar `409` para tentativa de entrada ativa duplicada;
- manter nomes e formatos estáveis entre frontend e backend.

## 9. Direção visual do MVP

Utilizar `guiaDeEstilo.md` como referência visual, com estas decisões adicionais para tornar o MVP implementável:

### 9.1 Cores

Definir variáveis CSS globais, no mínimo:

```css
:root {
  --color-background: #ffffff;
  --color-surface: #f1f0e8;
  --color-brand: #10aaa6;
  --color-action: #34545c;
  --color-accent: #f2ea65;
  --color-text: #34545c;
  --color-text-secondary: #6b6b6b;
  --color-border-soft: #dbddd7;
  --color-border-control: #6b6b6b;
  --color-success: #067647;
  --color-warning: #854d0e;
  --color-danger: #b42318;
  --color-info: #175cd3;
  --color-focus: #34545c;
}
```

Regras:

- não usar `--color-brand` para texto pequeno sobre branco;
- não usar `--color-brand` sozinho para bordas, foco ou ícones funcionais sobre branco;
- usar `--color-action` para botões primários e links textuais;
- usar texto escuro sobre o amarelo de destaque;
- não utilizar somente cor para comunicar estado;
- cumprir WCAG 2.2 nível AA no escopo implementado.

### 9.2 Tipografia e espaçamento

- usar `Inter, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif`;
- não depender de download externo de fonte;
- corpo com `1rem` e altura de linha mínima de `1.5`;
- escala de espaçamento: 4, 8, 12, 16, 24, 32 e 48 px;
- largura máxima do conteúdo: 1200 px;
- foco sempre visível;
- área interativa adequada para toque.

### 9.3 Responsividade

Utilizar como referência:

- até 639 px: mobile;
- 640 a 899 px: tablet;
- 900 a 1199 px: desktop;
- 1200 px ou mais: desktop amplo.

No mobile:

- formulário em uma coluna;
- ações principais ocupam largura disponível quando necessário;
- tabela pode utilizar rolagem horizontal dentro de uma região identificada;
- nenhuma página deve causar rolagem horizontal global;
- navegação deve ser acessível por teclado e toque.

## 10. Acessibilidade mínima obrigatória

- HTML semântico;
- `label` associado a todo campo;
- indicação textual de obrigatoriedade;
- foco visível;
- ordem de tabulação lógica;
- operação completa por teclado;
- mensagens de erro associadas aos campos;
- mensagens de sucesso e erro anunciáveis por tecnologia assistiva;
- contraste mínimo validado;
- título de página e hierarquia correta de headings;
- botões com nomes claros;
- ícones decorativos ocultos de tecnologia assistiva;
- não depender apenas de placeholder;
- respeitar `prefers-reduced-motion` se houver animação.

## 11. Fora do escopo

Não implementar neste MVP:

- RF003, gestão e notificação de vagas;
- visitas domiciliares;
- escutas;
- PIA;
- admissão no serviço;
- desligamentos;
- violações de direitos;
- atividades e comunicações;
- gestão institucional;
- relatórios completos;
- dashboards analíticos;
- integrações externas;
- importação ou exportação;
- autenticação real;
- autorização por perfil;
- envio de e-mail, SMS ou WhatsApp;
- exclusão de beneficiários;
- infraestrutura em nuvem;
- Docker, se não for necessário para execução local;
- migração para PostgreSQL;
- arquitetura distribuída;
- biblioteca de componentes criada antecipadamente.

## 12. Organização da implementação

Executar em etapas verificáveis:

1. inspecionar o repositório e confirmar a ausência ou presença de código;
2. criar a estrutura mínima do backend;
3. configurar SQLite e validar foreign keys;
4. criar schema, entidades e persistência;
5. implementar cadastro transacional;
6. implementar lista, detalhe, posição e resumo;
7. criar a estrutura mínima do frontend e os tokens visuais;
8. implementar navegação e página inicial;
9. implementar formulário integrado;
10. implementar lista e detalhes integrados;
11. executar testes e revisão responsiva;
12. documentar execução local e limitações do MVP.

Não criar todos os componentes antes de serem utilizados. Extrair componente compartilhado somente quando houver reutilização real ou responsabilidade visual clara.

## 13. Validação obrigatória

### Backend

- compilação Maven;
- testes da validação de idade;
- teste de entrada ativa duplicada;
- teste da ordenação e posição;
- teste transacional do cadastro;
- teste de validação de arquivo;
- verificação de `PRAGMA foreign_keys`;
- teste de persistência após reinício, quando possível.

### Frontend

- build de produção;
- cadastro integrado com dados fictícios;
- validações de formulário;
- estados loading, vazio, sucesso e erro;
- navegação completa por teclado;
- revisão em largura de 360 px e desktop;
- ausência de rolagem horizontal global;
- contraste das combinações efetivamente utilizadas.

### Fluxo de aceite

O MVP estará concluído quando for possível:

1. iniciar backend e frontend seguindo o README;
2. cadastrar um beneficiário fictício com os dois documentos;
3. reiniciar a aplicação sem perder o registro;
4. visualizar o beneficiário na lista cronológica;
5. consultar sua posição;
6. abrir os detalhes sem exposição do caminho dos arquivos;
7. repetir o fluxo em mobile e desktop;
8. receber mensagens compreensíveis diante de dados inválidos.

## 14. Segurança e limites de uso

Este MVP é somente local e não deve ser publicado ou utilizado com dados reais porque não inclui autenticação, autorização, auditoria completa, política definitiva de arquivos, backup ou recuperação de desastre.

Exibir essa limitação no README do projeto.

Não registrar em logs:

- payload completo;
- nome de arquivo enviado;
- dados pessoais;
- conteúdo de documentos;
- caminhos físicos;
- stack trace devolvido ao cliente.

## 15. Forma de trabalho e resposta

Antes de alterar arquivos:

1. informe a etapa atual;
2. delimite o que será e o que não será feito;
3. liste os arquivos envolvidos;
4. identifique qualquer conflito documental relevante.

Depois da implementação:

1. informe os arquivos criados ou alterados;
2. informe os comandos de validação executados;
3. apresente os resultados reais;
4. registre limitações conhecidas;
5. pare ao concluir a etapa atual.

Não afirme que algo funciona sem validação executada.

## 16. Comando de início sugerido

Utilize este prompt junto da seguinte solicitação:

> Execute a Etapa 1 do MVP CESJO SGI. Inspecione o repositório, proponha a estrutura mínima de backend e frontend e implemente somente o bootstrap executável, sem avançar para o cadastro.
