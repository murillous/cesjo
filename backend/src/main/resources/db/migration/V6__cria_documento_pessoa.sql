-- Documentos anexados a uma pessoa: laudo médico e comprovante de residência (RF001, issue #27,
-- ADR 0055). Um documento de cada tipo por pessoa; trocar o arquivo atualiza a linha. O conteúdo fica
-- no módulo Arquivos, aqui só o arquivo_id. Um arquivo é ligado a um documento só.

CREATE TABLE documento_pessoa (
    id              UUID         PRIMARY KEY,
    pessoa_id       UUID         NOT NULL REFERENCES pessoa (id),
    tipo            VARCHAR(30)  NOT NULL,
    arquivo_id      UUID         NOT NULL REFERENCES arquivo (id),
    criado_em       TIMESTAMPTZ  NOT NULL,
    criado_por      UUID,
    atualizado_em   TIMESTAMPTZ  NOT NULL,
    atualizado_por  UUID,
    versao          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_documento_pessoa_tipo CHECK (tipo IN ('LAUDO_MEDICO', 'COMPROVANTE_RESIDENCIA')),
    CONSTRAINT uk_documento_pessoa_tipo UNIQUE (pessoa_id, tipo),
    CONSTRAINT uk_documento_pessoa_arquivo UNIQUE (arquivo_id)
);
