# ADR 0144 — Tela de completar o cadastro do usuário atendido

**Status:** Aceito (2026-11-22) · Issue #40

## Contexto

O cadastro completo (ADR 0140) tem quase vinte campos, e boa parte da equipe usa o celular.
Alternativas consideradas: um assistente em passos (mais cliques para corrigir um campo só); abas
(escondem erros de validação); uma página só, com grupos.

## Decisão

- Rota `/atendidos/:id/cadastro`, com `ATENDIDOS_EDITAR`, aberta pelo botão "Completar cadastro" da
  ficha. Uma página com grupos (`fieldset`): Pessoa, Documentos, Endereço, Escolaridade, renda e
  benefícios e, para quem tem `ATENDIDOS_DADOS_SENSIVEIS`, Crença religiosa e saúde.
- O grupo sensível tem cadeado, borda de destaque e o aviso de que a leitura fica registrada. Sem a
  permissão, ele não aparece e não vai no envio.
- A faixa etária é recalculada na tela ao mudar a data de nascimento, com a mesma regra do backend.
- Mobile first: uma coluna no celular; campos curtos lado a lado a partir do tablet.
- A ficha ganha os blocos "Documentos e endereço", "Escolaridade, renda e benefícios" e, quando a API
  manda, "Crença religiosa e saúde", e mostra o gênero no bloco Pessoa.

## Consequências

- Campo novo no cadastro aparece na tela só depois de entrar no formulário e na ficha.
