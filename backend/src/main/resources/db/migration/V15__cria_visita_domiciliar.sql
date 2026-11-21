-- Visitas domiciliares de avaliação de perfil (RF004, issue #34, ADR 0070).
-- A visita é do módulo Atendimento e guarda só o pessoa_id; os profissionais são colaboradores do
-- módulo Institucional, guardados por colaborador_id.

CREATE TABLE visita_domiciliar (
    id              UUID         PRIMARY KEY,
    pessoa_id       UUID         NOT NULL REFERENCES pessoa (id),
    -- Data e hora locais do Centro-Dia (America/Sao_Paulo), sem fuso (ADR 0070).
    data_hora       TIMESTAMP    NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    criado_em       TIMESTAMPTZ  NOT NULL,
    criado_por      UUID,
    atualizado_em   TIMESTAMPTZ  NOT NULL,
    atualizado_por  UUID,
    versao          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_visita_status CHECK (status IN ('AGENDADA', 'REALIZADA', 'CANCELADA'))
);

-- Agenda por período e visitas de uma pessoa.
CREATE INDEX ix_visita_data_hora ON visita_domiciliar (data_hora);
CREATE INDEX ix_visita_pessoa ON visita_domiciliar (pessoa_id, data_hora);

CREATE TABLE visita_profissional (
    visita_id       UUID  NOT NULL REFERENCES visita_domiciliar (id),
    colaborador_id  UUID  NOT NULL REFERENCES colaborador (id),
    PRIMARY KEY (visita_id, colaborador_id)
);

CREATE INDEX ix_visita_profissional_colaborador ON visita_profissional (colaborador_id);
