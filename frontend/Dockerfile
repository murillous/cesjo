# syntax=docker/dockerfile:1
# Imagem do Caddy com o build de produção do Angular em /srv (issue #20, ADR 0050).
# O Caddyfile não entra na imagem: vem de deploy/Caddyfile, montado pelo compose.
# Construir a partir de frontend/: docker build -t cesjo-sgi-caddy frontend

# --- Etapa 1: build do Angular (mesma versão do Node do .nvmrc) ---
FROM node:24-alpine AS compilacao
WORKDIR /fonte
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund
COPY . .
RUN npm run build

# --- Etapa 2: Caddy servindo os estáticos ---
FROM caddy:2-alpine
COPY --from=compilacao /fonte/dist/sgi-frontend/browser/ /srv/
