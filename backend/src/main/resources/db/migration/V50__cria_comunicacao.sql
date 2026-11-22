-- Contatos e comunicações (RF011, issue #48, ADR 0160).
-- Contatos telefônicos com cuidadores familiares, contatos telefônicos institucionais e atendimento a
-- público não acompanhado. A pessoa ou instituição do outro lado (contraparte) e o assunto ficam em
-- claro, para a listagem e a busca; a descrição, opcional, é cifrada na aplicação (TextoCifradoConverter).

CREATE TABLE comunicacao (
    id                UUID          PRIMARY KEY,
    tipo              VARCHAR(40)   NOT NULL,
    data_comunicacao  DATE          NOT NULL,
    contraparte       VARCHAR(200)  NOT NULL,
    assunto           VARCHAR(200)  NOT NULL,
    descricao         TEXT,
    criado_em         TIMESTAMPTZ   NOT NULL,
    criado_por        UUID,
    atualizado_em     TIMESTAMPTZ   NOT NULL,
    atualizado_por    UUID,
    versao            BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT ck_comunicacao_tipo CHECK (tipo IN ('CONTATO_CUIDADOR_FAMILIAR', 'CONTATO_INSTITUCIONAL',
        'ATENDIMENTO_PUBLICO_NAO_ACOMPANHADO'))
);

-- Listagem e contagem por período e por tipo (relatório mensal, issue #58).
CREATE INDEX ix_comunicacao_data_tipo ON comunicacao (data_comunicacao, tipo);
