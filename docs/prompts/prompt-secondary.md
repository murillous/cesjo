# PROMPT AUXILIAR — GUIA DE EXECUÇÃO DO PROJETO

## 1. Finalidade

Este documento é uma referência operacional complementar ao:

> PROMPT MESTRE — SISTEMA DE GESTÃO DO CENTRO-DIA.

Ele orienta:

* análise documental;
* definição de microetapas;
* inspeção do código;
* controle de escopo;
* implementação;
* validação;
* tratamento de inconsistências;
* escolha da menor solução suficiente.

Este documento NÃO constitui fonte de regras de negócio.

As regras de negócio devem vir dos documentos oficiais do sistema.

---

# 2. Fluxo padrão de uma microetapa

Sempre que houver uma microetapa autorizada, siga esta sequência:

```text
1. Entender
↓
2. Consultar documentação
↓
3. Delimitar escopo
↓
4. Inspecionar código existente
↓
5. Identificar dependências mínimas
↓
6. Implementar
↓
7. Validar
↓
8. Encerrar
```

Não pule diretamente para código quando houver informações documentais que precisem ser verificadas.

---

# 3. Pergunta principal

Antes de qualquer implementação, responda internamente:

> Qual é a menor alteração correta capaz de concluir esta microetapa?

A resposta deve guiar todo o trabalho.

---

# 4. Definition of Ready

Antes de implementar, verifique se existe informação suficiente.

Uma microetapa está pronta para implementação quando for possível identificar:

* objetivo;
* comportamento esperado;
* documentação relevante;
* arquivos ou componentes envolvidos;
* dados necessários;
* critério mínimo de validação.

Não exija especificações irrelevantes para a tarefa.

Se uma ausência documental não impedir a implementação, não transforme isso em bloqueio artificial.

---

# 5. Controle de escopo

Antes de implementar, crie mentalmente duas listas.

## Dentro do escopo

Tudo que é indispensável para concluir o objetivo.

## Fora do escopo

Tudo que pode ser realizado posteriormente.

Exemplo:

Microetapa:

> Implementar consulta da lista de espera.

Dentro:

* repository necessário;
* consulta;
* service se houver lógica;
* endpoint;
* DTO necessário;
* validação da consulta.

Fora:

* cadastro;
* edição;
* exclusão;
* alteração de status;
* exportação;
* relatório;
* gráfico;
* frontend.

---

# 6. Teste de necessidade

Antes de criar qualquer elemento novo, aplique:

### Pergunta 1

Existe requisito atual para isso?

### Pergunta 2

Existe regra documental para isso?

### Pergunta 3

Existe necessidade técnica imediata?

### Pergunta 4

A microetapa não funciona corretamente sem isso?

Se todas forem NÃO:

> não crie.

---

# 7. Teste anti-overengineering

Antes de adicionar uma solução mais sofisticada, verifique:

* existe problema concreto?
* a solução simples falhou?
* existe ganho real?
* a complexidade adicional será utilizada agora?

Caso contrário, escolha a solução simples.

---

# 8. Inspeção do código existente

Antes de criar um arquivo:

1. localize arquivos relacionados;
2. leia apenas o contexto necessário;
3. verifique se já existe comportamento equivalente;
4. identifique convenções existentes;
5. preserve padrões já utilizados quando adequados.

Não faça varredura completa do projeto sem necessidade.

---

# 9. Regra para arquivo novo

Crie arquivo somente se possuir responsabilidade necessária e distinta.

Pergunte:

> Este código realmente precisa existir em um arquivo separado?

Se não, mantenha a solução localizada.

---

# 10. Regra para arquivo existente

Ao alterar:

```text
ANTES
arquivo existente
↓
ALTERAÇÃO NECESSÁRIA
↓
DEPOIS
mesmo arquivo + mínima mudança
```

Evite:

```text
pequena necessidade
↓
reescrita completa
↓
refatoração
↓
mudança de arquitetura
↓
novas dependências
```

