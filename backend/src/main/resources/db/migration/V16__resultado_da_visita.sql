-- Realização, cancelamento e reagendamento da visita domiciliar (RF004, issue #35, ADR 0073).
-- As observações são cifradas na aplicação (TextoCifradoConverter, formato v1:<id>:<Base64>), por isso
-- a coluna é text e não pode ser pesquisada.

ALTER TABLE visita_domiciliar
    ADD COLUMN motivo_cancelamento  VARCHAR(500),
    ADD COLUMN observacoes          TEXT,
    ADD COLUMN visita_origem_id     UUID REFERENCES visita_domiciliar (id),
    ADD CONSTRAINT ck_visita_cancelada_com_motivo
        CHECK (status <> 'CANCELADA' OR motivo_cancelamento IS NOT NULL),
    -- Uma visita cancelada é reagendada no máximo uma vez.
    ADD CONSTRAINT uk_visita_origem UNIQUE (visita_origem_id);
