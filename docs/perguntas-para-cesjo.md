# Perguntas para a equipe do Centro-Dia

Novembro de 2026.

## Para que serve este documento

Estamos construindo o sistema que vai organizar o dia a dia do Centro-Dia: lista de espera, usuários
atendidos, PIA, atividades, contatos, gestão da equipe, doações, parcerias e relatórios.

Em vários pontos, o documento de requisitos não diz exatamente como o Centro-Dia trabalha. Nesses
pontos, nós escolhemos um jeito provisório para o sistema não ficar parado. Agora precisamos que
vocês confirmem ou corrijam essas escolhas.

**Como responder**

- As perguntas estão separadas por setor. Cada setor responde só a sua parte.
- Cada pergunta mostra **como o sistema funciona hoje**. Se estiver bom, basta escrever "ok".
- Se não souberem responder alguma pergunta, escrevam "não sei" ou indiquem quem sabe. Isso também
  ajuda.
- Exemplos, fichas em papel e planilhas antigas ajudam muito. Se mandarem planilhas, troquem os nomes
  e dados reais por dados inventados.
- O código entre colchetes, como [P1], é só para a nossa equipe se achar. Podem ignorar.

**Quem é quem no sistema**

O sistema tem quatro tipos de acesso. Eles aparecem em várias perguntas:

- **Coordenação**: vê e faz tudo.
- **Equipe técnica**: assistente social, psicólogo(a), terapeuta ocupacional e outros profissionais
  do atendimento.
- **Administrativo**: secretaria, recepção, cadastros e agenda.
- **Consulta**: só olha informações básicas, sem alterar nada (por exemplo, alguém da Secretaria que
  acompanha os números).

---

## 1. Direção do Centro-Dia

### 1.1 Quem é o Centro-Dia [P12]

1. O Centro-Dia é um serviço da própria Prefeitura de São Luís, ligado à Semcas? Ou é uma entidade
   (associação, ONG) que tem convênio com a Prefeitura?
2. Se for entidade conveniada: o convênio ou termo de colaboração fala alguma coisa sobre troca de
   informações das pessoas atendidas com a Prefeitura?

> Por que perguntamos: isso decide se o sistema pode receber informações do CadÚnico e do INSS e de
> que forma. Um serviço da Prefeitura tem caminhos que uma entidade não tem.

### 1.2 Proteção dos dados das pessoas atendidas [P14]

O sistema guarda informações delicadas: deficiência, laudos, saúde, religião, renda. A lei de
proteção de dados (LGPD) pede algumas definições.

1. Com base em que o Centro-Dia guarda esses dados? A família assina um termo de autorização, ou os
   dados são guardados porque o Centro-Dia executa uma política pública?
2. Quem é a pessoa responsável pelos dados pessoais (o "encarregado de dados") da Semcas ou do
   Centro-Dia? Ela tem um e-mail ou telefone público?
3. Depois que a pessoa deixa o Centro-Dia, por quanto tempo os dados dela devem ficar guardados?

Como o sistema funciona hoje:

- O registro de "quem fez o quê" no sistema é guardado para sempre.
- As cópias de segurança são guardadas por cerca de seis meses.
- Quando um laudo é trocado por outro mais novo, o antigo continua guardado, mas não aparece mais na
  ficha da pessoa.

### 1.3 Onde o sistema vai ficar [P15]

1. Quem vai pagar o servidor onde o sistema fica (um computador alugado na internet, com custo
   mensal)? A Prefeitura, o Centro-Dia ou outra pessoa?
2. Já existe um endereço na internet para o sistema (por exemplo, `centrodia.saoluis.ma.gov.br`)?
3. Quem guarda as senhas principais do sistema? Hoje a proposta é a coordenação guardar duas cópias:
   uma num cofre de senhas e outra num envelope lacrado.

### 1.4 Aparência [P16]

1. A Prefeitura ou o Centro-Dia tem uma fonte de letra oficial, logotipo ou cores que o sistema deve
   usar?

Como o sistema funciona hoje: usa uma fonte gratuita feita para facilitar a leitura por pessoas com
baixa visão.

### 1.5 Tamanho do Centro-Dia [P40]

1. Quantas pessoas o Centro-Dia atende ao mesmo tempo (quantas vagas)?
2. Quantas pessoas entram na lista de espera por ano, mais ou menos?
3. Quantas atividades, atendimentos e contatos a equipe registra por mês, mais ou menos?
4. Quantas pessoas da equipe vão usar o sistema ao mesmo tempo nos horários mais cheios?

Como o sistema funciona hoje: foi testado pensando em cerca de 60 vagas e 600 pessoas na lista de
espera em cinco anos, e aguenta dez vezes isso.

