-- Contas de acesso do sistema (issues #13 e #15, ADR 0003 e ADR 0010).
create table conta_acesso (
    id                      uuid         primary key,
    login                   varchar(60)  not null,
    nome                    varchar(150) not null,
    senha_hash              varchar(255) not null,
    perfil                  varchar(30)  not null,
    ativa                   boolean      not null default true,
    troca_senha_obrigatoria boolean      not null default true,
    tentativas_falhas       integer      not null default 0,
    bloqueada_ate           timestamptz,
    criado_em               timestamptz  not null,
    criado_por              uuid,
    atualizado_em           timestamptz  not null,
    atualizado_por          uuid,
    versao                  bigint       not null default 0,
    constraint conta_acesso_login_uk unique (login),
    -- O login é guardado sempre em minúsculas, então a unicidade não depende de maiúsculas.
    constraint conta_acesso_login_minusculo_ck check (login = lower(login)),
    constraint conta_acesso_perfil_ck check (perfil in ('COORDENADOR', 'EQUIPE_TECNICA', 'ADMINISTRATIVO', 'CONSULTA')),
    constraint conta_acesso_tentativas_ck check (tentativas_falhas >= 0)
);

-- A regra da última conta ativa de Coordenador consulta por perfil e situação.
create index conta_acesso_perfil_ativa_idx on conta_acesso (perfil, ativa);
