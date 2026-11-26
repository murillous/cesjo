#!/usr/bin/env python3
"""Gerencia o ambiente local em segundo plano: Postgres e backend em contêineres, frontend com ng serve.

Uso: python3 scripts/dev.py <comando> [opções]
  start [--demo]            sobe Postgres, backend (contêiner, :8080) e frontend (ng serve, :4200)
  stop                      para backend, frontend e o Postgres
  restart [--demo]          stop + start
  status                    mostra o que está no ar
  logs [backend|frontend]   acompanha os logs (Ctrl+C sai)
  clean [--demo]            stop e APAGA o banco e os arquivos enviados do projeto compose (pede confirmação)
  purge                     remove artefatos de build (backend/target, frontend/dist, frontend/.angular) e logs
"""
import base64
import json
import os
import shutil
import signal
import socket
import subprocess
import sys
import threading
import time

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BACKEND = os.path.join(RAIZ, "backend")
FRONTEND = os.path.join(RAIZ, "frontend")
LOGS = os.path.join(RAIZ, "logs")
ESTADO = os.path.join(LOGS, "dev_state.json")
COMPOSE = ["docker", "compose", "-f", os.path.join(BACKEND, "compose.yaml")]
CONFIG = os.path.join(BACKEND, "config", "application.properties")
APP = "cesjo-backend-local"

WINDOWS = os.name == "nt"
# No Windows o npm é um .cmd: o CreateProcess só o encontra pelo caminho completo.
NPM = shutil.which("npm") or "npm"
if WINDOWS:
    os.system("")  # liga as cores ANSI no console do Windows

VERDE, AZUL, AMARELO, VERMELHO, NEGRITO, FIM = "\033[92m", "\033[94m", "\033[93m", "\033[91m", "\033[1m", "\033[0m"


def info(msg):
    print(f"{AZUL}==>{FIM} {msg}")


def ok(msg):
    print(f"{VERDE}ok{FIM}  {msg}")


def aviso(msg):
    print(f"{AMARELO}aviso{FIM} {msg}")


def erro(msg):
    print(f"{VERMELHO}erro{FIM} {msg}", file=sys.stderr)
    sys.exit(1)


def projetos(demo):
    # Dev usa o projeto "backend" (o mesmo nome que o Spring Boot Docker Compose usa), para manter o volume existente.
    return ("cesjo-demo", "backend", "demo") if demo else ("backend", "cesjo-demo", "default")


def carregar_estado():
    try:
        with open(ESTADO) as f:
            return json.load(f)
    except (OSError, ValueError):
        return {}


def salvar_estado(estado):
    os.makedirs(LOGS, exist_ok=True)
    with open(ESTADO, "w") as f:
        json.dump(estado, f, indent=2)


def vivo(pid):
    if not pid:
        return False
    if WINDOWS:
        # No Windows, os.kill(pid, 0) encerra o processo em vez de só testar.
        r = subprocess.run(["tasklist", "/FI", f"PID eq {pid}", "/NH"], capture_output=True, text=True)
        return str(pid) in r.stdout
    try:
        os.kill(pid, 0)
        return True
    except OSError:
        return False


def porta_em_uso(porta):
    with socket.socket() as s:
        s.settimeout(0.3)
        return s.connect_ex(("127.0.0.1", porta)) == 0


def backend_no_ar():
    r = subprocess.run(["docker", "ps", "-q", "-f", f"name=^{APP}$"], capture_output=True, text=True)
    return bool(r.stdout.strip())


def chave_criptografia():
    """Lê (ou gera) a chave local, a mesma do ./mvnw spring-boot:run (README, "Chave de criptografia")."""
    if not os.path.isfile(CONFIG):
        info("Gerando chave de criptografia local em backend/config/application.properties")
        os.makedirs(os.path.dirname(CONFIG), exist_ok=True)
        chave = base64.b64encode(os.urandom(32)).decode()
        with open(CONFIG, "w") as f:
            f.write(f"sgi.criptografia.chave-ativa=dev1\nsgi.criptografia.chaves.dev1={chave}\n")
    props = {}
    with open(CONFIG) as f:
        for linha in f:
            k, sep, v = linha.strip().partition("=")
            if sep:
                props[k] = v
    ativa = props.get("sgi.criptografia.chave-ativa")
    chave = props.get(f"sgi.criptografia.chaves.{ativa}")
    if not ativa or not chave:
        erro(f"chave ativa não encontrada em {CONFIG}")
    return ativa, chave


