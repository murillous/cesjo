# ADR 0225 — Nenhuma integração externa antes da resposta da P12

**Status:** Aceito (2026-11-23) · Issue #63

## Contexto

A seção 5.1 do documento de requisitos lista integrações com o CadÚnico, com o sistema de gestão do
BPC e com sistemas municipais de saúde e educação, sem dizer o que se quer de cada uma. A pergunta
P12 (acesso formal e contato técnico) está sem resposta. O documento também não diz o município do
Centro-Dia nem se a CESJO é órgão público ou organização da sociedade civil.

O levantamento em [`docs/integracoes.md`](../integracoes.md) mostra que:

- as APIs do CadÚnico e do BPC no Conecta gov.br são só para órgãos públicos aderentes (federais e,
  no caso do CadÚnico, estaduais). Municípios não têm essas APIs na lista do Conecta, e não há acesso
  para entidade privada;
- a cessão de dados identificados do CadÚnico exige Termo de Uso, Termo de Responsabilidade, Termos
  de Sigilo e extrato publicado (Portaria MC 810/2022), e o repasse sem autorização é uso indevido;
- a LGPD só permite que o poder público transfira dados a entidade privada nas hipóteses do art. 26,
  §1º (por exemplo, convênio ou contrato);
- saúde e educação municipais não têm meio de acesso público.

Alternativas consideradas:

- **Implementar já um cliente das APIs do Conecta.** A CESJO provavelmente não tem como obter
  credenciais, e o código ficaria sem uso e sem teste real.
- **Implementar já a importação de um arquivo do CadÚnico.** O formato e a base legal dependem de um
  termo com a gestão municipal que talvez não exista.
- **Não integrar até a P12 e recomendar a consulta manual registrada.** Não depende de convênio, não
  traz dado de terceiro para o sistema e funciona para órgão público ou OSC.

## Decisão

- Nenhuma integração com sistema externo é implementada antes da resposta da P12.
- A abordagem recomendada, em ordem: (1) consulta manual registrada, com o comprovante trazido pela
  família ou obtido no CRAS; (2) importação de arquivo cedido pela gestão municipal, só com termo de
  cessão ou instrumento equivalente; (3) API do Conecta, só se a CESJO ou um órgão público parceiro
  tiver adesão; (4) nenhuma integração com saúde e educação.
- As issues de implementação ficam propostas em `docs/integracoes.md` e só são criadas depois da
  resposta.
- O cadastro continua guardando NIS, "inscrito no CadÚnico" e "recebe BPC" por declaração (ADR 0140).

## Consequências

- Nada muda no código. A P12 continua aberta, sem decisão provisória.
- Quando a resposta chegar, a escolha entre as opções é direta: o documento de integrações já diz o
  que cada caminho exige.
- Enquanto isso, CadÚnico e BPC no sistema são só o que a família declarou. Não há prova de
  verificação nem data da última conferência.
- Qualquer integração futura precisa de ADR próprio com a base legal (P14) e o instrumento formal.
