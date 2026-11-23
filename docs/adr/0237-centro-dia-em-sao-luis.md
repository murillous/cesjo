# ADR 0237 — Centro-Dia em São Luís (MA)

**Status:** Aceito (2026-11-23) · Complementa o [ADR 0225](0225-nenhuma-integracao-antes-da-p12.md)

## Contexto

O levantamento de integrações ([`docs/integracoes.md`](../integracoes.md)) assumia São José dos
Campos (SP) como hipótese, porque o documento de requisitos não diz o município. A equipe do projeto
informou que o Centro-Dia da CESJO fica em São Luís (MA).

Notícias de 2017 descrevem o Centro-Dia de São Luís como serviço da Prefeitura, administrado pela
Secretaria Municipal da Criança e Assistência Social (Semcas), no Centro Educacional São José Operário,
para pessoas com deficiência de 18 a 59 anos. A Semcas também faz as inscrições do CadÚnico no
município. Não foi encontrada fonte oficial atual que confirme a natureza jurídica.

## Decisão

- O município é São Luís (MA). O levantamento de integrações passa a usar São Luís.
- A hipótese de trabalho para a natureza jurídica é órgão público municipal (Semcas), até a CESJO
  confirmar. O levantamento continua tratando também o caso de OSC.
- A decisão do ADR 0225 não muda: nenhuma integração antes da resposta completa da P12.

## Consequências

- Se o Centro-Dia for da Semcas, o caminho do arquivo do CadÚnico fica mais simples: o gestor do
  CadÚnico é a mesma secretaria, e o uso para gestão de política pública é permitido ao próprio
  gestor (Decreto 11.016/2022, art. 13, §3º). Não seria cessão a terceiro, mas ainda pede ato interno
  e registro da operação (LGPD, art. 37).
- São Luís é capital e pode aderir ao Conecta gov.br, mas as APIs do CadÚnico e do BPC não aparecem
  na lista para municípios. A API continua improvável.
- Nada muda no código.
