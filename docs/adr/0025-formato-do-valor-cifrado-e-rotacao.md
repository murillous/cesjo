# ADR 0025 — Formato do valor cifrado, configuração das chaves e rotação

**Status:** Aceito (2026-11-20) · Detalha o [ADR 0004](0004-criptografia-na-aplicacao.md) (issue #17)

## Contexto

O ADR 0004 fixou AES-256-GCM na aplicação, com a chave vinda do ambiente e o identificador da chave
em cada valor, mas deixou para a issue #17 o desenho do conversor, o formato do valor cifrado e como
as chaves são configuradas. O formato precisa permitir trocar a chave sem recifrar tudo de uma vez,
servir para colunas de texto e para arquivos, e falhar de forma clara quando a chave falta.

Alternativas consideradas:

- **Formato do texto**: Base64 do formato binário inteiro (compacto, mas o identificador da chave
  fica ilegível no banco); JSON com campos separados (verboso); prefixo legível
  `v1:<id>:<Base64>` (permite achar no SQL os valores de uma chave antiga com `like 'v1:antiga:%'`).
- **Chave**: derivar a chave de uma senha com PBKDF2 (mais um parâmetro para errar e nenhum ganho,
  já que o segredo vem do ambiente); usar diretamente 32 bytes aleatórios em Base64.
- **Onde validar**: validação do Bean Validation nas propriedades (mensagem genérica de binding);
  validação no construtor do serviço de cifra (mensagem própria, falha na criação do bean).
- **Dados associados (AAD) do GCM**: nenhum; o cabeçalho com versão e identificador da chave.

## Decisão

Serviço `compartilhado.criptografia.Cifrador`, com cifra de bytes e de texto, e conversor JPA
`TextoCifradoConverter` para campos `String`, aplicado campo a campo com
`@Convert(converter = TextoCifradoConverter.class)` em colunas `text`. O Hibernate cria o conversor
pelo Spring, que injeta o `Cifrador`.

Configuração (`PropriedadesCriptografia`):

- `sgi.criptografia.chave-ativa`: identificador da chave que cifra valores novos;
- `sgi.criptografia.chaves.<id>`: cada chave, com 32 bytes aleatórios em Base64.
- O identificador tem de 1 a 32 caracteres entre letras minúsculas, dígitos e hífen. Por variável de
  ambiente (`SGI_CRIPTOGRAFIA_CHAVEATIVA` e `SGI_CRIPTOGRAFIA_CHAVES_<ID>`), o identificador não pode
  ter hífen, porque o Spring converte o nome da variável em minúsculas.

Formato binário (arquivos), versão 1:

| Bytes | Conteúdo |
|---|---|
| 1 | versão, `0x01` |
| 1 | tamanho `n` do identificador da chave |
| n | identificador da chave em ASCII |
| 12 | IV aleatório |
| resto | texto cifrado seguido da etiqueta GCM de 128 bits |

Formato textual (colunas), versão 1: `v1:<id>:<Base64 do IV seguido do texto cifrado e da etiqueta>`.

Nos dois formatos, o cabeçalho (versão e identificador) entra como dado associado do GCM: trocar o
identificador num valor faz a decifração falhar. O IV é novo a cada cifração (12 bytes do
`SecureRandom`), então o mesmo texto cifrado duas vezes gera valores diferentes. `null` continua
`null`.

Rotação: a decifração usa a chave indicada no próprio valor; a cifração usa sempre a chave ativa. Para
trocar de chave, acrescenta-se a nova em `sgi.criptografia.chaves`, aponta-se a `chave-ativa` para ela
e mantém-se a antiga enquanto houver valor cifrado com ela. `Cifrador.precisaRecifrar` e
`chaveDoTexto`/`chaveDoValor` servem a uma futura rotina de recifração, que não faz parte desta issue.

Inicialização: o construtor do `Cifrador` valida a configuração e lança
`ChaveCriptografiaInvalidaException` quando a chave ativa falta, não está entre as chaves, não é Base64
válido, não tem 32 bytes ou tem identificador inválido. A mensagem diz o que configurar e nunca
mostra o valor da chave. Como o bean é criado na subida, a aplicação não sobe.

Em desenvolvimento, a chave fica num `backend/config/application.properties` local, ignorado pelo git
(o Spring Boot lê `./config/` automaticamente). Nos testes, em
`src/test/resources/config/application.properties`, como o ADR 0004 já fixou.

## Consequências

- Valor cifrado é maior que o claro (cerca de 4/3 do tamanho mais uns 40 caracteres), por isso as
  colunas cifradas são `text`.
- O identificador da chave fica legível no banco, o que não expõe nada e facilita a rotação.
- Mudar o formato exige nova versão (`v2:` ou `0x02`), mantendo a leitura da `v1`.
- Perder uma chave que ainda tem valores torna esses valores ilegíveis; a cópia fora da VPS do ADR 0004
  vale para todas as chaves da configuração, não só a ativa.