---

# 11. Orçamento de arquivos

Utilize como referência:

```text
1 arquivo  → excelente para microetapa pequena
2 arquivos → normal
3 arquivos → normal
4+         → revisar se existem múltiplas responsabilidades
```

Essa orientação NÃO deve impedir uma implementação tecnicamente correta.

Não divida artificialmente arquivos que precisam mudar juntos.

---

# 12. Backend — checklist para entidade

Quando a microetapa envolver uma entidade:

Verificar:

* tabela;
* nome;
* PK;
* tipo da PK;
* colunas;
* tipos;
* nullable;
* unique;
* FKs;
* cardinalidades;
* constraints;
* enums;
* nomes definidos no modelo físico.

Implementar somente o mapeamento necessário.

Não criar automaticamente:

* repository;
* service;
* controller;
* DTO;
* endpoints.

---

# 13. Backend — checklist para Repository

Antes de criar método customizado, verificar se Spring Data já fornece:

* `findById`;
* `findAll`;
* `save`;
* `existsById`;
* `deleteById`.

Métodos derivados são preferíveis para consultas simples.

Não utilizar automaticamente:

* Specifications;
* Criteria API;
* QueryDSL;
* repository genérico;
* implementação customizada.

Utilize esses recursos somente quando a consulta justificar.

---

# 14. Backend — checklist para Service

Um service deve existir quando houver responsabilidade real.

Exemplos:

* aplicar regra de negócio;
* validar transição;
* coordenar múltiplas operações;
* garantir comportamento transacional;
* transformar uma operação de domínio.

Não criar service somente para envolver:

```java
repository.findAll();
```

se não houver benefício arquitetural ou regra real.

---

# 15. Backend — checklist para Controller

Controller deve:

* receber requisição;
* validar entrada;
* delegar comportamento;
* devolver resposta HTTP.

Evite:

* regra complexa;
* SQL;
* acesso direto a detalhes de persistência;
* transformação excessiva.

---

# 16. Backend — DTO

Antes de criar um DTO, pergunte:

> Expor diretamente a entidade seria inadequado?

Se SIM, use DTO.

Motivos comuns:

* campos internos;
* controle da entrada;
* resposta específica;
* validação;
* relacionamentos que não devem ser serializados;
* prevenção de alteração de campos indevidos.

Não crie automaticamente:

```text
Entity
EntityRequest
EntityResponse
EntityCreateRequest
EntityUpdateRequest
EntityDTO
EntityMapper
```

quando um ou dois contratos simples resolvem.

---

# 17. Backend — Mapper

Não crie mapper dedicado para transformações triviais.

Uma conversão pequena pode ser realizada de forma explícita.

Crie mapper quando:

* conversão se repetir;
* houver muitos campos;
* houver ganho real de organização.

Não adicionar biblioteca de mapping automaticamente.

---

# 18. Backend — tratamento de erros

Trate erros reais previstos pela microetapa.

Não construa antecipadamente uma hierarquia extensa de exceptions.

Utilize mensagens:

* claras;
* sem stack trace para cliente;
* sem dados sensíveis;
* consistentes.

---

# 19. Banco — checklist

Antes de alterar persistência:

### Estrutura

* tabela identificada?
* PK identificada?
* FK identificada?
* tipos compatíveis?
* campos obrigatórios?
* constraints?

### Documentação

* DER concorda?
* modelo lógico concorda?
* SQL concorda?

### Hibernate

* mapeamento corresponde ao schema?
* existe tentativa de alterar schema automaticamente?

### PostgreSQL

* SQL compatível?
* tipos compatíveis?
* auto incremento compatível?
* sequences?
* funções específicas?

---

# 20. Uso do PostgreSQL

PostgreSQL é a baseline definida para o projeto porque oferece:

