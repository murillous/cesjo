# ADR 0004 — Criptografia AES-256-GCM na aplicação para texto livre sensível e arquivos

**Status:** Aceito (2026-11-20) · Registra a linha "Criptografia" da seção 1 do [planejamento](../planejamento.md)

## Contexto

O sistema guarda dados pessoais sensíveis (saúde, religião, violações de direitos, relatos de
escuta). O RNF 3.3 pede que fiquem protegidos mesmo se o banco ou o backup vazarem. Relatórios e
dashboards precisam agregar campos categóricos direto em SQL.

Alternativas consideradas: só disco cifrado; `pgcrypto` no banco; cifrar todos os campos.

## Decisão

Campos de texto livre sensíveis e todos os arquivos enviados são cifrados na aplicação com
AES-256-GCM. A chave mestra vem de segredo do ambiente, e cada valor cifrado leva o identificador
da chave para permitir rotação. Campos categóricos ficam em claro, protegidos por perfil, auditoria
de leitura e disco cifrado. A aplicação não sobe sem chave configurada.

Nos testes, a chave fica em `src/test/resources/config/application.properties`
(`sgi.criptografia.chave-ativa` e `sgi.criptografia.chaves.<id>`), nunca usada fora deles.

## Consequências

- Campo cifrado não é pesquisável nem ordenável; a busca usa campos em claro.
- Perder a chave torna o backup inútil, então ela tem cópia guardada fora da VPS.
- O desenho exato do conversor e do formato do valor cifrado fica no ADR da issue #17.
