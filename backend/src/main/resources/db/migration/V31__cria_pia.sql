-- Plano de Atendimento Individual e Familiar, o PIA (RF006, issue #37, ADR 0103).
-- Cada linha é uma versão do PIA de uma pessoa. O conteúdo é um JSON cifrado na aplicação
-- (TextoCifradoConverter), então a estrutura do plano muda sem migração (pergunta 5, ADR 0103).

CREATE TABLE pia (
    id                  UUID         PRIMARY KEY,
    pessoa_id           UUID         NOT NULL REFERENCES pessoa (id),
    numero_versao       INTEGER      NOT NULL,
    status              VARCHAR(30)  NOT NULL,
    conteudo            TEXT         NOT NULL,
    versao_anterior_id  UUID         REFERENCES pia (id),
    criado_em           TIMESTAMPTZ  NOT NULL,
    criado_por          UUID,
    atualizado_em       TIMESTAMPTZ  NOT NULL,
    atualizado_por      UUID,
    versao              BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_pia_status
        CHECK (status IN ('ELABORADO', 'APRESENTADO_FAMILIA', 'APRESENTADO_EQUIPE', 'APROVADO')),
    CONSTRAINT ck_pia_numero_versao CHECK (numero_versao >= 1),
    CONSTRAINT uk_pia_pessoa_versao UNIQUE (pessoa_id, numero_versao)
);
