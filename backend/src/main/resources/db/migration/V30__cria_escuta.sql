-- Escuta ativa e qualificada (RF005, issue #36, ADR 0100).
-- Os quatro textos são cifrados na aplicação (TextoCifradoConverter), por isso são text e não podem
-- ser pesquisados. A escuta guarda só o pessoa_id e o colaborador_id do profissional responsável.

CREATE TABLE escuta (
    id                      UUID         PRIMARY KEY,
    pessoa_id               UUID         NOT NULL REFERENCES pessoa (id),
    data_escuta             DATE         NOT NULL,
    colaborador_id          UUID         NOT NULL REFERENCES colaborador (id),
    dinamica_familiar       TEXT,
    necessidades_imediatas  TEXT,
    habilidades             TEXT,
    potencialidades         TEXT,
    criado_em               TIMESTAMPTZ  NOT NULL,
    criado_por              UUID,
    atualizado_em           TIMESTAMPTZ  NOT NULL,
    atualizado_por          UUID,
    versao                  BIGINT       NOT NULL DEFAULT 0
);

-- Escutas de uma pessoa, da mais recente para a mais antiga.
CREATE INDEX ix_escuta_pessoa ON escuta (pessoa_id, data_escuta DESC);
