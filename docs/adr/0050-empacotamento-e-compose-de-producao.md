# ADR 0050 — Imagens Docker e compose de produção em `deploy/`

**Status:** Provisório (PENDENTE-CESJO P15) (2026-11-21) · Detalha o [ADR 0005](0005-hospedagem-vps-docker-compose.md)

## Contexto

O ADR 0005 escolheu uma VPS com Docker Compose (Caddy, aplicação e Postgres). Falta dizer como as
imagens são construídas, onde fica o compose e como a pilha se organiza. Ainda não se sabe o
provedor, o domínio nem quem administra a VPS (pergunta 15), e o projeto não tem registro de imagens.

Alternativas consideradas: imagem do backend com Buildpacks (`spring-boot:build-image`), que precisa
de Docker no build do Maven e esconde o que entra na imagem; um único contêiner com Caddy e Java; o
Caddy servindo os estáticos de um volume preenchido à mão; publicar imagens num registro (GHCR) e só
puxá-las na VPS.

## Decisão

- **Backend:** `backend/Dockerfile` multi-stage. A primeira etapa compila o jar com o Maven Wrapper
  no `eclipse-temurin:25-jdk`, sem rodar testes (o CI já roda). A segunda usa `eclipse-temurin:25-jre`,
  copia as camadas extraídas do jar (`-Djarmode=tools extract --layers`), roda como usuário sem
  privilégio (uid 10001), ativa o perfil `prod` e tem healthcheck por `curl` no
  `/actuator/health`. `curl` é o único pacote acrescentado.
- **Frontend:** `frontend/Dockerfile` faz o `npm run build` no `node:24-alpine` (mesma versão do
  `.nvmrc`) e copia `dist/sgi-frontend/browser` para o `/srv` de uma imagem `caddy:2-alpine`. O
  Caddyfile não entra na imagem: fica em `deploy/Caddyfile` e é montado pelo compose, para ser
  ajustado sem reconstruir.
- **Compose:** `deploy/compose.yaml`, com os serviços `db` (`postgres:18-alpine`), `app` e `caddy`.
  Só o Caddy publica portas (80, 443 e 443/udp). O banco fica numa rede `interna` sem saída para a
  internet; a aplicação está nessa rede e na rede `borda`, junto com o Caddy. A ordem de subida segue
  os healthchecks (`db` saudável, depois `app` saudável, depois `caddy`).
- **Volumes nomeados com nome fixo:** `cesjo-pgdata` (banco), `cesjo-arquivos` (arquivos cifrados,
  montado em `/var/lib/sgi/arquivos`, que é o `SGI_ARQUIVOS_DIRETORIO`), `cesjo-caddy-data` (certificados)
  e `cesjo-caddy-config`. O nome fixo deixa o backup (issue #21) e a restauração independentes do
  nome do projeto Compose.
- **Build na própria VPS:** `docker compose up -d --build` constrói as duas imagens a partir do
  repositório clonado. As variáveis `SGI_IMAGEM_APP` e `SGI_IMAGEM_CADDY` permitem trocar por imagens
  de um registro sem mexer no compose.
- PENDENTE-CESJO(P15): domínio, e-mail do Let's Encrypt e credenciais ficam todos no `deploy/.env`
  ([ADR 0052](0052-segredos-da-implantacao-no-env.md)); nada no compose depende do provedor.

## Consequências

- A implantação é `git pull` e `docker compose up -d --build` numa VPS com Docker, sem registro de
  imagens nem pipeline de entrega.
- Construir na VPS gasta CPU e memória durante o build (Maven e Node). Numa VPS pequena (1 GB) pode
  faltar memória; aí a saída é publicar as imagens num registro e usar `SGI_IMAGEM_*`.
- Backup (issue #21) e monitoramento (issue #23) entram como serviços novos no mesmo compose, lendo os
  volumes de nome fixo e usando a rede interna.
- Quando a equipe responder a pergunta 15, um ADR novo confirma ou ajusta provedor, domínio e quem
  opera a VPS.
