# CESJO SGI

<p align="center">
  <img src="docs/frontend/assets/logo-pobres-servos-cesjo.png" alt="Logo Pobres Servos CESJO" width="500">
</p>

Sistema de gestão de informações do Centro-Dia, que atende pessoas com deficiência (física, sensorial,
intelectual, múltipla, TEA e T21) de 18 a 59 anos.

O projeto recomeçou do zero em 2026-11-20. O MVP anterior (Spring Boot 3.5, SQLite, React 18) está
preservado na tag `mvp-legado`.

## Requisitos

A única fonte de requisitos é o
[Documento de Requisitos do Sistema Centro-Dia](docs/requisitos/documentoRequisitosCentroDia.pdf)
(RF001 a RF017, requisitos não funcionais, perfis de usuário e integrações).

## Stack

| Camada | Escolha |
|---|---|
| Backend | Java 25, Spring Boot 4.2 (Web MVC, Data JPA, Validation, Actuator) |
| Banco | PostgreSQL 18, migrações com Flyway |
| Testes | JUnit, Testcontainers com Postgres real |
| Frontend | Angular 22, Angular Material 22 com tema institucional |

O planejamento completo (arquitetura, modelo de dados, permissões, ordem de entrega e backlog) está em
[docs/planejamento.md](docs/planejamento.md). Cada decisão do projeto, com o motivo, fica num ADR em
[docs/adr](docs/adr/README.md). Decisões provisórias, tomadas enquanto a equipe CESJO não responde, ficam
marcadas no código com `PENDENTE-CESJO` (`git grep -n "PENDENTE-CESJO"`).

## Rodar o backend

Pré-requisitos: JDK 25 e Docker em execução.

```bash
cd backend
./mvnw spring-boot:run   # sobe o Postgres pelo compose.yaml (Spring Boot Docker Compose)
./mvnw verify            # testes de integração sobem o Postgres via Testcontainers
```

### Dados de demonstração

Para apresentar o sistema com dados fictícios, suba o backend com o perfil `demo` e o banco vazio
([ADR 0237](docs/adr/0237-carga-de-demonstracao.md)):

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

A carga grava cerca de dois anos de uso: 220 pessoas na lista de espera, 150 delas inseridas no
serviço, além de visitas, escutas, PIAs, atividades, comunicações, parcerias, eventos, doações e o log
de auditoria. Ela cria uma conta por perfil, todas com a senha `demonstracao` e sem troca obrigatória:

| Login | Perfil |
|---|---|
| `coordenador` | Coordenador |
| `equipe.tecnica` | Equipe Técnica |
| `administrativo` | Administrativo |
| `consulta` | Consulta |

A carga só roda quando o banco não tem nenhuma pessoa nem conta e se recusa a subir com o perfil
`prod`. Para recomeçar, apague o volume do Postgres local (`docker compose down -v` na pasta
`backend`) e os arquivos em `backend/dados/arquivos`.

### Chave de criptografia

Texto sensível e arquivos são cifrados com AES-256-GCM na aplicação
([ADR 0004](docs/adr/0004-criptografia-na-aplicacao.md), [ADR 0025](docs/adr/0025-formato-do-valor-cifrado-e-rotacao.md)).
A aplicação não sobe sem uma chave ativa válida. Os testes já trazem uma chave própria; o
`spring-boot:run` precisa de uma local.

Para desenvolvimento, gere uma chave de 32 bytes e grave-a em `backend/config/application.properties`,
que o git ignora e o Spring Boot lê automaticamente:

```bash
cd backend
mkdir -p config
CHAVE=$(openssl rand -base64 32)   # ou: head -c 32 /dev/urandom | base64
printf 'sgi.criptografia.chave-ativa=dev1\nsgi.criptografia.chaves.dev1=%s\n' "$CHAVE" > config/application.properties
```

Em produção, as chaves vêm de variáveis de ambiente:

| Variável | Conteúdo |
|---|---|
| `SGI_CRIPTOGRAFIA_CHAVEATIVA` | identificador da chave que cifra valores novos, por exemplo `prod1` |
| `SGI_CRIPTOGRAFIA_CHAVES_<ID>` | a chave de cada identificador, 32 bytes em Base64, por exemplo `SGI_CRIPTOGRAFIA_CHAVES_PROD1` |
| `SGI_ARQUIVOS_DIRETORIO` | diretório (volume) onde ficam os arquivos cifrados; obrigatório no perfil `prod` |

Use identificadores só com letras minúsculas e dígitos nas variáveis de ambiente. Para trocar a chave,
acrescente a nova, aponte `SGI_CRIPTOGRAFIA_CHAVEATIVA` para ela e mantenha a antiga enquanto houver
dado cifrado com ela. Sem a chave, o backup do banco e dos arquivos é inútil: guarde uma cópia de
todas as chaves fora da VPS.

### Primeira conta de Coordenador

Com o banco sem nenhuma conta, o backend cria na subida uma conta de Coordenador a partir destas
variáveis de ambiente ([ADR 0014](docs/adr/0014-primeira-conta-de-coordenador.md)):

| Variável | Obrigatória | Conteúdo |
|---|---|---|
| `SGI_COORDENADOR_LOGIN` | sim | Login, de 3 a 60 caracteres entre letras sem acento, números, `.`, `-` e `_`. |
| `SGI_COORDENADOR_SENHA` | sim | Senha inicial, com pelo menos 12 caracteres. Precisa ser trocada no primeiro acesso. |
| `SGI_COORDENADOR_NOME` | não | Nome exibido. Padrão: `Coordenador`. |

```bash
SGI_COORDENADOR_LOGIN=coordenacao SGI_COORDENADOR_SENHA='troque-esta-senha' ./mvnw spring-boot:run
```

Se já existe alguma conta, as variáveis são ignoradas e podem ser removidas do ambiente.

## Implantar em produção

A produção é uma VPS com Docker Compose: Caddy (TLS automático, frontend e proxy de `/api`), a
aplicação e o Postgres ([ADR 0005](docs/adr/0005-hospedagem-vps-docker-compose.md),
[ADR 0050](docs/adr/0050-empacotamento-e-compose-de-producao.md)). Os segredos ficam num `.env` fora do
repositório. O backup diário vai cifrado com restic para fora da VPS
([ADR 0054](docs/adr/0054-backup-diario-com-restic.md)), e um monitor externo consulta `/saude` a cada 5
minutos ([ADR 0067](docs/adr/0067-monitor-externo-no-github-actions.md)). Resumo:

```bash
cd deploy
cp .env.example .env && chmod 600 .env                 # domínio, banco, chaves e Coordenador
cp backup.env.example backup.env && chmod 600 backup.env   # destino, senha e alertas do backup
docker compose up -d --build --wait
```

O passo a passo completo, com requisitos da VPS, atualização, troca de chave e problemas comuns, está
em [docs/implantacao.md](docs/implantacao.md). Para restaurar numa VPS nova a partir do backup, siga
[docs/recuperacao-de-desastres.md](docs/recuperacao-de-desastres.md).
