#!/usr/bin/env bash
# Sobe o projeto local: instala dependências, faz o build, sobe os contêineres (Postgres e backend) e
# inicia o frontend, tudo de uma vez. Ctrl+C encerra tudo e para os contêineres.
# Pré-requisitos: Docker e Node 24. O backend compila dentro da imagem (backend/Dockerfile, JDK 25).
#
# Uso: scripts/iniciar.sh [--demo]
#   --demo  usa um banco separado (projeto compose cesjo-demo) com a carga de demonstração (ADR 0237).
#           O banco de desenvolvimento não é tocado. Para recomeçar a demo:
#           docker compose -f backend/compose.yaml -p cesjo-demo down -v && docker volume rm cesjo-demo-arquivos
set -euo pipefail

raiz=$(cd "$(dirname "$0")/.." && pwd)

demo=false
for arg in "$@"; do
	case $arg in
		--demo) demo=true ;;
		*) echo "uso: $0 [--demo]" >&2; exit 2 ;;
	esac
done

for cmd in docker npm openssl; do
	command -v "$cmd" >/dev/null || { echo "erro: '$cmd' não encontrado no PATH" >&2; exit 1; }
done

# Dev usa o projeto "backend" (o mesmo nome que o Spring Boot Docker Compose usa), para manter o volume existente.
if $demo; then
	projeto=cesjo-demo outro=backend perfil=demo
else
	projeto=backend outro=cesjo-demo perfil=default
fi
compose=(docker compose -f "$raiz/backend/compose.yaml")
app=cesjo-backend-local

# --- Chave de criptografia local (README, "Chave de criptografia"), a mesma do ./mvnw spring-boot:run ---
config="$raiz/backend/config/application.properties"
if [[ ! -f $config ]]; then
	echo "==> Gerando chave de criptografia local em backend/config/application.properties"
	mkdir -p "$(dirname "$config")"
	printf 'sgi.criptografia.chave-ativa=dev1\nsgi.criptografia.chaves.dev1=%s\n' "$(openssl rand -base64 32)" >"$config"
fi
chave_ativa=$(sed -n 's/^sgi\.criptografia\.chave-ativa=//p' "$config")
chave=$(sed -n "s/^sgi\.criptografia\.chaves\.$chave_ativa=//p" "$config")
[[ -n $chave_ativa && -n $chave ]] || { echo "erro: chave ativa não encontrada em $config" >&2; exit 1; }

# --- Dependências e build ---
echo "==> Backend: build da imagem (dependências Maven e jar, sem testes)"
docker build -q -t "$app" "$raiz/backend" >/dev/null

echo "==> Frontend: dependências"
cd "$raiz/frontend"
if [[ ! -f node_modules/.package-lock.json || package-lock.json -nt node_modules/.package-lock.json ]]; then
	npm ci --no-audit --no-fund
fi

# --- Banco ---
# Os dois projetos publicam a 5432: para o outro antes de subir este.
echo "==> Postgres ($projeto)"
"${compose[@]}" -p "$outro" stop >/dev/null 2>&1 || true
"${compose[@]}" -p "$projeto" up -d
# -h 127.0.0.1: o servidor temporário do initdb só escuta no socket, então só passa quando o banco real subiu.
until "${compose[@]}" -p "$projeto" exec -T postgres pg_isready -q -h 127.0.0.1 -U cesjo -d cesjo; do sleep 1; done

encerrar() {
	trap - INT TERM EXIT
	echo "==> Encerrando"
	docker stop "$app" >/dev/null 2>&1 || true
	kill $(jobs -p) 2>/dev/null || true
	wait 2>/dev/null || true
	"${compose[@]}" -p "$projeto" stop
}
trap encerrar INT TERM EXIT

# --- Backend e frontend em paralelo ---
echo "==> Iniciando backend (:8080, perfil $perfil) e frontend (:4200)"
docker rm -f "$app" >/dev/null 2>&1 || true
# A chave vai por variável de ambiente (ADR 0052); identificador em maiúsculas no nome da variável.
docker run --rm --name "$app" \
	--network "${projeto}_default" -p 8080:8080 \
	-v "${projeto}-arquivos:/var/lib/sgi/arquivos" \
	-e SPRING_PROFILES_ACTIVE="$perfil" \
	-e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/cesjo \
	-e SPRING_DATASOURCE_USERNAME=cesjo -e SPRING_DATASOURCE_PASSWORD=cesjo \
	-e SPRING_DOCKER_COMPOSE_ENABLED=false \
	-e SGI_CRIPTOGRAFIA_CHAVEATIVA="$chave_ativa" \
	-e "SGI_CRIPTOGRAFIA_CHAVES_${chave_ativa^^}=$chave" \
	-e SGI_COORDENADOR_LOGIN -e SGI_COORDENADOR_SENHA -e SGI_COORDENADOR_NOME \
	"$app" &
# Em segundo plano o ng serve não tem terminal: a pergunta de analytics do CLI aborta a subida.
NG_CLI_ANALYTICS=false npm start &

if $demo; then
	echo "==> Demo: entre em http://localhost:4200 com 'coordenador' / 'demonstracao' (a carga roda na primeira subida)"
fi

# Se um dos dois cair, derruba o outro.
wait -n