* modelo relacional robusto;
* integridade referencial;
* integração madura com Spring;
* bom suporte a relatórios;
* estabilidade;
* baixo custo;
* ausência de licença proprietária obrigatória.

Consulte `modeloFisico.md` e preserve a regra:

> documentação reconciliada > suposição técnica.

---

# 21. Índices

Não crie índices com base em:

> talvez essa coluna seja consultada muito.

Primeiro identifique:

* consulta real;
* filtro real;
* ordenação;
* volume;
* possível necessidade.

PKs e constraints já podem gerar índices conforme o SGBD.

Não duplique índices desnecessariamente.

---

# 22. Frontend — checklist de página

Para uma página administrativa, avaliar somente quando aplicável:

* título;
* ação principal;
* tabela/listagem;
* formulário;
* busca;
* filtros;
* paginação;
* loading;
* erro;
* estado vazio;
* confirmação.

Não implementar todos automaticamente.

---

# 23. Frontend — estrutura sugerida

Uma estrutura inicial simples:

```text
src/
├── components/
├── pages/
├── services/
├── hooks/
└── utils/
```

Somente crie diretórios efetivamente necessários.

Não crie pastas vazias apenas para antecipar organização futura.

---

# 24. Frontend — componentes

Crie componente reutilizável quando:

* existir reutilização real;
* possuir responsabilidade visual clara;
* reduzir complexidade da página.

Não transforme cada:

* botão;
* label;
* `div`;
* input;

em componente independente sem motivo.

---

# 25. Frontend — estado

Comece com recursos nativos do React.

Exemplos:

* `useState`;
* `useEffect`;
* props;
* estado local.

Adicione gerenciamento global somente quando houver estado realmente compartilhado e difícil de administrar pelas ferramentas existentes.

---

# 26. Frontend — chamadas HTTP

Escolha UMA abordagem:

```text
fetch
```

ou:

```text
Axios
```

Centralize chamadas quando isso trouxer benefício claro.

Não crie uma arquitetura de SDK completa para poucas requisições.

---

# 27. Frontend — formulário

Ao implementar formulário:

* utilizar campos previstos;
* indicar obrigatoriedade;
* validar dados necessários;
* mostrar erro;
* impedir submissões claramente inválidas;
* informar sucesso.

Não inventar campos auxiliares.

---

# 28. Frontend — tabela

Ao implementar uma tabela:

mostrar apenas campos necessários para consulta administrativa.

Não transforme todos os campos existentes no banco em colunas.

Priorizar legibilidade.

Detalhes podem existir em visualização específica quando necessário.

---

# 29. Dashboard

Antes de criar um card ou gráfico, perguntar:

> Qual decisão ou atividade administrativa este elemento ajuda a realizar?

Se não houver resposta concreta:

> não implementar.

---

# 30. Segurança — checklist mínimo

Antes de concluir uma funcionalidade, verificar:

* dados pessoais aparecem em logs?
* erro expõe detalhe interno?
* entrada é validada?
* credencial foi colocada no código?
* exemplo contém dados reais?
* API devolve campos desnecessários?

Não transformar esta verificação em projeto completo de segurança.

---

# 31. Validação proporcional

Escolher o menor nível que realmente demonstra a correção.

## Nível 1 — compilação

Adequado para algumas alterações estruturais.

```bash
./mvnw compile
```

## Nível 2 — teste específico

Adequado para regra isolada.

```bash
./mvnw test -Dtest=NomeDoTeste
```

## Nível 3 — suíte backend

Quando necessário.

```bash
./mvnw test
```

## Nível 4 — frontend

```bash
npm run build
```

Testes adicionais somente quando necessários.

---

# 32. Validação manual de API

Quando útil, apresentar exemplo pequeno.

Exemplo:

```http
GET /api/lista-espera
```

Informar somente:

* requisição necessária;
* resultado esperado;
* status esperado.

Não criar dezenas de cenários se a microetapa não exigir.

---

# 33. Tratamento de erro de compilação

Se surgir erro:

