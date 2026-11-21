-- Violações de direitos identificadas (RF009, issue #43, ADR 0112).
-- A descrição é cifrada na aplicação (formato v1 do ADR 0025). O âmbito só existe nas categorias que
-- o RF009 divide em intra e extrafamiliar.

CREATE TABLE violacao_direito (
    id                  UUID         PRIMARY KEY,
    pessoa_id           UUID         NOT NULL REFERENCES pessoa (id),
    tipo                VARCHAR(40)  NOT NULL,
    ambito              VARCHAR(20),
    data_identificacao  DATE         NOT NULL,
    descricao           TEXT         NOT NULL,
    criado_em           TIMESTAMPTZ  NOT NULL,
    criado_por          UUID,
    atualizado_em       TIMESTAMPTZ  NOT NULL,
    atualizado_por      UUID,
    versao              BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_violacao_tipo CHECK (tipo IN ('EXPLORACAO_IMAGEM', 'ISOLAMENTO_SOCIAL', 'CONFINAMENTO',
        'DISCRIMINACAO_FAMILIAR', 'FALTA_CUIDADOS_CUIDADOR', 'ESTRESSE_CUIDADOR', 'DESVALORIZACAO_CAPACIDADE',
        'VIOLENCIA_PATRIMONIAL', 'NEGLIGENCIA_ABANDONO', 'ABUSO_SEXUAL', 'VIOLENCIA_FISICA_PSICOLOGICA', 'OUTRAS')),
    CONSTRAINT ck_violacao_ambito CHECK (
        (tipo IN ('ABUSO_SEXUAL', 'VIOLENCIA_FISICA_PSICOLOGICA') AND ambito IN ('INTRAFAMILIAR', 'EXTRAFAMILIAR'))
        OR (tipo NOT IN ('ABUSO_SEXUAL', 'VIOLENCIA_FISICA_PSICOLOGICA') AND ambito IS NULL))
);

-- Consulta por usuário e geral, por categoria e período (issue #44).
CREATE INDEX ix_violacao_pessoa ON violacao_direito (pessoa_id, data_identificacao DESC);
CREATE INDEX ix_violacao_tipo_data ON violacao_direito (tipo, data_identificacao DESC);