def iniciar_processo(cmd, log, cwd=RAIZ, env=None):
    # Processo destacado: sobrevive ao terminal e o stop mata a árvore inteira (npm -> ng).
    if WINDOWS:
        destacar = {"creationflags": subprocess.CREATE_NEW_PROCESS_GROUP | subprocess.CREATE_NO_WINDOW}
    else:
        destacar = {"start_new_session": True}
    with open(log, "w") as f:
        return subprocess.Popen(cmd, cwd=cwd, env=env, stdout=f, stderr=subprocess.STDOUT,
                                stdin=subprocess.DEVNULL, **destacar).pid


def start(demo):
    for cmd in ("docker", "npm"):
        if not shutil.which(cmd):
            erro(f"'{cmd}' não encontrado no PATH")

    estado = carregar_estado()
    if backend_no_ar() or vivo(estado.get("frontend_pid")):
        aviso("Já há serviços no ar. Use 'stop' ou 'restart'.")
        status()
        return

    projeto, outro, perfil = projetos(demo)
    ativa, chave = chave_criptografia()
    os.makedirs(LOGS, exist_ok=True)

    info("Backend: build da imagem (dependências Maven e jar, sem testes)")
    if subprocess.run(["docker", "build", "-q", "-t", APP, BACKEND], stdout=subprocess.DEVNULL).returncode:
        erro("falha no build da imagem do backend")

    info("Frontend: dependências")
    lock, instalado = os.path.join(FRONTEND, "package-lock.json"), os.path.join(FRONTEND, "node_modules", ".package-lock.json")
    if not os.path.isfile(instalado) or os.path.getmtime(lock) > os.path.getmtime(instalado):
        if subprocess.run([NPM, "ci", "--no-audit", "--no-fund"], cwd=FRONTEND).returncode:
            erro("npm ci falhou")

    # Os dois projetos publicam a 5432: para o outro antes de subir este.
    info(f"Postgres ({projeto})")
    subprocess.run(COMPOSE + ["-p", outro, "stop"], capture_output=True)
    if subprocess.run(COMPOSE + ["-p", projeto, "up", "-d"]).returncode:
        erro("falha ao subir o Postgres")
    # -h 127.0.0.1: o servidor temporário do initdb só escuta no socket, então só passa quando o banco real subiu.
    while subprocess.run(COMPOSE + ["-p", projeto, "exec", "-T", "postgres", "pg_isready", "-q",
                                    "-h", "127.0.0.1", "-U", "cesjo", "-d", "cesjo"]).returncode:
        time.sleep(1)

    if porta_em_uso(8080):
        erro("porta 8080 em uso: libere antes de subir o backend")
    if porta_em_uso(4200):
        erro("porta 4200 em uso: libere antes de subir o frontend")

    info(f"Backend (:8080, perfil {perfil}) -> logs/backend.log")
    subprocess.run(["docker", "rm", "-f", APP], capture_output=True)
    # A chave vai por variável de ambiente (ADR 0052); identificador em maiúsculas no nome da variável.
    backend_cmd = [
        "docker", "run", "--rm", "--name", APP,
        "--network", f"{projeto}_default", "-p", "8080:8080",
        "-v", f"{projeto}-arquivos:/var/lib/sgi/arquivos",
        "-e", f"SPRING_PROFILES_ACTIVE={perfil}",
        "-e", "SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/cesjo",
        "-e", "SPRING_DATASOURCE_USERNAME=cesjo", "-e", "SPRING_DATASOURCE_PASSWORD=cesjo",
        "-e", "SPRING_DOCKER_COMPOSE_ENABLED=false",
        "-e", f"SGI_CRIPTOGRAFIA_CHAVEATIVA={ativa}",
        "-e", f"SGI_CRIPTOGRAFIA_CHAVES_{ativa.upper()}={chave}",
        "-e", "SGI_COORDENADOR_LOGIN", "-e", "SGI_COORDENADOR_SENHA", "-e", "SGI_COORDENADOR_NOME",
        APP,
    ]
    backend_pid = iniciar_processo(backend_cmd, os.path.join(LOGS, "backend.log"))

    info("Frontend (:4200) -> logs/frontend.log")
    # Em segundo plano o ng serve não tem terminal: a pergunta de analytics do CLI aborta a subida.
    frontend_pid = iniciar_processo([NPM, "start"], os.path.join(LOGS, "frontend.log"), cwd=FRONTEND,
                                    env=dict(os.environ, NG_CLI_ANALYTICS="false"))

    salvar_estado({"projeto": projeto, "perfil": perfil, "backend_pid": backend_pid, "frontend_pid": frontend_pid})
    ok("Serviços iniciados. Acompanhe com: python3 scripts/dev.py logs")
    if demo:
        info("Demo: entre em http://localhost:4200 com 'coordenador' / 'demonstracao' (a carga roda na primeira subida)")


