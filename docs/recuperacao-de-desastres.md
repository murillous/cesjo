# Recuperação de desastres

Como pôr o CESJO SGI de volta no ar numa VPS nova quando a atual se perdeu: disco corrompido,
provedor fora do ar, conta suspensa, invasão ou exclusão acidental (issue #22, RNF 3.4). As decisões
estão nos ADRs [0120](adr/0120-procedimento-de-restauracao.md),
[0121](adr/0121-guarda-dos-segredos-fora-da-vps.md) e
[0122](adr/0122-teste-de-restauracao-no-ci-e-exercicio.md). A implantação normal está em
[implantacao.md](implantacao.md).

## O que é preciso ter fora da VPS

Sem estes três itens não há recuperação. O backup sem as chaves é ilegível.

| Item | Onde está | Para quê |
|---|---|---|
| `deploy/.env` | cópias da coordenação (ver [Segredos](#segredos)) | chaves de criptografia da aplicação (`SGI_CRIPTOGRAFIA_*`), senha do banco, domínio |
| `deploy/backup.env` | cópias da coordenação | `RESTIC_REPOSITORY`, `RESTIC_PASSWORD` e credenciais do destino |
| Repositório restic | destino do backup, fora da VPS ([ADR 0054](adr/0054-backup-diario-com-restic.md)) | dump do banco e volume de arquivos |

O código vem do GitHub (`git clone`). O certificado TLS é emitido de novo pelo Caddy.

## Segredos

<!-- PENDENTE-CESJO(P15): guardiões e local das cópias ainda não definidos; duas cópias, gerenciador de senhas institucional e envelope lacrado (ADR 0121) -->

> **Provisório (PENDENTE-CESJO P15).** Ainda não se sabe quem administra a VPS; os guardiões das
> cópias são definidos pela coordenação.

A coordenação guarda **duas cópias** do `.env` e do `backup.env`:

1. num gerenciador de senhas com conta da instituição (Bitwarden, 1Password ou parecido), com acesso
   de pelo menos duas pessoas;
2. impressa ou num pen drive cifrado, em envelope lacrado no cofre ou arquivo da instituição.

Nunca guarde as cópias na VPS, no repositório, em e-mail nem no mesmo provedor do destino do backup.

Lista de conferência a cada mudança de segredo (rotação de chave, troca de senha ou de destino):

- [ ] cópia do gerenciador de senhas atualizada;
- [ ] envelope substituído, com o antigo destruído;
- [ ] as chaves antigas continuam nas cópias enquanto houver dado cifrado com elas (ADR 0025);
- [ ] data anotada na [tabela de exercícios](#registro-dos-exercícios) como "troca de segredo".

## Restaurar numa VPS nova

Tempo de referência: ver o [registro dos exercícios](#registro-dos-exercícios). A maior parte do tempo
real é o build das imagens e o download do repositório restic.

1. **Prepare a VPS** com os [requisitos da implantação](implantacao.md#requisitos-da-vps): Docker,
   Compose, portas 80 e 443 e acesso de saída ao destino do backup.

2. **Clone o código** e entre em `deploy/`:

   ```bash
   git clone https://github.com/murillous/cesjo.git /opt/cesjo
   cd /opt/cesjo/deploy
   ```

3. **Recoloque os segredos** a partir da cópia guardada fora da VPS:

   ```bash
   nano .env          # cole o conteúdo guardado
   nano backup.env    # cole o conteúdo guardado
   chmod 600 .env backup.env
   ```

   Se o domínio mudou, troque `SGI_DOMINIO` e aponte o DNS para o IP da VPS nova.

4. **Confira o acesso ao backup** antes de mexer em qualquer coisa:

   ```bash
   docker compose build
   docker compose run --rm --no-deps --entrypoint restic backup snapshots
   ```

   A lista deve mostrar os snapshots diários com host `cesjo`. Anote o ID do que vai restaurar (o
   padrão é o mais recente).

5. **Suba só o banco** (vazio):

   ```bash
   docker compose up -d --wait db
   ```

   Não suba a aplicação antes da restauração: ela criaria o esquema e uma conta de Coordenador nova.

6. **Restaure** o banco e os arquivos:

   ```bash
   docker compose run --rm --no-deps -v cesjo-arquivos:/restauro/arquivos \
     --entrypoint restaurar.sh backup            # ou: ... backup <id-do-snapshot>
   ```

   O script termina com `fim: sucesso em N s`. Se falhar, nada fica pela metade no banco (a
   restauração do banco é uma transação só); corrija e rode de novo.

7. **Suba o resto**:

   ```bash
   docker compose up -d --wait
   ```

8. **Confira**:

   ```bash
   curl https://SEU-DOMINIO/saude                       # "status":"UP"
   docker compose exec backup restic snapshots          # backup continua apontando para o mesmo destino
   ```

   Entre no sistema com uma conta existente, abra a lista de espera e baixe um anexo: isso prova que
   as chaves de criptografia batem com os dados restaurados.

9. **Rode um backup na hora** (`docker compose exec backup backup.sh`), religue o monitor externo
   se a URL mudou ([implantacao.md](implantacao.md#monitoramento)) e registre o incidente e o tempo
   na tabela abaixo.

## Restaurar só um dado antigo

Para recuperar um registro apagado sem desfazer o resto, restaure o dump num banco à parte e copie
só o que precisa:

```bash
docker compose exec backup sh -c 'restic dump <id-do-snapshot> /var/backups/sgi/cesjo.dump > /tmp/antigo.dump'
docker compose exec db sh -c 'createdb -U "$POSTGRES_USER" cesjo_antigo'
docker compose exec backup sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" pg_restore -h db -U "$POSTGRES_USER" -d cesjo_antigo --no-owner /tmp/antigo.dump && rm /tmp/antigo.dump'
# consulte com: docker compose exec db psql -U cesjo cesjo_antigo
# ao terminar:  docker compose exec db sh -c 'dropdb -U "$POSTGRES_USER" cesjo_antigo'
```

Campos de texto sensível continuam cifrados no banco à parte; para lê-los, use o sistema.

## Exercício trimestral

A cada três meses, restaure o último backup numa VPS ou máquina descartável seguindo
[Restaurar numa VPS nova](#restaurar-numa-vps-nova), com outro domínio (ou `localhost`), e anote o
resultado abaixo. Depois, apague a máquina descartável: ela tem uma cópia dos dados sensíveis.

O CI faz o mesmo percurso a cada PR, numa base quase vazia (job `implantacao`, passo "Recuperação de
desastres"), e publica o tempo no resumo da execução.

## Registro dos exercícios

| Data | Onde | Snapshot | Tempo (banco vazio até o sistema no ar) | Resultado | Quem |
|---|---|---|---|---|---|
| 2026-11-21 | GitHub Actions, PR #90, execução 44948327630 (base de teste quase vazia) | o do próprio teste | 19 s (restaurar.sh: 1 s) | sucesso | CI |
