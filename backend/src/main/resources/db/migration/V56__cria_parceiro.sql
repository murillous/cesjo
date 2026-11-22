-- Parceiros do Centro-Dia e ações de articulação (RF013, issue #53, ADR 0172).
-- PENDENTE-CESJO(P10): campos provisórios do parceiro (ADR 0172).

CREATE TABLE parceiro (
    id                 UUID         PRIMARY KEY,
    nome               VARCHAR(150) NOT NULL,
    tipo               VARCHAR(40)  NOT NULL,
    instrumento        VARCHAR(200),
    vigencia_inicio    DATE,
    vigencia_fim       DATE,
    contato            VARCHAR(500),
    observacoes        VARCHAR(2000),
    ativo              BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em          TIMESTAMPTZ  NOT NULL,
    criado_por         UUID,
    atualizado_em      TIMESTAMPTZ  NOT NULL,
    atualizado_por     UUID,
    versao             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_parceiro_tipo CHECK (tipo IN ('ORGAO_PUBLICO', 'ORGANIZACAO_SOCIEDADE_CIVIL', 'EMPRESA',
        'INSTITUICAO_ENSINO', 'OUTRO')),
    CONSTRAINT ck_parceiro_vigencia CHECK (vigencia_fim IS NULL OR vigencia_inicio IS NULL
        OR vigencia_fim >= vigencia_inicio)
);

CREATE INDEX ix_parceiro_ativo_nome ON parceiro (ativo, nome);

CREATE TABLE articulacao_parceria (
    id              UUID          PRIMARY KEY,
    parceiro_id     UUID          NOT NULL REFERENCES parceiro (id),
    data            DATE          NOT NULL,
    descricao       VARCHAR(2000) NOT NULL,
    criado_em       TIMESTAMPTZ   NOT NULL,
    criado_por      UUID,
    atualizado_em   TIMESTAMPTZ   NOT NULL,
    atualizado_por  UUID,
    versao          BIGINT        NOT NULL DEFAULT 0
);

-- Ações do parceiro da mais recente à mais antiga.
CREATE INDEX ix_articulacao_parceiro_data ON articulacao_parceria (parceiro_id, data DESC);
