# Implantação na VPS com Docker Compose

Roteiro para pôr o CESJO SGI no ar numa VPS (issue #20). As decisões estão nos ADRs
[0005](adr/0005-hospedagem-vps-docker-compose.md), [0050](adr/0050-empacotamento-e-compose-de-producao.md),
[0051](adr/0051-caddy-como-borda.md), [0052](adr/0052-segredos-da-implantacao-no-env.md),
[0053](adr/0053-ci-das-imagens-de-implantacao.md) e, para o backup (issue #21),
[0054](adr/0054-backup-diario-com-restic.md) e [0065](adr/0065-retencao-dos-backups.md).

> **Provisório (PENDENTE-CESJO P15).** Provedor, domínio e quem administra a VPS ainda não foram
> definidos. Nada abaixo depende de um provedor específico; o que muda de uma VPS para outra fica no
> `deploy/.env` e no `deploy/backup.env`.

## Visão geral

```text
internet ──80/443──► caddy ──/api/*──► app:8080 ──► db:5432
                     │  (TLS, estáticos       │ (Spring Boot,      (Postgres 18,
                     │   do Angular)          │  perfil prod)       rede interna)
                     ▼                        ▼                      ▼
               cesjo-caddy-data         cesjo-arquivos          cesjo-pgdata
                                              ▲ (só leitura)          ▲ (pg_dump)
                                              └──────── backup ───────┘──► repositório restic
                                                                           fora da VPS
```

| Serviço | Imagem | Portas no host | Volume |
|---|---|---|---|
| `caddy` | `frontend/Dockerfile` (Caddy 2 com o build do Angular) | 80, 443, 443/udp | `cesjo-caddy-data`, `cesjo-caddy-config` |
| `app` | `backend/Dockerfile` (JRE 25, perfil `prod`) | nenhuma | `cesjo-arquivos` em `/var/lib/sgi/arquivos` |
| `db` | `postgres:18-alpine` | nenhuma | `cesjo-pgdata` |
| `backup` | `deploy/backup/Dockerfile` (Postgres 18 cliente, restic) | nenhuma | `cesjo-arquivos` (só leitura), `cesjo-backup-cache` |

O Actuator (`/actuator/health`) só é acessível de dentro da rede do Compose; o Caddy publica apenas
`/api/*` e os arquivos estáticos.

## Requisitos da VPS

- Linux de 64 bits com Docker Engine 24 ou mais novo e o plugin Docker Compose v2
  (`docker compose version`).
- Pelo menos 2 GB de RAM e 20 GB de disco. O build das imagens (Maven e Node) roda na própria VPS;
  com 1 GB, crie swap ou use imagens prontas (ver [Imagens prontas](#imagens-prontas)).
- Portas 80 e 443 (TCP) e 443 (UDP, HTTP/3) abertas no firewall do provedor e da VPS. Feche as
  demais, exceto o SSH.
- Um domínio com registro DNS `A` (e `AAAA`, se houver IPv6) apontando para o IP da VPS. O Caddy só
  consegue o certificado depois que o DNS propagar.
- `git` e `openssl` instalados.
- Um destino de backup fora da VPS aceito pelo restic: bucket S3 ou compatível (Backblaze B2,
  Wasabi, MinIO), SFTP noutra máquina, Azure ou Google Cloud Storage.

## Primeira implantação

1. Clone o repositório e entre na pasta de implantação:

   ```bash
   git clone https://github.com/murillous/cesjo.git /opt/cesjo
   cd /opt/cesjo/deploy
   ```

2. Crie o `.env` e o `backup.env` a partir dos exemplos e proteja os arquivos:

   ```bash
   cp .env.example .env
   cp backup.env.example backup.env
   chmod 600 .env backup.env
   ```

3. Gere os segredos:

   ```bash
   openssl rand -base64 32 | tr -d '/+='   # POSTGRES_PASSWORD
   openssl rand -base64 32                 # SGI_CRIPTOGRAFIA_CHAVES_PROD1
   openssl rand -base64 32                 # RESTIC_PASSWORD (backup.env)
   ```

4. Edite o `.env` e preencha todas as variáveis:

   | Variável | O que é |
   |---|---|
   | `SGI_DOMINIO` | Domínio público, por exemplo `sgi.exemplo.org.br`. |
   | `SGI_ACME_EMAIL` | E-mail para avisos do Let's Encrypt. |
   | `POSTGRES_DB`, `POSTGRES_USER` | Nome do banco e do usuário (padrão `cesjo`). |
   | `POSTGRES_PASSWORD` | Senha do banco gerada no passo 3. |
   | `SGI_CRIPTOGRAFIA_CHAVEATIVA` | Identificador da chave ativa, por exemplo `prod1`. |
   | `SGI_CRIPTOGRAFIA_CHAVES_PROD1` | Chave de 32 bytes em Base64 gerada no passo 3. O sufixo é o identificador em maiúsculas. |
   | `SGI_COORDENADOR_LOGIN`, `SGI_COORDENADOR_SENHA`, `SGI_COORDENADOR_NOME` | Primeira conta de Coordenador ([ADR 0014](adr/0014-primeira-conta-de-coordenador.md)). |

   Edite também o `backup.env`: destino (`RESTIC_REPOSITORY` e credenciais), `RESTIC_PASSWORD` e as
   URLs de alerta (ver [Backup](#backup)).

5. **Guarde uma cópia do `.env` e do `backup.env` fora da VPS** (gerenciador de senhas da
   coordenação, por exemplo). Sem as chaves de criptografia e a `RESTIC_PASSWORD`, o banco e os
   arquivos cifrados ficam ilegíveis, inclusive nos backups.

6. Construa as imagens e suba a pilha:

   ```bash
   docker compose up -d --build --wait
   ```

   `--wait` só retorna quando o banco, a aplicação e o Caddy estão saudáveis. A primeira subida aplica
   as migrações do Flyway e cria a conta de Coordenador.

7. Confira:

   ```bash
   docker compose ps                                   # todos "healthy"
   docker compose exec app curl -fsS http://localhost:8080/actuator/health
   curl -I https://SEU-DOMINIO/                        # 200 com HSTS
   ```

8. Entre em `https://SEU-DOMINIO/` com o login do Coordenador e troque a senha inicial, como o sistema
   pede.

9. Apague `SGI_COORDENADOR_SENHA` do `.env` (a conta já existe e a variável passa a ser ignorada) e
   recrie a aplicação: `docker compose up -d app`.

10. Rode o primeiro backup à mão e confira que ele chegou ao destino:

    ```bash
    docker compose exec backup backup.sh
    docker compose exec backup restic snapshots
    ```

## Atualizar para uma versão nova

```bash
cd /opt/cesjo
git pull
cd deploy
docker compose up -d --build --wait
docker image prune -f
```

As migrações do Flyway rodam na subida da aplicação. Antes de atualizar, rode um backup à mão: `docker compose exec backup backup.sh`.
Durante a troca do contêiner da aplicação, a API fica fora por alguns segundos.

## Trocar a chave de criptografia

1. Gere uma chave nova com `openssl rand -base64 32`.
2. Acrescente `SGI_CRIPTOGRAFIA_CHAVES_PROD2=<chave>` no `.env` **sem apagar a antiga**.
3. Mude `SGI_CRIPTOGRAFIA_CHAVEATIVA=prod2`.
4. `docker compose up -d app`.
5. Atualize a cópia do `.env` guardada fora da VPS.

Valores novos passam a ser cifrados com a `prod2`; os antigos continuam legíveis com a `prod1`
([ADR 0025](adr/0025-formato-do-valor-cifrado-e-rotacao.md)).

## Backup

O serviço `backup` roda todo dia no horário de `SGI_BACKUP_HORARIO` (padrão 3h, fuso `TZ`):

1. `pg_dump` do banco;
2. `restic backup` do dump e do volume de arquivos, cifrados pelo restic (AES-256) e enviados para
   `RESTIC_REPOSITORY`;
3. retenção com `restic forget --prune`: 7 diários, 4 semanais e 6 mensais por padrão
   (`SGI_BACKUP_MANTER_*` no `backup.env`; provisório, PENDENTE-CESJO P14);
4. `restic check` do repositório;
5. aviso em `SGI_BACKUP_URL_SUCESSO` ou `SGI_BACKUP_URL_FALHA`.

> **Provisório (PENDENTE-CESJO P15).** O destino ainda não foi escolhido. Qualquer repositório do
> restic fora da VPS serve; nunca use um caminho da própria VPS.

### Alerta de falha

Crie um monitor de tarefa agendada num serviço de "ping" (Healthchecks.io, Cronitor, Better Stack),
com período de 1 dia e tolerância de algumas horas, e ponha as URLs no `backup.env`. No
Healthchecks.io:

```bash
SGI_BACKUP_URL_SUCESSO=https://hc-ping.com/<uuid>
SGI_BACKUP_URL_FALHA=https://hc-ping.com/<uuid>/fail
```

O serviço avisa (e-mail, Telegram, WhatsApp, conforme configurado) quando chega o ping de falha e
também quando nenhum ping de sucesso chega no prazo, o que cobre o backup que nem rodou. Depois de
mudar o `backup.env`, recrie o serviço: `docker compose up -d backup`.

### Comandos do backup

| Tarefa | Comando (em `deploy/`) |
|---|---|
| Backup agora | `docker compose exec backup backup.sh` |
| Listar snapshots | `docker compose exec backup restic snapshots` |
| Ver o conteúdo do último | `docker compose exec backup restic ls latest` |
| Conferir a integridade lendo todos os dados | `docker compose exec backup restic check --read-data` |
| Logs dos backups | `docker compose logs backup` |

### Restaurar (resumo)

O procedimento completo de recuperação de desastres, numa VPS nova, é a issue #22. Em resumo, com o
`backup.env` e o `.env` guardados fora da VPS:

```bash
docker compose stop app
docker compose exec backup sh -c 'restic dump latest /var/backups/sgi/cesjo.dump > /tmp/cesjo.dump'
docker compose exec backup sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" pg_restore -h db -U "$POSTGRES_USER"   -d "$POSTGRES_DB" --clean --if-exists --no-owner /tmp/cesjo.dump && rm /tmp/cesjo.dump'
docker compose run --rm --no-deps -v cesjo-arquivos:/destino --entrypoint sh backup -c   'restic restore latest --include /dados/arquivos --target /tmp/r && cp -a /tmp/r/dados/arquivos/. /destino/'
docker compose start app
```

O volume de arquivos é montado só leitura no serviço `backup`; por isso a restauração dos arquivos
usa um contêiner avulso com o volume montado para escrita.

## Operação do dia a dia

| Tarefa | Comando (em `deploy/`) |
|---|---|
| Ver o estado | `docker compose ps` |
| Logs da aplicação | `docker compose logs -f app` |
| Reiniciar a aplicação | `docker compose restart app` |
| Parar tudo (dados preservados) | `docker compose down` |
| Console do banco | `docker compose exec db psql -U cesjo cesjo` |

**Nunca rode `docker compose down -v`** em produção: o `-v` apaga os volumes do banco, dos arquivos
cifrados e dos certificados.

## Imagens prontas

Se a VPS não aguenta o build, construa as imagens em outra máquina (ou num registro) e informe no
`.env`:

```bash
SGI_IMAGEM_APP=registro.exemplo/cesjo-sgi-backend:1.0.0
SGI_IMAGEM_CADDY=registro.exemplo/cesjo-sgi-caddy:1.0.0
```

e suba com `docker compose pull && docker compose up -d --no-build --wait`.

## Testar localmente

Numa máquina com Docker, use `SGI_DOMINIO=localhost` no `.env`: o Caddy emite um certificado da sua
CA interna (o navegador avisa que não confia). O job `implantacao` do CI faz exatamente isso a cada
PR ([ADR 0053](adr/0053-ci-das-imagens-de-implantacao.md)).

## Problemas comuns

- **Caddy não obtém o certificado:** confira o DNS (`dig +short SEU-DOMINIO`) e se as portas 80 e
  443 estão abertas. Veja `docker compose logs caddy`.
- **Aplicação reinicia em laço:** `docker compose logs app`. Causas comuns: chave ativa sem valor ou
  com tamanho errado, senha do banco diferente da usada na criação do volume.
- **Backup falha com `repository does not exist` ou senha errada:** confira `RESTIC_REPOSITORY`,
  `RESTIC_PASSWORD` e as credenciais no `backup.env`. O script só cria o repositório quando o destino
  responde que ele não existe.
- **Senha do banco trocada no `.env` depois da primeira subida:** o Postgres só lê
  `POSTGRES_PASSWORD` ao criar o volume. Troque também no banco:
  `docker compose exec db psql -U cesjo -c "ALTER USER cesjo PASSWORD '<nova>'"`.

## Ainda não coberto

- Procedimento completo de recuperação de desastres, com restauração testada: issue #22.
- Monitoramento externo e alertas: issue #23.