---

## 2. Coordenação

### 2.1 Ordem da lista de espera [P1]

O documento diz que o serviço dá prioridade a quem recebe BPC e está no CadÚnico, mas também diz que
a fila segue a ordem de chegada.

1. Como a prioridade funciona na prática? Escolham uma:
   - quem recebe BPC e está no CadÚnico passa na frente de todos;
   - serve só para desempatar quem chegou no mesmo dia;
   - é só uma informação, a fila segue a ordem de chegada.

Como o sistema funciona hoje: a fila é só por ordem de chegada (data do cadastro).

### 2.2 Idade [P2]

1. O cadastro na lista de espera deve exigir a data de nascimento?
2. O sistema deve recusar quem tem menos de 18 ou mais de 59 anos?

Como o sistema funciona hoje: a data de nascimento é opcional e não há bloqueio por idade.

### 2.3 Vagas [P3]

1. As vagas são um número total, ou são separadas por turno (manhã, tarde, dia inteiro)? Se forem
   separadas, quantas há em cada turno?
2. Quando abre uma vaga, quem deve ser avisado? O aviso deve ser só no sistema, ou também por
   e-mail ou WhatsApp?

Como o sistema funciona hoje: as vagas são um número total. Quando abre vaga, a coordenação e o
administrativo recebem um aviso dentro do sistema, com o nome do próximo da fila.

### 2.4 Saída da lista de espera [P4]

1. Por quais motivos uma pessoa sai da lista de espera?
2. Se a pessoa é chamada e não comparece, ela volta para a fila? Na mesma posição ou no fim?
3. Quem saiu da lista pode voltar depois? Na posição antiga ou no fim?

Como o sistema funciona hoje:

- Os motivos são: desistência, não localizado, fora do perfil e outro (com explicação obrigatória).
- Quem saiu pode voltar na posição antiga, com uma explicação obrigatória.
- Quem tem visita agendada continua na fila, na mesma posição.

### 2.5 Cuidador da família [P7]

1. O cuidador responsável pela pessoa atendida é sempre o mesmo responsável informado na lista de
   espera?
2. Uma pessoa atendida pode ter mais de um cuidador responsável?

Como o sistema funciona hoje: um cuidador por pessoa atendida. O responsável da lista de espera já vem
escolhido, mas pode ser trocado.

### 2.6 Planilhas que vocês já usam [P13]

1. A lista de espera e o cadastro das pessoas atendidas estão hoje em planilhas do Excel?
2. Podem mandar um exemplo dessas planilhas com dados inventados (nomes e documentos falsos)?

Como o sistema funciona hoje: aceita uma planilha num modelo nosso, que pode ser baixado na tela. Se a
planilha de vocês for diferente, adaptamos o sistema a ela.

### 2.7 Quem vê o quê [P8 e P17]

Para cada linha, digam **sim** ou **não**.

| Pergunta | Como está hoje |
|---|---|
| O administrativo pode ver laudos médicos e comprovantes? | sim |
| O administrativo pode ver a religião e as informações de saúde da pessoa atendida? | não |
| O administrativo pode ver as atividades e atendimentos? | não |
| O administrativo pode registrar o que aconteceu numa visita e ler as anotações da visita? | não (só agenda, cancela e remarca) |
| O administrativo pode registrar parcerias, reuniões, eventos e doações? | não |
| O administrativo pode ver relatórios? | não |
| O administrativo pode enviar planilhas para o sistema? | não |
| A equipe técnica pode ver parcerias, reuniões, eventos e doações? | não |
| A equipe técnica pode ver relatórios? | não |
| A equipe técnica pode cadastrar pessoas na lista de espera? | não (só a coordenação e o administrativo) |
| Só a coordenação pode colocar uma pessoa da lista de espera no serviço? | sim |
| Só a coordenação pode registrar a saída de uma pessoa atendida? | sim |
| Só a coordenação pode ver o cadastro dos funcionários? | sim |

Sobre o acesso de **Consulta** (só olhar):

1. O que essa pessoa pode ver? Hoje ela vê só nome, situação, turno e datas. Não vê tipo de
   deficiência, documentos nem dados de saúde.
2. Ela pode baixar a lista de espera em Excel ou PDF? Hoje, não.
3. Ela pode ver nos relatórios quantas violações de direitos aconteceram? Hoje, não. Só a coordenação
   vê. [P33]

---

## 3. Equipe técnica

### 3.1 PIA – Plano Individual de Atendimento [P5]

1. Como é o PIA de vocês? Podem mandar um modelo em branco, mesmo que seja em papel (foto serve)?
2. O PIA é revisado de tempos em tempos? Cada revisão vira uma versão nova, guardando a anterior?
3. A ordem é sempre: conversa com a família, depois equipe, depois aprovação? Ou muda?

