# Versão Windows do scripts/iniciar.sh: instala dependências, faz o build, sobe os contêineres
# (Postgres e backend) e inicia o frontend. Ctrl+C encerra tudo e para os contêineres.
# Pré-requisitos: Docker Desktop e Node 24. O backend compila dentro da imagem (backend/Dockerfile, JDK 25).
#
# Uso (PowerShell, na raiz do repositório):
#   powershell -ExecutionPolicy Bypass -File scripts\iniciar.ps1 [--demo]
#   --demo  usa um banco separado (projeto compose cesjo-demo) com a carga de demonstração (ADR 0237).
#           O banco de desenvolvimento não é tocado. Para recomeçar a demo:
#           docker compose -f backend/compose.yaml -p cesjo-demo down -v; docker volume rm cesjo-demo-arquivos
#
# Salvo em UTF-8 com BOM: sem ele o Windows PowerShell 5.1 lê os acentos errado.
# Sem $ErrorActionPreference = 'Stop': no PowerShell 5.1 ele transforma em exceção o stderr do docker redirecionado.
function Falhar([string]$mensagem) {
	Write-Host "erro: $mensagem" -ForegroundColor Red
	exit 1
}

$demo = $false
foreach ($arg in $args) {
	if ($arg -in '--demo', '-demo') { $demo = $true }
	else { Falhar "uso: iniciar.ps1 [--demo]" }
}

foreach ($cmd in 'docker', 'npm') {
	if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) { Falhar "'$cmd' não encontrado no PATH" }
}

# Para no primeiro comando externo que falhar.
function Exigir([string]$etapa) {
	if ($LASTEXITCODE -ne 0) { Falhar "$etapa falhou (código $LASTEXITCODE)" }
}

$raiz = Split-Path -Parent $PSScriptRoot
# Dev usa o projeto "backend" (o mesmo nome que o Spring Boot Docker Compose usa), para manter o volume existente.
if ($demo) { $projeto = 'cesjo-demo'; $outro = 'backend'; $perfil = 'demo' }
else { $projeto = 'backend'; $outro = 'cesjo-demo'; $perfil = 'default' }
$composeArquivo = Join-Path $raiz 'backend/compose.yaml'
$app = 'cesjo-backend-local'

# --- Chave de criptografia local (README, "Chave de criptografia"), a mesma do ./mvnw spring-boot:run ---
$config = Join-Path $raiz 'backend/config/application.properties'
if (-not (Test-Path $config)) {
	Write-Host '==> Gerando chave de criptografia local em backend/config/application.properties'
	New-Item -ItemType Directory -Force -Path (Split-Path $config) | Out-Null
	$bytes = New-Object byte[] 32
	[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
	$conteudo = "sgi.criptografia.chave-ativa=dev1`nsgi.criptografia.chaves.dev1=$([Convert]::ToBase64String($bytes))`n"
	# Sem BOM: o Spring Boot leria o BOM como parte da primeira chave.
	[IO.File]::WriteAllText($config, $conteudo, (New-Object Text.UTF8Encoding($false)))
}
$propriedades = Get-Content $config
$chaveAtiva = ($propriedades | Where-Object { $_ -match '^sgi\.criptografia\.chave-ativa=(.+)$' } | ForEach-Object { $Matches[1].Trim() }) | Select-Object -First 1
$chave = ($propriedades | Where-Object { $_ -match "^sgi\.criptografia\.chaves\.$([regex]::Escape("$chaveAtiva"))=(.+)$" } | ForEach-Object { $Matches[1].Trim() }) | Select-Object -First 1
if (-not $chaveAtiva -or -not $chave) { Falhar "chave ativa não encontrada em $config" }

# --- Dependências e build ---
Write-Host '==> Backend: build da imagem (dependências Maven e jar, sem testes)'
docker build -q -t $app (Join-Path $raiz 'backend') | Out-Null
Exigir 'docker build'

Write-Host '==> Frontend: dependências'
Set-Location (Join-Path $raiz 'frontend')
$marca = 'node_modules/.package-lock.json'
if (-not (Test-Path $marca) -or (Get-Item 'package-lock.json').LastWriteTime -gt (Get-Item $marca).LastWriteTime) {
	npm ci --no-audit --no-fund
	Exigir 'npm ci'
}

# --- Banco ---
# Os dois projetos publicam a 5432: para o outro antes de subir este.
Write-Host "==> Postgres ($projeto)"
docker compose -f $composeArquivo -p $outro stop 2>&1 | Out-Null
docker compose -f $composeArquivo -p $projeto up -d
Exigir 'docker compose up'
# -h 127.0.0.1: o servidor temporário do initdb só escuta no socket, então só passa quando o banco real subiu.
do {
	Start-Sleep -Seconds 1
	docker compose -f $composeArquivo -p $projeto exec -T postgres pg_isready -q -h 127.0.0.1 -U cesjo -d cesjo
} while ($LASTEXITCODE -ne 0)

try {
	# --- Backend em contêiner (logs no mesmo console) e frontend em primeiro plano ---
	Write-Host "==> Iniciando backend (:8080, perfil $perfil) e frontend (:4200)"
	docker rm -f $app 2>&1 | Out-Null
	# A chave vai por variável de ambiente (ADR 0052); identificador em maiúsculas no nome da variável.
	docker run -d --rm --name $app `
		--network "${projeto}_default" -p 8080:8080 `
		-v "${projeto}-arquivos:/var/lib/sgi/arquivos" `
		-e "SPRING_PROFILES_ACTIVE=$perfil" `
		-e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/cesjo `
		-e SPRING_DATASOURCE_USERNAME=cesjo -e SPRING_DATASOURCE_PASSWORD=cesjo `
		-e SPRING_DOCKER_COMPOSE_ENABLED=false `
		-e "SGI_CRIPTOGRAFIA_CHAVEATIVA=$chaveAtiva" `
		-e "SGI_CRIPTOGRAFIA_CHAVES_$($chaveAtiva.ToUpper())=$chave" `
		-e SGI_COORDENADOR_LOGIN -e SGI_COORDENADOR_SENHA -e SGI_COORDENADOR_NOME `
		$app | Out-Null
	Exigir 'docker run'
	Start-Process docker -ArgumentList 'logs', '-f', $app -NoNewWindow

	if ($demo) {
		Write-Host "==> Demo: entre em http://localhost:4200 com 'coordenador' / 'demonstracao' (a carga roda na primeira subida)"
	}
	# shortcut: diferente do iniciar.sh, se o backend cair o frontend continua; os logs mostram a queda.
	npm start
}
finally {
	Write-Host '==> Encerrando'
	docker stop $app 2>&1 | Out-Null
	docker compose -f $composeArquivo -p $projeto stop
}
