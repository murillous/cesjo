# ADR 0014 — Primeira conta de Coordenador por variáveis de ambiente

**Status:** Aceito (2026-11-20)

## Contexto

O ADR 0003 decidiu que a primeira conta de Coordenador vem de variável de ambiente na implantação.
Faltava decidir os nomes das variáveis, quando a conta é criada e o que acontece com uma configuração
errada.

Alternativas consideradas: migração Flyway com uma conta fixa (senha conhecida no repositório
público); comando separado de linha de comando (mais um passo na implantação); criar a conta toda vez
que as variáveis existirem (sobrescreveria uma conta já em uso).

## Decisão

- Variáveis `SGI_COORDENADOR_LOGIN`, `SGI_COORDENADOR_SENHA` e, opcional, `SGI_COORDENADOR_NOME`
  (padrão "Coordenador"), lidas pelas propriedades `sgi.acesso.primeira-conta.*`.
- Na subida, se a tabela `conta_acesso` está vazia e login e senha foram informados, cria a conta de
  Coordenador. Se já existe qualquer conta, não faz nada. Se a tabela está vazia e faltam as
  variáveis, só registra um aviso no log.
- A senha informada segue a regra do ADR 0012 e é temporária: a conta precisa trocá-la no primeiro
  acesso. Login ou senha inválidos impedem a subida, com mensagem clara.
- A criação é registrada na auditoria.

## Consequências

- Depois do primeiro acesso, a senha da variável não vale mais e as variáveis podem ser removidas do
  ambiente.
- Uma implantação nova sem as variáveis sobe, mas ninguém consegue entrar até defini-las e reiniciar.
