# ADR 0052 — Segredos da implantação num `.env` fora do repositório

**Status:** Aceito (2026-11-21)

## Contexto

A implantação precisa de segredos: senha do banco, chaves de criptografia (ADR 0025, com nomes
variáveis `SGI_CRIPTOGRAFIA_CHAVES_<ID>`) e a senha inicial do Coordenador (ADR 0014). O repositório é
público. Ainda não há cofre de segredos nem se sabe quem administra a VPS (pergunta 15).

Alternativas consideradas: Docker secrets em arquivos (o Spring Boot leria com `configtree:`, mas
cada chave nova vira arquivo e montagem nova); um cofre externo (custo e operação acima do porte do
projeto); listar cada variável no `environment` do serviço `app` (impossível para as chaves, que têm
nomes variáveis).

## Decisão

- Todos os segredos e parâmetros da implantação ficam em `deploy/.env`, criado a partir do
  `deploy/.env.example` (que lista todas as variáveis com valores fictícios). O `.env` está no
  `.gitignore` e deve ter permissão `600` na VPS.
- O serviço `app` recebe o `.env` inteiro por `env_file`, mais as variáveis montadas pelo compose
  (URL do banco, diretório dos arquivos, perfil `prod`). O `db` e o `caddy` recebem só o que usam.
- O compose falha na subida se faltar `POSTGRES_PASSWORD`, `SGI_CRIPTOGRAFIA_CHAVEATIVA`,
  `SGI_DOMINIO` ou `SGI_ACME_EMAIL`; a aplicação falha se a chave ativa não tiver valor válido.
- Uma cópia do `.env` (sobretudo das chaves) é guardada fora da VPS, num lugar escolhido pela
  coordenação. Sem as chaves, o backup é inútil.

## Consequências

- Implantação simples: um arquivo para preencher e proteger.
- Quem tem acesso de root na VPS lê todos os segredos; isso é aceito no porte atual.
- A aplicação recebe também variáveis que não usa (domínio, e-mail do ACME), sem efeito.
- Trocar para Docker secrets ou um cofre depois é uma mudança só no compose.
