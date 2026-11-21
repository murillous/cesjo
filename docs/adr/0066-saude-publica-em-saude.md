# ADR 0066 — Estado de saúde publicado em `/saude`

**Status:** Aceito (2026-11-21) · Complementa o [ADR 0051](0051-caddy-como-borda.md)

## Contexto

O monitor externo de disponibilidade (issue #23) precisa de um endereço público que diga se o sistema
está no ar de verdade, com aplicação e banco respondendo. O ADR 0051 deixou o Actuator fora do Caddy.
A página inicial (`/`) só prova que o Caddy está de pé, porque é um arquivo estático.

Alternativas consideradas: publicar `/actuator/health` como está (expõe o caminho do Actuator e
convida a sondar os outros endpoints); um endpoint próprio em `/api` (duplica o que o Actuator já
faz, inclusive a verificação do banco e do disco).

## Decisão

- O Caddy atende `GET /saude` reescrevendo para `/actuator/health` na aplicação, com
  `Cache-Control: no-store`. Nenhum outro caminho do Actuator é publicado.
- A resposta é só o estado agregado (`{"status":"UP"}`, HTTP 200, ou `DOWN`, HTTP 503): o
  `show-details` do Actuator continua no padrão `never`, então não aparecem componentes, versões nem
  caminhos.
- O estado agregado inclui o banco (`db`) e o espaço em disco (`diskSpace`) do Spring Boot.

## Consequências

- Um monitor fora da VPS distingue "Caddy no ar, aplicação ou banco fora" de "tudo no ar".
- Qualquer pessoa pode ver se o sistema está no ar. Isso é aceito: não revela dado nenhum.
- Se alguém ligar `show-details` no futuro, os detalhes passam a sair em `/saude`; nesse caso o
  Caddyfile precisa mudar junto.
