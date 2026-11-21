# ADR 0051 — Caddy como borda: TLS, estáticos, proxy de `/api` e cabeçalhos

**Status:** Aceito (2026-11-21) · Complementado pelo [ADR 0066](0066-saude-publica-em-saude.md) (`/saude`)

## Contexto

A pilha de produção ([ADR 0050](0050-empacotamento-e-compose-de-producao.md)) precisa de um único
ponto de entrada com HTTPS. O cookie de sessão só sai com `Secure` em produção (ADR 0011), o
frontend chama a API em `/api` na mesma origem e a auditoria grava o IP de quem faz a requisição
(ADR 0020). O Actuator está liberado sem login para o healthcheck (ADR 0010).

Alternativas consideradas: Nginx com Certbot (mais peças para renovar o certificado); a aplicação
servindo os estáticos do Angular e terminando o TLS (mistura responsabilidades e dificulta trocar o
frontend sem reconstruir o backend).

## Decisão

- O Caddy obtém e renova o certificado do Let's Encrypt para `SGI_DOMINIO`, com `SGI_ACME_EMAIL` como
  contato, e redireciona HTTP para HTTPS (comportamento padrão).
- Só `/api/*` é repassado para `app:8080`. O Actuator não é publicado: qualquer caminho fora de
  `/api/` é atendido pelos estáticos, e `/actuator` cai no `index.html` como qualquer rota da SPA.
- Rota desconhecida vira `index.html` (`try_files`), para o roteamento do Angular funcionar ao
  recarregar a página.
- As saídas do `ng build` com hash no nome (`main-*.js`, `polyfills-*.js`, `chunk-*.js`,
  `styles-*.css` e `media/*`) vão com `Cache-Control: immutable` de um ano; o resto, inclusive o
  `index.html`, com `no-cache`.
- Corpo de requisição em `/api` limitado a 12 MB (arquivo de 10 MB mais o envelope multipart,
  ADR 0027).
- Cabeçalhos em toda resposta: HSTS de um ano, `X-Content-Type-Options: nosniff`,
  `X-Frame-Options: DENY`, `Referrer-Policy: same-origin`, `Permissions-Policy` sem câmera, microfone
  e localização, e sem o cabeçalho `Server`.
- Sem log de acesso no Caddy: URLs da API podem levar identificadores de pessoas (LGPD).
- No perfil `prod`, `server.forward-headers-strategy=native`: o Tomcat aceita `X-Forwarded-For` e
  `X-Forwarded-Proto` só de endereços internos (a rede do Compose), então a auditoria grava o IP real
  e a aplicação enxerga o esquema `https`. O perfil `prod` também faz encerramento gracioso.

## Consequências

- TLS sem intervenção manual, desde que o DNS aponte para a VPS e as portas 80 e 443 estejam abertas.
- Para diagnóstico de acesso, use os logs da aplicação e a auditoria; o Caddy não guarda histórico de
  requisições. Se for preciso, liga-se `log` no Caddyfile com filtro de campos.
- Uma Content-Security-Policy ainda não foi definida; entra quando o frontend estabilizar as fontes e
  os estilos que carrega.