1. identificar causa;
2. verificar se pertence à microetapa;
3. corrigir se foi provocado pela alteração atual;
4. não iniciar refatoração ampla.

Se o erro for anterior à microetapa:

> registrar claramente que é preexistente.

Não assumir responsabilidade por corrigir todo o projeto.

---

# 34. Conflitos documentais

Utilizar formato:

```text
CONFLITO IDENTIFICADO

Documento A:
...

Documento B:
...

Diferença:
...

Impacto:
...

Interpretação mínima sugerida:
...
```

Não modificar o sistema para esconder a divergência.

---

# 35. Informação ausente

Quando uma informação estiver ausente, classificar:

```text
INFORMAÇÃO NÃO DEFINIDA

Item:
...

Impacto:
...

Bloqueia esta microetapa?
SIM / NÃO
```

Se NÃO bloquear:

continue com o que estiver definido.

Se SIM:

não invente.

---

# 36. Decisões técnicas

Uma decisão técnica pode ser tomada sem transformar tudo em dúvida para o usuário.

Exemplos:

* `ResponseEntity`;
* `Optional`;
* nome coerente de método;
* organização local do código;
* utilização de recurso padrão Spring.

Não peça confirmação para cada detalhe trivial.

---

# 37. Regra de perguntas

Não interrompa a execução por dúvidas pequenas que possam ser resolvidas por:

* documentação;
* código existente;
* convenções já estabelecidas;
* decisão técnica segura.

Solicite definição somente quando a ausência afetar:

* regra de negócio;
* dados;
* comportamento;
* integridade;
* resultado esperado.

---

# 38. Refatoração

Refatorar somente quando:

* for necessária para executar a microetapa;
* eliminar problema concreto;
* reduzir complexidade diretamente relacionada.

Não refatorar porque:

> o código poderia ficar mais bonito.

Registrar melhorias opcionais separadamente.

---

# 39. Dependência externa — decisão

Antes de adicionar biblioteca:

```text
Problema:
...

Pode ser resolvido nativamente?
SIM / NÃO

A biblioteca já existe no projeto?
SIM / NÃO

Benefício concreto:
...

Complexidade adicionada:
...
```

Se o benefício não superar a complexidade:

> não adicionar.

---

# 40. Critério para quebrar uma tarefa

Quebrar uma tarefa quando houver mais de uma responsabilidade independente.

Exemplo:

```text
CRUD Lista de Espera
```

não é uma microetapa adequada.

Pode ser:

```text
1. Mapear estrutura.
2. Criar entidade.
3. Criar persistência.
4. Implementar cadastro.
5. Implementar consulta.
6. Implementar atualização.
7. Implementar mudança de status.
8. Construir listagem frontend.
9. Construir formulário.
10. Integrar.
```

A decomposição final deve seguir a documentação real.

---

# 41. Microetapas frontend e backend

Evite misturar frontend e backend na mesma microetapa quando eles puderem ser validados separadamente.

Exemplo:

```text
B1 — Endpoint de consulta
F1 — Estrutura visual
F2 — Tabela
F3 — Integração da tabela ao endpoint
```

A integração naturalmente pode envolver os dois lados quando indispensável.

---

# 42. Template de microetapa

Ao iniciar uma nova microetapa, utilizar mentalmente:

```text
MICROETAPA:
[nome]

OBJETIVO:
[uma frase]

ENTRADA:
[documentos/código necessário]

SAÍDA:
[resultado verificável]

INCLUÍDO:
- ...

FORA:
- ...

VALIDAÇÃO:
[como demonstrar que terminou]
```

---

# 43. Formato de resposta

Utilize:

```markdown
## Microetapa atual

**Nome:** ...

**Objetivo:** ...

**Resultado esperado:** ...

## Escopo

### Será feito

- ...

### Não será feito agora

- ...

## Base documental

- ...

## Decisões

- ...

## Arquivos envolvidos

### Criar

- ...

### Alterar

- ...

## Implementação

...

## Validação

...

## Critério de conclusão

...

## Resultado

...

## Próxima microetapa sugerida

Microetapa X.X — ...
```

