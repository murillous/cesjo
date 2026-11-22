-- Doações recebidas e realizadas (RF015, issue #54, ADR 0178).
-- PENDENTE-CESJO(P11): valor só para doação em dinheiro, opcional, sem recibo (ADR 0178).

CREATE TABLE doacao (
    id              UUID           PRIMARY KEY,
    direcao         VARCHAR(20)    NOT NULL,
    data            DATE           NOT NULL,
    contraparte     VARCHAR(200)   NOT NULL,
    tipo            VARCHAR(30)    NOT NULL,
    descricao       VARCHAR(2000)  NOT NULL,
    quantidade      VARCHAR(100),
    valor           NUMERIC(12, 2),
    criado_em       TIMESTAMPTZ    NOT NULL,
    criado_por      UUID,
    atualizado_em   TIMESTAMPTZ    NOT NULL,
    atualizado_por  UUID,
    versao          BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT ck_doacao_direcao CHECK (direcao IN ('RECEBIDA', 'REALIZADA')),
    CONSTRAINT ck_doacao_tipo CHECK (tipo IN ('DINHEIRO', 'ALIMENTOS', 'VESTUARIO', 'HIGIENE_LIMPEZA',
        'MATERIAL_PEDAGOGICO', 'MOVEIS_EQUIPAMENTOS', 'OUTRO')),
    CONSTRAINT ck_doacao_valor CHECK (valor IS NULL OR (tipo = 'DINHEIRO' AND valor > 0))
);

-- Listagem por período e direção.
CREATE INDEX ix_doacao_data ON doacao (data DESC);
CREATE INDEX ix_doacao_direcao_data ON doacao (direcao, data DESC);
