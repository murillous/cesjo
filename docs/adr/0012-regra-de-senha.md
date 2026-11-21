# ADR 0012 — Regra de senha e senhas temporárias

**Status:** Aceito (2026-11-20)

## Contexto

As contas escolhem a própria senha no primeiro acesso e depois de uma redefinição. É preciso uma
regra que resista a ataque e que a equipe consiga seguir. Regras de composição ("uma maiúscula, um
número, um símbolo") levam a senhas previsíveis como `Cesjo@2026` e são desaconselhadas pela
NIST SP 800-63B, que recomenda tamanho mínimo, aceitar frases longas e não exigir composição.

Alternativas consideradas: mínimo de 8 caracteres com composição obrigatória; mínimo de 15
caracteres (o que a NIST pede para senha como único fator), que pesa para quem não usa gerenciador
de senhas.

## Decisão

- Mínimo de **12 caracteres** (`sgi.acesso.senha-tamanho-minimo`), máximo de 128, qualquer caractere,
  sem regra de composição. Espaços valem, para permitir frases.
- A senha não pode ser só espaços, nem igual ao login, nem igual à senha atual na troca.
- Quem cria a conta ou redefine a senha não escolhe a senha: o sistema gera uma **senha temporária**
  de 16 caracteres (letras e números sem os que se confundem, como `0`/`O` e `1`/`l`), com
  `SecureRandom`, mostrada uma única vez na resposta. A conta precisa trocá-la no próximo acesso.
- A tela de troca de senha explica a regra e sugere usar uma frase.

## Consequências

- A regra fica num lugar só (`RegraSenha`), e o tamanho mínimo muda por propriedade.
- O Coordenador nunca conhece a senha definitiva de ninguém; a temporária só serve até a troca.
- Se a pessoa perder a senha temporária antes de trocar, o Coordenador redefine de novo.
