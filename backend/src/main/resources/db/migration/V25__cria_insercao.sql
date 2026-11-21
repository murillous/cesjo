-- Inserção no serviço: o usuário atendido (RF007, issue #39, ADR 0080).
-- Guarda só os ids da pessoa, da inscrição e do cuidador (responsável), que são de outros módulos.
-- A faixa etária não tem coluna: sai da data de nascimento da pessoa.

CREATE TABLE insercao (
    id                       UUID         PRIMARY KEY,
    pessoa_id                UUID         NOT NULL REFERENCES pessoa (id),
    inscricao_id             UUID         NOT NULL REFERENCES inscricao_lista_espera (id),
    data_insercao            DATE         NOT NULL,
    turno                    VARCHAR(20)  NOT NULL,
    cuidador_responsavel_id  UUID         NOT NULL REFERENCES responsavel (id),
    situacao                 VARCHAR(20)  NOT NULL,
    criado_em                TIMESTAMPTZ  NOT NULL,
    criado_por               UUID,
    atualizado_em            TIMESTAMPTZ  NOT NULL,
    atualizado_por           UUID,
    versao                   BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_insercao_turno CHECK (turno IN ('MATUTINO', 'VESPERTINO', 'INTEGRAL')),
    CONSTRAINT ck_insercao_situacao CHECK (situacao IN ('ATIVA', 'DESLIGADA')),
    -- Uma inscrição da fila vira no máximo uma inserção.
    CONSTRAINT uq_insercao_inscricao UNIQUE (inscricao_id)
);

-- Uma pessoa tem no máximo uma inserção ativa; depois de desligada pode voltar por outra inscrição.
CREATE UNIQUE INDEX uq_insercao_pessoa_ativa ON insercao (pessoa_id) WHERE situacao = 'ATIVA';

-- Listagem e contagem de vagas: inserções por situação e turno.
CREATE INDEX ix_insercao_situacao_turno ON insercao (situacao, turno, data_insercao);