Como o sistema funciona hoje: o PIA tem diagnóstico, objetivos (cada um com metas, ações, prazo e
responsável) e observações. Cada passo (família, equipe, aprovação) tem uma data. Depois de
apresentado, o PIA só muda com uma versão nova.

### 3.2 Cadastro completo da pessoa atendida [P6 e P21]

1. Quais informações vocês precisam ter de cada pessoa atendida?

   Hoje o cadastro tem: gênero, CPF, NIS, RG, escolaridade, endereço com bairro, renda da família, se
   recebe BPC, se está no CadÚnico, se recebe Bolsa Família, religião, remédios de uso contínuo e
   outras informações de saúde. Tudo é opcional. Falta alguma coisa? Sobra alguma?
2. Quais opções de gênero o cadastro deve ter? Hoje: feminino, masculino e outro.
3. Quais níveis de escolaridade vocês usam nos relatórios? Hoje: sem escolaridade, fundamental, médio
   e superior, cada um incompleto ou completo.

### 3.3 Violações de direitos [P19]

1. Ver só a lista de violações (o tipo e a data, sem abrir o texto do que aconteceu) já deve ficar
   registrado como acesso a informação delicada? Ou basta registrar quem abre o texto?

Como o sistema funciona hoje: só fica registrado quem abre o texto da violação.

### 3.4 Atividades [P23, P24 e P25]

1. Nas atividades com famílias, basta registrar de qual pessoa atendida é a família presente? Ou é
   preciso registrar cada familiar (nome e parentesco)?
2. Vocês querem agendar atividades futuras no sistema e depois marcar se aconteceram? Ou registram só
   o que já aconteceu?
3. O atendimento multiprofissional precisa ter sempre dois ou mais profissionais? Quais profissões
   contam?

Como o sistema funciona hoje:

- Na atividade com famílias, registra-se só de qual pessoa atendida é a família.
- Só se registram atividades que já aconteceram. Não há agenda de atividades.
- O atendimento multiprofissional exige pelo menos dois profissionais, de qualquer profissão.

### 3.5 Contatos, ofícios e encaminhamentos [P30 e P31]

1. Quando a equipe registra um contato (por exemplo, a mãe de um usuário ligou), basta escrever o
   nome da pessoa? Ou o sistema deve ligar o contato à ficha da pessoa atendida ou da instituição
   parceira?
2. Vocês querem registrar contatos agendados, ou só os que já aconteceram?
3. Os ofícios e declarações têm um número de protocolo que precisa de um campo próprio?
4. Um registro pode ter mais de um anexo (por exemplo, o ofício e a resposta)?
5. Em algum tipo de registro o anexo é obrigatório?

Como o sistema funciona hoje: o nome da pessoa ou instituição é escrito livremente. Só se registra o
que já aconteceu. Cada registro tem no máximo um anexo, sempre opcional. O número do ofício vai no
assunto.

---

## 4. Administrativo e recursos humanos

### 4.1 Funções da equipe [P18]

1. Quais funções os funcionários do Centro-Dia têm? Além de assistente social, psicólogo(a) e
   terapeuta ocupacional, há outras que precisam aparecer separadas nos relatórios (cuidador,
   monitor, motorista, coordenação, estagiário)?

Como o sistema funciona hoje: assistente social, psicólogo(a), terapeuta ocupacional e outra.

### 4.2 Saída de funcionários [P26]

1. Quais motivos de saída de funcionário vocês usam (pedido de demissão, fim de contrato, dispensa,
   aposentadoria)? Precisam aparecer separados em algum relatório?

Como o sistema funciona hoje: o motivo é escrito livremente.

### 4.3 Parcerias [P10]

1. O que precisa ser registrado sobre cada parceria?

Como o sistema funciona hoje: nome do parceiro, tipo de instituição (órgão público, ONG, empresa,
escola ou faculdade, outro), documento da parceria, data de início e de fim, contato, observações e
se está ativa. Cada ação feita com o parceiro tem data e descrição.

### 4.4 Doações [P11]

1. O Centro-Dia recebe doações em dinheiro?
2. É preciso emitir recibo ou prestar contas dos valores?
3. Quais tipos de doação vocês recebem?

Como o sistema funciona hoje: a doação em dinheiro pode ter o valor em reais. As outras têm só a
quantidade, escrita livremente. Não há recibo nem prestação de contas. Os tipos são: dinheiro,
alimentos, roupas e calçados, higiene e limpeza, material pedagógico, móveis e equipamentos, outro.

---

## 5. Relatórios

