# ADR 0016 — Sessão, telas de acesso e gestão de contas no frontend

**Status:** Aceito (2026-11-20) · Issues #13, #14 e #15

## Contexto

O frontend já tinha o serviço `Sessao`, o guard `exigePermissao` e o menu filtrado por permissão
(ADR 0007), mas nada carregava a sessão. Faltava decidir quando buscar a sessão, o que fazer com um
401 no meio do uso, como forçar a troca de senha e onde ficam as telas de acesso.

Alternativas consideradas: guardar a conta em `localStorage` (fica desatualizada e expõe dados no
navegador); buscar a sessão no guard a cada navegação (uma chamada por troca de página); deixar a
troca de senha obrigatória só no frontend (contornável; o backend também barra, ADR 0010).

## Decisão

- Na inicialização (`provideAppInitializer`), `GET /api/sessao` define a `Sessao`. Qualquer falha
  começa deslogado.
- `provideHttpClient` com XSRF (`XSRF-TOKEN` / `X-XSRF-TOKEN`) e um interceptor: 401 em qualquer
  chamada, menos o carregamento da sessão e o próprio login, limpa a sessão e leva para `/entrar`
  com `?retorno=` da página atual; 403 de troca de senha pendente leva para `/trocar-senha`.
- Rotas `/entrar` e `/trocar-senha` ficam fora do layout, em tela cheia. O layout e as rotas internas
  exigem sessão (`canActivate` e `canActivateChild`); com troca pendente, só `/trocar-senha` abre. O
  `?retorno=` só aceita caminho interno, para não servir de redirecionamento aberto.
- O layout passa a ser carregado sob demanda, para o pacote inicial caber no orçamento de 500 kB com
  o `HttpClient`, os formulários e o menu da conta.
- Menu da conta na barra superior, no lugar do botão "Minha conta": nome, login, perfil, "Trocar
  senha" e "Sair".
- Módulo `modulos/contas` com a permissão `CONTAS_GERENCIAR`: lista paginada (linhas empilhadas no
  celular, colunas a partir de tablet) e formulário de criação e edição. Ações que tiram acesso pedem
  confirmação. A senha temporária aparece num diálogo, uma vez, com botão de copiar.
- Os rótulos dos perfis ficam em `acesso/perfis.ts`, iguais aos do enum `Perfil` do backend, e o
  tamanho mínimo da senha repete o valor do ADR 0012 para validar antes de enviar.

## Consequências

- Uma sessão expirada leva de volta à tela de entrada e, depois de entrar, à página onde a pessoa
  estava.
- Mudar o tamanho mínimo da senha no backend exige mudar `TAMANHO_MINIMO_SENHA` no frontend; se
  esquecer, o backend ainda recusa e a tela mostra a mensagem dele.
