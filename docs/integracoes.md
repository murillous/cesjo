# Integrações com sistemas externos

Data: 2026-11-23. Issue #63, item 54 do [planejamento](planejamento.md). Decisão: [ADR 0225](adr/0225-nenhuma-integracao-antes-da-p12.md).

A seção 5.1 do [documento de requisitos](requisitos/documentoRequisitosCentroDia.pdf) lista três
integrações e não diz mais nada sobre elas:

- CadÚnico (consulta de dados);
- Sistema de gestão do BPC;
- Sistemas municipais de saúde e educação.

O documento não diz o município do Centro-Dia nem a natureza jurídica da CESJO (órgão público ou
organização da sociedade civil). As duas coisas mudam o que é possível.

- **Município: São Luís (MA).** Informado pela equipe do projeto em 2026-11-23
  ([ADR 0237](adr/0237-centro-dia-em-sao-luis.md)).
- **Natureza jurídica: provavelmente órgão público municipal, a confirmar.** Notícias de 2017 descrevem
  o Centro-Dia de São Luís como serviço da Prefeitura, administrado pela Secretaria Municipal da
  Criança e Assistência Social (Semcas), no Centro Educacional São José Operário, na Cidade Operária,
  para pessoas com deficiência de 18 a 59 anos
  ([O Imparcial, 2017](https://oimparcial.com.br/noticias/2017/03/centro-dia-completa-tres-anos-de-funcionamento/),
  [O Imparcial, 2017](https://oimparcial.com.br/noticias/2017/08/prefeitura-expande-servicos-para-criancas-com-microcefalia/)).
  A sigla CESJO bate com o nome do centro educacional. Não foi encontrada fonte oficial atual. Este
  levantamento continua tratando os dois casos, mas o de órgão público passa a ser o mais provável.

Este documento é só um levantamento. Nenhuma integração foi implementada. A pergunta P12 continua
aberta em [PENDENTE-CESJO.md](PENDENTE-CESJO.md#p12-integrações-seção-51).

## Resumo

| Sistema | Meio de acesso público | Acessível para a CESJO hoje? | Recomendação |
|---|---|---|---|
| CadÚnico | API no Conecta gov.br (só órgãos federais e estaduais); cessão de arquivo pelo órgão gestor, com Termo de Uso; comprovante emitido pela própria família | Só a consulta manual (comprovante trazido pela família). O resto depende de termo com a gestão municipal | Consulta manual com registro do comprovante; arquivo só se a P12 trouxer termo de cessão |
| BPC (INSS/Dataprev) | API no Conecta gov.br (só órgãos públicos federais); extrato ou declaração emitidos pelo beneficiário no Meu INSS | Só a consulta manual | Consulta manual com registro do comprovante |
| Saúde municipal | Nenhum aberto. A RNDS é para estabelecimentos de saúde com CNES | Não | Nenhuma integração; encaminhamentos e contrarreferência registrados no sistema, como hoje |
| Educação municipal | Nenhum aberto. Dados identificados do Censo Escolar só em sala segura do Inep, para pesquisa | Não | Nenhuma integração; escolaridade continua declarada no cadastro |

O sistema já guarda, por declaração, `nis`, `inscrito_cad_unico` e `recebe_bpc` no cadastro completo
(`V40__cria_cadastro_complementar.sql`, [ADR 0140](adr/0140-campos-do-cadastro-completo.md)). A fila
não usa BPC nem CadÚnico para prioridade ([ADR 0057](adr/0057-ordem-e-posicao-na-fila.md), P1).

## Requisitos legais comuns

Valem para qualquer integração que vier depois da P12.

- **LGPD** ([Lei 13.709/2018](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm)):
  - dado de saúde e de deficiência é dado pessoal sensível (art. 5º, II). Sem consentimento, só pode
    ser tratado nas hipóteses do art. 11, II, como execução de política pública pela administração
    (alínea b) ou cumprimento de obrigação legal (alínea a);
  - o poder público só transfere dados a entidade privada nas hipóteses do art. 26, §1º: execução
    descentralizada de atividade pública, previsão legal, contrato ou convênio, ou prevenção de fraude.
    Fora delas, o art. 27 exige consentimento do titular. Para uma OSC, o caminho normal é o termo de
    colaboração ou convênio com o município;
  - é preciso manter registro das operações de tratamento (art. 37), indicar encarregado com contato
    público (art. 41) e adotar medidas de segurança (art. 46). Isso depende da resposta da P14 (base
    legal e encarregado).
- **Decreto 11.016/2022** ([texto](https://planalto.gov.br/ccivil_03/_Ato2019-2022/2022/Decreto/D11016.htm)):
  os dados de identificação do CadÚnico são sigilosos (art. 13). Só podem ser usados para gestão de
  políticas públicas e para estudos e pesquisas, no mínimo necessário (art. 13, §2º). O órgão gestor
  municipal pode usar a base do município e cedê-la a terceiros nessas finalidades (art. 13, §§3º e 4º).
- **Portaria MC 810/2022** ([página no MDS](https://www.gov.br/mds/pt-br/acesso-a-informacao/legislacao/portaria/portaria-mc-no-810-de-14-de-setembro-de-2022),
  texto consolidado de dezembro de 2025), alterada pela Portaria MDS 860/2023 e pela Portaria MDS
  1.133/2025. Regras de cessão dos dados do CadÚnico. Pontos relevantes do texto original (a numeração
  deve ser conferida no texto consolidado):
  - o gestor municipal só cede dados da própria esfera;
  - a cessão depende de solicitação formal, Termo de Uso, Termo de Responsabilidade do representante
    legal e Termo de Compromisso de Manutenção de Sigilo de cada técnico, e só vale após o extrato
    publicado no Diário Oficial;
  - instituições executoras de programas sociais, públicas ou privadas, podem receber dados com
    autorização formal do órgão gestor, restritos à execução do programa e ao mínimo necessário;
  - repassar os dados a terceiros sem autorização é uso indevido, sujeito a sanção.
- **Portaria MDS 502/2017**: política de controle de acesso aos dados do CadÚnico
  ([lista de normas do MDS](https://www.gov.br/mds/pt-br/acesso-a-informacao/carta-de-servicos/avaliacao-e-gestao-da-informacao-e-cadastro-unico/cadastro-unico/legislacao-acesso-dados)).
- **Decreto 10.046/2019** ([texto](https://planalto.gov.br/ccivil_03/_ato2019-2022/2019/Decreto/D10046.htm)):
  governa o compartilhamento de dados na administração pública federal, base do Conecta gov.br. Não se
  aplica ao compartilhamento com o setor privado (art. 1º, §1º).

## CadÚnico

**Meios de acesso que existem:**

1. **APIs do Conecta gov.br**, mantidas pelo MDS com a Dataprev. Recebem CPF (ou NIS) e respondem:
   - [Consulta Situação Cadastral](https://www.gov.br/conecta/catalogo/apis/cadunico-servicos):
     inscrito, baixa renda e cadastro atualizado em 24 meses (sim ou não);
   - [Indicadores Familiares](https://www.gov.br/conecta/catalogo/apis/cadunico-servicos-indicadores-familiares)
     e [Dados Familiares](https://www.gov.br/conecta/catalogo/apis/cadunico-servicos-dados-familiares):
     situação, faixa de renda, município, Bolsa Família e tamanho da família.

   Só órgãos públicos aderentes ao Conecta consomem essas APIs. A página
   [Conecta para Estados e Municípios](https://www.gov.br/governodigital/pt-br/infraestrutura-nacional-de-dados/interoperabilidade/conecta-gov.br/conecta-para-estados-e-municipios)
   lista as APIs do CadÚnico para estados, mas não para municípios. Municípios só aderem se forem
   capitais ou tiverem mais de 500 mil habitantes. Não há acesso para entidade privada.

   São Luís é capital, então a Prefeitura pode aderir ao Conecta. Não foi encontrado registro de que
   já tenha aderido. Mesmo com adesão, as APIs do CadÚnico não aparecem na lista para municípios; isso
   precisa ser confirmado com a equipe do Conecta (conecta@gestao.gov.br).
2. **Cessão de dados identificados** pelo órgão gestor do CadÚnico:
   - em nível federal, pelo [SolicitaCad](https://www.gov.br/pt-br/servicos/solicitar-cessao-de-dados-identificados-do-cadastro-unico),
     só para órgãos públicos (gestão de políticas) e órgãos de pesquisa. Pedidos sobre um município vão
     ao gestor daquele município. A entrega é um link de download, com validade de 7 dias, num ambiente
     seguro. Não é API;
   - em nível municipal, pelo órgão gestor do CadÚnico do município, com os termos da Portaria MC
     810/2022. O formato (planilha, extração do CECAD ou outro) é o que o município fornecer.

   Em São Luís, a Semcas faz as inscrições e atualizações do CadÚnico
   ([O Imparcial, 2020](https://oimparcial.com.br/economia/2020/05/inscricoes-e-atualizacoes-no-cadastro-unico-podem-ser-feitas-por-telefone-em-sao-luis/),
   [O Imparcial, 2021](https://oimparcial.com.br/cidades/2021/11/prazo-para-atualizacao-do-cadunico-e-prorrogado-ate-30-de-novembro/)).
   Se o Centro-Dia também for da Semcas, gestor do CadÚnico e serviço estão na mesma secretaria. O
   uso da base do município para gestão de política pública é permitido ao próprio gestor (Decreto
   11.016/2022, art. 13, §3º), então não seria cessão a terceiro. Ainda assim vale formalizar o uso
   num ato interno, com o mínimo necessário e com o registro da operação (LGPD, art. 37).
3. **Consulta manual pela família**: o [comprovante do Cadastro Único](https://www.gov.br/pt-br/servicos/emitir-comprovante-do-cadastro-unico)
   sai na web, no aplicativo Cadastro Único ou no CRAS. A folha resumo sai no órgão gestor municipal.
4. **Dados agregados**: CECAD e Observatório do Cadastro Único dão números do município, sem
   identificação. Não servem para verificar uma pessoa.

**Requisitos legais:** Decreto 11.016/2022, art. 13; Portaria MC 810/2022 e alterações (Termo de Uso,
Termo de Responsabilidade, Termos de Sigilo, publicação do extrato); Portaria MDS 502/2017; LGPD.

**Contatos públicos:** MDS, uso para políticas públicas, info.cadastro@mds.gov.br; estudos e
pesquisas, dados.sagi@mds.gov.br (fonte: página do SolicitaCad). Gestão municipal do CadÚnico e
contato técnico: a confirmar com a equipe CESJO, P12.

## BPC (INSS e Dataprev)

**Meios de acesso que existem:**

1. **APIs do Conecta gov.br**:
   - [Benefícios Previdenciários](https://www.gov.br/conecta/catalogo/apis/api-beneficios-previdenciarios):
     benefícios por CPF, com consulta de benefício ativo do tipo LOAS (espécie 87, o BPC da pessoa com
     deficiência). Gestor: Diretoria de Benefícios do INSS. Operação: Dataprev;
   - [Pessoa com Deficiência](https://www.gov.br/conecta/catalogo/apis/pessoa-com-deficiencia):
     responde se o CPF está na base de beneficiários do BPC ou da aposentadoria da LC 142/2013.
     OAuth2 com JWT. Tem [dados fictícios de teste na Dataprev](https://docs.dataprev.gov.br/docs/api/api-pessoa-com-deficiencia/dados-ficticios-api-de-teste/).

   As duas páginas indicam adesão ao Conecta para órgãos públicos federais, com cadastro dos IPs no
   firewall do Serpro. Não há acesso para municípios na lista do Conecta nem para entidade privada.
2. **Consulta manual pelo beneficiário** no Meu INSS, com conta gov.br:
   [Extrato de Informações do Benefício](https://www.gov.br/pt-br/servicos/emitir-extrato-de-informacoes-do-beneficio)
   e Declaração de Beneficiário (consta/nada consta).

**Requisitos legais:** LGPD (art. 26 e 27 para transferência a entidade privada); Decreto 10.046/2019
para o Conecta; Resolução CCGD nº 10/2022 (Registro de Referência da Pessoa com Deficiência), citada
na página da API Pessoa com Deficiência.

**Contatos públicos:** INSS, Diretoria de Benefícios, dirben@inss.gov.br, (61) 3313-4949; suporte
técnico da Dataprev pelo Sistema Pronto Atendimento; MDHC, cgdipd@mdh.gov.br; Conecta,
conecta@gestao.gov.br. Contato técnico para a CESJO: a confirmar com a equipe CESJO, P12.

## Sistemas municipais de saúde

**Meio de acesso que existe:** nenhum aberto. Não foi encontrado qual prontuário eletrônico a
Secretaria Municipal de Saúde de São Luís (Semus) usa, nem API publicada. Um estudo sobre as unidades
básicas de São Luís relata conexão precária e prontuário eletrônico em poucas unidades
([preprint SciELO](https://preprints.scielo.org/index.php/scielo/preprint/download/12356/22684/23361));
o dado é antigo e não descreve a rede atual.

Em nível federal, a [RNDS](https://www.gov.br/saude/pt-br/composicao/seidigi/rnds) é a plataforma de
interoperabilidade do SUS, regulamentada pelo
[Decreto 12.560/2025](https://legis.senado.leg.br/norma/41155564/publicacao/41160178). Ela identifica
o estabelecimento pelo CNES e serve a estabelecimentos e serviços de saúde. O Centro-Dia é serviço do
SUAS, não de saúde, e não tem esse papel.

**Requisitos legais:** LGPD, art. 11, II, f (tutela da saúde só por profissionais, serviços de saúde
ou autoridade sanitária); Lei 8.080/1990, arts. 47 e 47-A; acordo formal com a Secretaria de Saúde.

**Contato técnico:** a confirmar com a equipe CESJO, P12.

## Sistemas municipais de educação

**Meio de acesso que existe:** nenhum aberto. Não foi encontrado sistema de gestão escolar do
município com API pública. Em nível federal, dados identificados do Censo Escolar só são consultados
por pesquisadores no [Serviço de Acesso a Dados Protegidos do Inep](https://www.gov.br/pt-br/servicos/usar-o-servico-de-acesso-a-dados-protegidos-do-inep),
em sala segura. A faixa etária do Centro-Dia (18 a 59 anos) também torna a integração com a rede
escolar pouco útil: o que o sistema precisa é a escolaridade, que já é declarada no cadastro.

**Requisitos legais:** LGPD; acordo formal com a Secretaria de Educação.

**Contato técnico:** a confirmar com a equipe CESJO, P12.

## Abordagem recomendada

1. **Consulta manual registrada.** A equipe vê o comprovante do CadÚnico e o extrato do BPC que a
   família traz, ou consulta o CRAS, e registra no sistema a data da verificação e o comprovante
   anexado. Não precisa de convênio, não traz dado de terceiro e funciona para OSC.
2. **Arquivo cedido pela gestão municipal**, só se a P12 confirmar um termo de cessão (Portaria MC
   810/2022) ou um termo de colaboração que preveja o compartilhamento. Importação de planilha, com
   o mesmo mecanismo da importação de planilhas (item 53, P13). Se o Centro-Dia for da Semcas, o
   arquivo vem da própria secretaria e o instrumento é um ato interno, não um termo de cessão.
3. **API**, só se a CESJO for órgão público aderente ao Conecta ou se um órgão público consumir a API e
   repassar o resultado por instrumento formal. Hoje nenhuma das APIs atende uma OSC. São Luís, por ser capital, pode aderir ao
   Conecta, mas as APIs do CadÚnico e do BPC não estão na lista para municípios.
4. **Saúde e educação**: sem integração. Encaminhamentos, ofícios e contrarreferência continuam nos
   módulos de atividades e comunicações.

## Perguntas para a P12

- O Centro-Dia é serviço da Prefeitura de São Luís, ligado à Semcas, ou é uma OSC conveniada?
- O setor do CadÚnico da Semcas fornece hoje alguma lista à equipe do Centro-Dia?
- A Prefeitura de São Luís tem adesão ao Conecta gov.br?
- Existe termo de colaboração ou convênio com o município? Ele prevê compartilhamento de dados?
- A gestão municipal do CadÚnico cede hoje alguma lista ou relatório à CESJO? Em que formato?
- A equipe confere CadÚnico e BPC pelo comprovante da família, pelo CRAS ou por outro meio?
- Há algum fluxo com a Secretaria de Saúde ou de Educação além de encaminhamento por ofício?
- Quem é o contato técnico em cada órgão?

## Proposta de issues de implementação

Não foram criadas no GitHub. Todas levam o rótulo `aguardando-cesjo` (P12) e o marco M7.

### 1. Registrar a verificação de CadÚnico e BPC

Depende da P12 (meio de verificação) e da P1 (uso na fila).

- [ ] O cadastro completo guarda, para CadÚnico e para BPC, a data da última verificação, o meio
      (comprovante da família, CRAS, arquivo da gestão municipal) e um anexo opcional, cifrado como
      os outros documentos (ADR 0004).
- [ ] A ficha mostra "verificado em" e destaca verificações com mais de 24 meses, o prazo de
      atualização do CadÚnico.
- [ ] A auditoria registra a verificação. Só Coordenador e Equipe Técnica registram.
- [ ] Testes de integração com dados fictícios.

### 2. Importar a lista de famílias cedida pela gestão municipal do CadÚnico

Só se a P12 confirmar termo de cessão publicado (Portaria MC 810/2022) ou instrumento equivalente.

- [ ] O Coordenador envia o arquivo no formato que a gestão municipal fornecer. O sistema cruza por
      NIS ou CPF só com as pessoas já cadastradas e descarta o resto, sem guardar o arquivo.
- [ ] O resultado atualiza `inscrito_cad_unico` e a data de verificação da issue 1, com relatório de
      quem mudou e quem não foi encontrado.
- [ ] A auditoria registra a importação, sem os dados do arquivo.
- [ ] ADR com a base legal e o instrumento de cessão. Registro da operação de tratamento (LGPD, art. 37).

### 3. Consultar CadÚnico e BPC por API

Só se a CESJO for órgão público aderente ao Conecta, ou se um órgão público assumir a consulta por
instrumento formal.

- [ ] Cliente das APIs Consulta Situação Cadastral (CadÚnico) e Benefícios Previdenciários ou Pessoa
      com Deficiência (BPC), com OAuth2 e credenciais fora do repositório.
- [ ] A consulta é feita por um botão na ficha, uma pessoa por vez, e guarda só o resultado (sim ou
      não) e a data, como na issue 1.
- [ ] Falha ou indisponibilidade da API não bloqueia o cadastro.
- [ ] Testes com servidor simulado e os dados fictícios da Dataprev.

### 4. Registrar a referência na rede de saúde e de educação

Só se a P12 mostrar que a equipe precisa disso. Não é integração.

- [ ] O cadastro completo guarda, opcionalmente, a unidade de saúde e a escola ou serviço de educação
      de referência, em texto ou lista configurável.
- [ ] Os encaminhamentos e ofícios podem citar essa referência.
- [ ] Auditoria e testes de integração.