Para quem faz e envia os relatórios do Centro-Dia.

### 5.1 Modelos [P9]

1. Podem mandar os modelos atuais dos relatórios mensal, quadrimestral e anual?
2. O que vai no relatório de intervenção para o CREAS? Para quem é enviado?
3. O que vai no relatório de transparência? Para quem é enviado, ou onde é publicado?

Como o sistema funciona hoje:

- **Mensal, quadrimestral e anual**: os mesmos números (lista de espera, entradas e saídas, pessoas
  atendidas, atividades, contatos e desligamentos). Os quadrimestres são janeiro a abril, maio a
  agosto e setembro a dezembro.
- **Intervenção (CREAS)**: um caso por pessoa com violação de direitos no período, com os
  atendimentos feitos. Não mostra o texto da violação. Só a coordenação gera.
- **Transparência**: só números, sem nenhum nome ou dado pessoal. O sistema gera o arquivo, e a
  publicação é feita fora dele.

### 5.2 Taxa de desligamento [P32]

1. Como vocês calculam a taxa de desligamento? Quantos saíram no período, dividido por quê? Escolham
   uma:
   - todas as pessoas atendidas em algum dia do período;
   - as pessoas atendidas no primeiro dia do período;
   - as pessoas atendidas no último dia do período.

Como o sistema funciona hoje: usa a primeira opção.

---

## 6. CadÚnico e BPC

Para o setor do CadÚnico da Semcas, ou para quem no Centro-Dia confere CadÚnico e BPC. [P12]

O sistema já guarda se a pessoa está no CadÚnico e se recebe BPC, mas só pelo que a família informa.
Queremos saber se dá para conferir essas informações de forma oficial.

1. Hoje, como a equipe confere se a pessoa está no CadÚnico e recebe BPC? Pelo comprovante que a
   família traz, pelo CRAS, pelo setor do CadÚnico ou de outro jeito?
2. O setor do CadÚnico da Semcas passa hoje alguma lista ou relatório para o Centro-Dia? Em que
   formato (planilha, PDF, papel)?
3. Se o Centro-Dia pedisse uma lista das pessoas atendidas que estão no CadÚnico, o setor poderia
   fornecer? Que documento seria preciso (ofício, termo, portaria)?
4. Quem é a pessoa de contato no setor do CadÚnico?
5. Há alguma troca de informações com a Secretaria de Saúde ou de Educação além de ofícios e
   encaminhamentos? Quem é o contato nelas?

> Nossa sugestão: no começo, a equipe confere o comprovante que a família traz e registra no sistema
> a data da conferência, com o comprovante anexado. Isso não depende de nenhum acordo. Depois, se o
> setor do CadÚnico puder mandar uma lista, o sistema passa a conferir a partir dela.

---

## 7. Responsável de TI

Para a pessoa que cuida dos computadores e da internet do Centro-Dia. Se alguma pergunta for de outro
setor (por exemplo, da TI da Prefeitura), indiquem o contato.

### 7.1 Equipamentos e internet

1. Quantos computadores a equipe vai usar no sistema? Qual o sistema operacional (Windows, Linux)?
2. Quais navegadores estão instalados (Chrome, Edge, Firefox)? Estão atualizados?
3. A equipe vai usar o sistema pelo celular? Celulares próprios ou do Centro-Dia?
4. Como é a internet do Centro-Dia? Ela cai com frequência?
5. A rede bloqueia algum site ou tem filtro de conteúdo? Quem administra esse filtro?
6. Há impressora em rede? O sistema gera relatórios em PDF para imprimir.

### 7.2 Servidor e endereço na internet

1. A Prefeitura tem um setor de TI que hospeda sistemas, ou o sistema deve ficar num servidor
   alugado?
2. Quem pode criar um endereço na internet para o sistema (por exemplo, no domínio da Prefeitura)?
3. Existe e-mail institucional que o sistema possa usar para enviar avisos?
4. Há alguma regra da Prefeitura para sistemas novos (aprovação, segurança, contrato)?

### 7.3 Cópias de segurança

1. O sistema faz uma cópia de segurança todo dia. Existe um lugar da Prefeitura ou do Centro-Dia para
   guardar essas cópias fora do servidor (um armazenamento na nuvem, outro servidor)?
2. Quem deve receber o aviso se o sistema sair do ar?

### 7.4 Conecta gov.br

1. A Prefeitura de São Luís usa o Conecta gov.br (a plataforma do governo federal para troca de dados
   entre órgãos públicos)? Se usa, quem é o responsável?

---

## Obrigado

Mandem as respostas como preferirem: neste documento, por e-mail ou numa conversa. Se for mais fácil,
podemos fazer uma reunião curta com cada setor.
