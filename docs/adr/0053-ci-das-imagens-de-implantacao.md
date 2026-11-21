# ADR 0053 — CI constrói as imagens e testa a pilha sem publicar

**Status:** Aceito (2026-11-21)

## Contexto

As imagens e o compose de produção ([ADR 0050](0050-empacotamento-e-compose-de-producao.md)) só
valem se forem testados. A máquina de desenvolvimento não tem Docker; os runners Ubuntu do GitHub
Actions têm. Ainda não há registro de imagens nem VPS (pergunta 15).

Alternativas consideradas: só `docker compose config` (não pega erro de build nem de proxy);
publicar as imagens no GHCR a cada merge (sem consumidor por enquanto).

## Decisão

O workflow `CI` ganha o job `implantacao`, que, em `deploy/`:

1. valida o compose com o `.env.example` (`docker compose config`);
2. gera um `.env` de teste com `SGI_DOMINIO=localhost`, senha e chave aleatórias;
3. constrói as duas imagens (`docker compose build`), sem publicar;
4. sobe a pilha com `--wait` e faz testes de fumaça: frontend e rota da SPA pelo Caddy, redirecionamento
   HTTP para HTTPS, `/api` chegando na aplicação, API exigindo sessão, Actuator fora do Caddy e
   saudável dentro da rede, aplicação rodando sem root;
5. mostra os logs e derruba a pilha com os volumes.

## Consequências

- Erro de Dockerfile, de Caddyfile ou de configuração do perfil `prod` aparece no PR.
- O job leva alguns minutos a mais por PR (build do Maven e do Node sem cache de camadas).
- Publicar imagens num registro fica para quando houver VPS e se decidir não construir nela.