---

# 44. Respostas curtas

Evite explicações teóricas longas durante desenvolvimento.

Explique detalhes somente quando:

* houver decisão relevante;
* existir trade-off;
* houver conflito;
* o comportamento não for óbvio;
* o usuário solicitar aprofundamento.

Priorize:

> código + justificativa curta + validação.

---

# 45. O que NÃO fazer ao concluir

Depois de atingir o objetivo, não diga:

> Aproveitei e também implementei...

Isso normalmente indica expansão indevida de escopo.

Também não:

* prepare endpoint futuro;
* adicione campo futuro;
* crie classe vazia;
* crie TODO especulativo;
* implemente botão ainda sem função;
* crie tabela para requisito futuro;
* adicione dependência preventiva.

---

# 46. Observações futuras

Se durante uma microetapa surgir algo relevante fora do escopo:

```text
Observação para etapa futura:
[descrição curta]
```

Não implementar.

Não transformar a observação em nova tarefa automaticamente.

---

# 47. Critério final de qualidade

Antes de encerrar, verificar:

### Requisito

Foi atendido?

### Documentação

Foi respeitada?

### Escopo

Algo desnecessário foi implementado?

### Código

A solução está simples?

### Banco

O schema foi preservado?

### Validação

Existe evidência suficiente de funcionamento?

Se estiver tudo adequado:

> encerrar.

---

# 48. Comandos úteis de execução

Backend Maven, quando aplicável:

```bash
./mvnw compile
./mvnw test
./mvnw spring-boot:run
```

Frontend:

```bash
npm install
npm run dev
npm run build
```

Use somente comandos pertinentes à microetapa.

Não execute instalação ou alteração de dependências sem necessidade.

---

# 49. Exemplos de autorização

Autorização válida:

```text
Execute somente a Microetapa 1.2 — Criar entidade da Lista de Espera.
```

Outro exemplo:

```text
Implemente apenas o endpoint de consulta definido na Microetapa 1.5.
Não avance depois disso.
```

Não constitui autorização:

```text
A próxima poderia ser o repository.
```

Nem:

```text
Depois precisaremos criar o frontend.
```

---

# 50. Primeira análise do projeto

A primeira análise da Lista de Espera deverá cruzar:

```text
REQUISITOS
    ↓
SGI
    ↓
DER
    ↓
MODELO LÓGICO
    ↓
MODELO FÍSICO
```

O objetivo é identificar como o conceito de Lista de Espera aparece em cada documento.

Não programar nessa análise.

---

# 51. Matriz recomendada para análise documental

Quando útil:

| Item        | Requisito | SGI | DER | Lógico | SQL | Situação             |
| ----------- | --------- | --- | --- | ------ | --- | -------------------- |
| Campo/regra | ...       | ... | ... | ...    | ... | Consistente/Conflito |

Não criar tabela enorme quando texto simples for suficiente.

---

# 52. Resultado esperado da Microetapa 0.1

A análise inicial deverá permitir responder:

* quem entra na lista?
* quais dados são necessários?
* como a entrada é registrada?
* existe ordem ou prioridade?
* quais status estão documentados?
* como ocorre saída da fila?
* quais tabelas participam?
* quais relacionamentos existem?
* quais campos são obrigatórios?
* quais operações administrativas são necessárias?
* quais informações precisam aparecer na interface?
* existe conflito documental?
* PostgreSQL é compatível com o modelo físico?

Quando a documentação não responder alguma pergunta, marcar como:

> não definido nos documentos fornecidos.

---

# 53. Regra máxima deste guia

Durante a execução:

> Fazer somente aquilo que é necessário para concluir corretamente a microetapa atual.

Quando o objetivo estiver atendido:

# PARE.
