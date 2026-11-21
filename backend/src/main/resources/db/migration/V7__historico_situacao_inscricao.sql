-- Saída da lista de espera e histórico das mudanças de situação (RF002, issue #29, ADR 0091).
-- A situação atual continua em inscricao_lista_espera; o histórico guarda cada mudança, com data,
-- motivo e observação (cifrada na aplicação, ADR 0004).

ALTER TABLE inscricao_lista_espera
    ADD COLUMN data_saida   DATE,
    ADD COLUMN motivo_saida VARCHAR(30),
    ADD CONSTRAINT ck_inscricao_motivo_saida
        CHECK (motivo_saida IN ('DESISTENCIA', 'NAO_LOCALIZADO', 'FORA_DO_PERFIL', 'OUTRO')),
    ADD CONSTRAINT ck_inscricao_saida
        CHECK ((situacao = 'SAIU') = (motivo_saida IS NOT NULL AND data_saida IS NOT NULL));

CREATE TABLE historico_situacao_inscricao (
    id                 UUID         PRIMARY KEY,
    inscricao_id       UUID         NOT NULL REFERENCES inscricao_lista_espera (id),
    situacao_anterior  VARCHAR(30),
    situacao_nova      VARCHAR(30)  NOT NULL,
    data               DATE         NOT NULL,
    motivo_saida       VARCHAR(30),
    observacao         TEXT,
    registrado_por     UUID,
    registrado_em      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_historico_motivo_saida
        CHECK (motivo_saida IN ('DESISTENCIA', 'NAO_LOCALIZADO', 'FORA_DO_PERFIL', 'OUTRO'))
);

CREATE INDEX ix_historico_inscricao ON historico_situacao_inscricao (inscricao_id, registrado_em);

COMMENT ON COLUMN historico_situacao_inscricao.observacao IS 'Texto livre cifrado (formato v1 do ADR 0025).';

-- As inscrições que já existem ganham a entrada inicial do histórico: entrada na fila.
INSERT INTO historico_situacao_inscricao (id, inscricao_id, situacao_anterior, situacao_nova, data,
                                          registrado_por, registrado_em)
SELECT gen_random_uuid(), id, NULL, situacao, data_cadastro, criado_por, criado_em
FROM inscricao_lista_espera;
