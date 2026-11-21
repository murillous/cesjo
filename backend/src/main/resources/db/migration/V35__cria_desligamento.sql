-- Desligamento do usuário atendido (RF008, issue #42, ADR 0110).
-- Um desligamento por inserção; a inserção passa a DESLIGADA na mesma transação. A observação é
-- cifrada na aplicação (formato v1 do ADR 0025), por isso é text.

CREATE TABLE desligamento (
    id              UUID         PRIMARY KEY,
    insercao_id     UUID         NOT NULL REFERENCES insercao (id),
    data            DATE         NOT NULL,
    motivo          VARCHAR(30)  NOT NULL,
    observacao      TEXT,
    criado_em       TIMESTAMPTZ  NOT NULL,
    criado_por      UUID,
    atualizado_em   TIMESTAMPTZ  NOT NULL,
    atualizado_por  UUID,
    versao          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_desligamento_motivo CHECK (motivo IN ('EVASAO', 'MUDANCA_MUNICIPIO', 'SOLICITACAO_FAMILIA',
        'ALCANCE_OBJETIVOS', 'OUTROS')),
    CONSTRAINT uq_desligamento_insercao UNIQUE (insercao_id)
);

-- Relatórios de desligamento por motivo e período (RF016, RF017).
CREATE INDEX ix_desligamento_data ON desligamento (data, motivo);
