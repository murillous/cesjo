-- Cadastro completo do usuário atendido (RF007, issue #40, ADR 0140 e ADR 0141).
-- Uma linha por pessoa, criada quando o cadastro é completado pela primeira vez. CPF, NIS, RG e os
-- textos de saúde são cifrados na aplicação (formato v1 do ADR 0025), por isso são TEXT. Gênero,
-- bairro, escolaridade, benefícios e crença ficam em claro, para filtros e dashboards (issue #56).

CREATE TABLE cadastro_complementar (
    pessoa_id                  UUID          PRIMARY KEY REFERENCES pessoa (id),
    genero                     VARCHAR(20),
    cpf                        TEXT,
    nis                        TEXT,
    rg                         TEXT,
    escolaridade               VARCHAR(30),
    cep                        VARCHAR(8),
    logradouro                 VARCHAR(150),
    numero                     VARCHAR(20),
    complemento                VARCHAR(80),
    bairro                     VARCHAR(80),
    cidade                     VARCHAR(80),
    renda_familiar             NUMERIC(12, 2),
    recebe_bpc                 BOOLEAN       NOT NULL DEFAULT FALSE,
    inscrito_cad_unico         BOOLEAN       NOT NULL DEFAULT FALSE,
    recebe_bolsa_familia       BOOLEAN       NOT NULL DEFAULT FALSE,
    crenca_religiosa           VARCHAR(30),
    medicamentos_uso_continuo  TEXT,
    observacoes_saude          TEXT,
    criado_em                  TIMESTAMPTZ   NOT NULL,
    criado_por                 UUID,
    atualizado_em              TIMESTAMPTZ   NOT NULL,
    atualizado_por             UUID,
    versao                     BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT ck_cadastro_genero CHECK (genero IN ('FEMININO', 'MASCULINO', 'OUTRO')),
    CONSTRAINT ck_cadastro_escolaridade CHECK (escolaridade IN ('SEM_ESCOLARIDADE', 'FUNDAMENTAL_INCOMPLETO',
        'FUNDAMENTAL_COMPLETO', 'MEDIO_INCOMPLETO', 'MEDIO_COMPLETO', 'SUPERIOR_INCOMPLETO', 'SUPERIOR_COMPLETO')),
    CONSTRAINT ck_cadastro_crenca CHECK (crenca_religiosa IN ('CATOLICA', 'EVANGELICA', 'TESTEMUNHA_DE_JEOVA',
        'MATRIZ_AFRICANA', 'OUTRAS')),
    CONSTRAINT ck_cadastro_cep CHECK (cep ~ '^[0-9]{8}$'),
    CONSTRAINT ck_cadastro_renda CHECK (renda_familiar >= 0)
);

-- Filtros da listagem e contagens do dashboard (issue #56).
CREATE INDEX ix_cadastro_genero ON cadastro_complementar (genero);
CREATE INDEX ix_cadastro_bairro ON cadastro_complementar (lower(bairro));
