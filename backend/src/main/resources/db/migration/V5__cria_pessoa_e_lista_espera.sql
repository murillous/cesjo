-- Pessoa, responsável e inscrição na lista de espera (RF001, issue #26, ADR 0037).
-- A pessoa e o responsável são do módulo Pessoas; a inscrição é do módulo Lista de espera e guarda
-- só o pessoa_id. A posição na fila não tem coluna: sai da ordem de data_cadastro entre as inscrições
-- aguardando.

CREATE TABLE pessoa (
    id                UUID         PRIMARY KEY,
    nome_completo     VARCHAR(150) NOT NULL,
    data_nascimento   DATE,
    tipo_deficiencia  VARCHAR(20)  NOT NULL,
    telefone          VARCHAR(11)  NOT NULL,
    criado_em         TIMESTAMPTZ  NOT NULL,
    criado_por        UUID,
    atualizado_em     TIMESTAMPTZ  NOT NULL,
    atualizado_por    UUID,
    versao            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_pessoa_tipo_deficiencia
        CHECK (tipo_deficiencia IN ('FISICA', 'SENSORIAL', 'INTELECTUAL', 'MULTIPLA', 'TEA', 'T21')),
    CONSTRAINT ck_pessoa_telefone CHECK (telefone ~ '^[0-9]{10,11}$')
);

CREATE INDEX ix_pessoa_nome ON pessoa (lower(nome_completo));

CREATE TABLE responsavel (
    id              UUID         PRIMARY KEY,
    pessoa_id       UUID         NOT NULL REFERENCES pessoa (id),
    nome_completo   VARCHAR(150) NOT NULL,
    criado_em       TIMESTAMPTZ  NOT NULL,
    criado_por      UUID,
    atualizado_em   TIMESTAMPTZ  NOT NULL,
    atualizado_por  UUID,
    versao          BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX ix_responsavel_pessoa ON responsavel (pessoa_id, criado_em);

CREATE TABLE inscricao_lista_espera (
    id                     UUID         PRIMARY KEY,
    pessoa_id              UUID         NOT NULL REFERENCES pessoa (id),
    data_cadastro          DATE         NOT NULL,
    origem_encaminhamento  VARCHAR(30)  NOT NULL,
    situacao               VARCHAR(30)  NOT NULL,
    criado_em              TIMESTAMPTZ  NOT NULL,
    criado_por             UUID,
    atualizado_em          TIMESTAMPTZ  NOT NULL,
    atualizado_por         UUID,
    versao                 BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_inscricao_origem
        CHECK (origem_encaminhamento IN ('CRAS', 'CREAS', 'JUDICIARIO', 'SAUDE', 'EDUCACAO', 'DEMANDA_ESPONTANEA'))
);

-- Fila: inscrições de uma situação em ordem de cadastro, com o criado_em como desempate.
CREATE INDEX ix_inscricao_fila ON inscricao_lista_espera (situacao, data_cadastro, criado_em);
CREATE INDEX ix_inscricao_pessoa ON inscricao_lista_espera (pessoa_id);
