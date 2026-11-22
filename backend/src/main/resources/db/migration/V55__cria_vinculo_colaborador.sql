-- Histórico de vínculos dos colaboradores: contratações e desligamentos (RF014, issue #51, ADR 0170).
-- Cada contratação abre um vínculo; o desligamento fecha o vínculo aberto com data e motivo. O motivo
-- é cifrado na aplicação (formato v1 do ADR 0025), por isso é text.

CREATE TABLE vinculo_colaborador (
    id                  UUID         PRIMARY KEY,
    colaborador_id      UUID         NOT NULL REFERENCES colaborador (id),
    data_contratacao    DATE         NOT NULL,
    data_desligamento   DATE,
    motivo_desligamento TEXT,
    criado_em           TIMESTAMPTZ  NOT NULL,
    criado_por          UUID,
    atualizado_em       TIMESTAMPTZ  NOT NULL,
    atualizado_por      UUID,
    versao              BIGINT       NOT NULL DEFAULT 0,
    -- Desligamento tem data e motivo juntos, e não vem antes da contratação.
    CONSTRAINT ck_vinculo_desligamento CHECK (
        (data_desligamento IS NULL AND motivo_desligamento IS NULL)
        OR (data_desligamento IS NOT NULL AND motivo_desligamento IS NOT NULL
            AND data_desligamento >= data_contratacao))
);

-- No máximo um vínculo aberto por colaborador.
CREATE UNIQUE INDEX uk_vinculo_colaborador_aberto ON vinculo_colaborador (colaborador_id)
    WHERE data_desligamento IS NULL;

-- Histórico do colaborador em ordem de contratação.
CREATE INDEX ix_vinculo_colaborador ON vinculo_colaborador (colaborador_id, data_contratacao DESC);
