-- Metadados dos arquivos enviados (issue #18, ADR 0026). O conteúdo fica cifrado no diretório
-- sgi.arquivos.diretorio, nunca no banco.
create table arquivo (
    id                    uuid         primary key,
    nome_original         text         not null,
    tipo_mime             varchar(50)  not null,
    tamanho               bigint       not null,
    sha256                varchar(64)  not null,
    caminho_armazenamento varchar(255) not null,
    chave_id              varchar(32)  not null,
    enviado_por           uuid,
    enviado_em            timestamp with time zone not null,
    constraint arquivo_tipo_mime_aceito check (tipo_mime in ('application/pdf', 'image/jpeg', 'image/png')),
    constraint arquivo_tamanho_positivo check (tamanho > 0),
    constraint arquivo_sha256_hex check (sha256 ~ '^[0-9a-f]{64}$'),
    constraint arquivo_caminho_unico unique (caminho_armazenamento)
);

comment on table arquivo is 'Metadados dos arquivos cifrados guardados no diretório de arquivos (ADR 0026).';
comment on column arquivo.nome_original is 'Nome enviado, normalizado e cifrado (formato v1 do ADR 0025).';
comment on column arquivo.tamanho is 'Tamanho do conteúdo em claro, em bytes.';
comment on column arquivo.sha256 is 'SHA-256 do conteúdo em claro, em hexadecimal minúsculo.';
comment on column arquivo.caminho_armazenamento is 'Caminho relativo a sgi.arquivos.diretorio.';
comment on column arquivo.chave_id is 'Identificador da chave que cifrou o conteúdo.';
comment on column arquivo.enviado_por is 'Conta que enviou; nulo quando o envio não veio de requisição autenticada.';