def matar_grupo(pid, nome):
    if not vivo(pid):
        return
    info(f"Parando {nome} (grupo {pid})")
    if WINDOWS:
        subprocess.run(["taskkill", "/PID", str(pid), "/T", "/F"], capture_output=True)
        return
    try:
        os.killpg(pid, signal.SIGTERM)
        for _ in range(20):
            time.sleep(0.5)
            if not vivo(pid):
                return
        aviso(f"{nome} não parou com SIGTERM, usando SIGKILL")
        os.killpg(pid, signal.SIGKILL)
    except ProcessLookupError:
        pass


def stop():
    estado = carregar_estado()
    if backend_no_ar():
        info("Parando backend")
        subprocess.run(["docker", "stop", APP], capture_output=True)
    matar_grupo(estado.get("backend_pid"), "backend (docker run)")
    matar_grupo(estado.get("frontend_pid"), "frontend")
    for projeto in ("backend", "cesjo-demo"):
        subprocess.run(COMPOSE + ["-p", projeto, "stop"], capture_output=True)
    salvar_estado({})
    ok("Tudo parado.")


def status():
    estado = carregar_estado()
    def linha(nome, no_ar, extra=""):
        print(f"  {nome:<22} {VERDE + 'NO AR' if no_ar else VERMELHO + 'PARADO'}{FIM} {extra if no_ar else ''}")
    print(f"\n{NEGRITO}Ambiente local CESJO{FIM} (perfil: {estado.get('perfil', '-')})")
    linha("Postgres (:5432)", porta_em_uso(5432), f"projeto {estado.get('projeto', '?')}")
    linha("Backend (:8080)", backend_no_ar(), f"contêiner {APP}")
    linha("Frontend (:4200)", vivo(estado.get("frontend_pid")), f"PID {estado.get('frontend_pid')}")
    print(f"  Logs: {os.path.join(LOGS, 'backend.log')}, {os.path.join(LOGS, 'frontend.log')}\n")


def seguir(caminho, prefixo, cor, ultimas):
    if not os.path.exists(caminho):
        open(caminho, "a").close()
    with open(caminho, encoding="utf-8", errors="replace") as f:
        for linha in f.readlines()[-ultimas:]:
            sys.stdout.write(f"{cor}{prefixo}{FIM} {linha}")
        while True:
            if os.path.getsize(caminho) < f.tell():  # arquivo recriado por um novo start
                f.seek(0)
            linha = f.readline()
            if linha:
                sys.stdout.write(f"{cor}{prefixo}{FIM} {linha}")
                sys.stdout.flush()
            else:
                time.sleep(0.1)


def logs(servico):
    os.makedirs(LOGS, exist_ok=True)
    alvos = {"backend": VERDE, "frontend": AZUL}
    if servico and servico not in alvos:
        erro(f"serviço desconhecido: {servico} (use backend ou frontend)")
    escolhidos = [servico] if servico else list(alvos)
    for s in escolhidos:
        threading.Thread(target=seguir, args=(os.path.join(LOGS, f"{s}.log"), f"[{s}]", alvos[s], 30 // len(escolhidos)),
                         daemon=True).start()
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print()


def clean(demo):
    projeto = projetos(demo)[0]
    resposta = input(f"Isto APAGA o banco e os arquivos enviados do projeto '{projeto}'. Digite '{projeto}' para confirmar: ")
    if resposta.strip() != projeto:
        erro("cancelado")
    stop()
    subprocess.run(COMPOSE + ["-p", projeto, "down", "-v"])
    subprocess.run(["docker", "volume", "rm", f"{projeto}-arquivos"], capture_output=True)
    ok(f"Banco e arquivos do projeto '{projeto}' removidos.")


def purge():
    estado = carregar_estado()
    if backend_no_ar() or vivo(estado.get("frontend_pid")):
        stop()
    for alvo in ("backend/target", "frontend/dist", "frontend/.angular", "logs"):
        caminho = os.path.join(RAIZ, alvo)
        if os.path.exists(caminho):
            shutil.rmtree(caminho)
            ok(f"Removido {alvo}")
    ok("Purge concluído. Banco e volumes intactos.")


if __name__ == "__main__":
    args = sys.argv[1:]
    cmd = args[0].lower() if args else ""
    demo = "--demo" in args
    if cmd == "start":
        start(demo)
    elif cmd == "stop":
        stop()
    elif cmd == "restart":
        stop()
        start(demo)
    elif cmd == "status":
        status()
    elif cmd in ("log", "logs"):
        logs(args[1].lower() if len(args) > 1 else None)
    elif cmd == "clean":
        clean(demo)
    elif cmd == "purge":
        purge()
    else:
        print(__doc__)
        sys.exit(0 if cmd in ("", "help", "-h", "--help") else 2)
