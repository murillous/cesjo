-- Atividades com usuários atendidos e famílias (RF010, issue #45, ADR 0150).
-- A atividade é do módulo Atividades e guarda só os ids: participantes são pessoas do módulo Pessoas
-- (usuários atendidos), profissionais são colaboradores do módulo Institucional.
-- A descrição é cifrada na aplicação (TextoCifradoConverter), por isso é text e não é pesquisável.

CREATE TABLE atividade (
    id              UUID          PRIMARY KEY,
    tipo            VARCHAR(40)   NOT NULL,
    data_atividade  DATE          NOT NULL,
    titulo          VARCHAR(200)  NOT NULL,
    descricao       TEXT          NOT NULL,
    criado_em       TIMESTAMPTZ   NOT NULL,
    criado_por      UUID,
    atualizado_em   TIMESTAMPTZ   NOT NULL,
    atualizado_por  UUID,
    versao          BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT ck_atividade_tipo CHECK (tipo IN ('COM_FAMILIAS', 'COM_USUARIOS', 'COM_USUARIOS_E_FAMILIAS'))
);

-- Listagens e contagens por período e por tipo (dashboards e relatório mensal, issues #57 e #58).
CREATE INDEX ix_atividade_data_tipo ON atividade (data_atividade, tipo);

CREATE TABLE atividade_participante (
    atividade_id  UUID  NOT NULL REFERENCES atividade (id),
    pessoa_id     UUID  NOT NULL REFERENCES pessoa (id),
    PRIMARY KEY (atividade_id, pessoa_id)
);

-- Atividades de um usuário atendido.
CREATE INDEX ix_atividade_participante_pessoa ON atividade_participante (pessoa_id);

CREATE TABLE atividade_profissional (
    atividade_id    UUID  NOT NULL REFERENCES atividade (id),
    colaborador_id  UUID  NOT NULL REFERENCES colaborador (id),
    PRIMARY KEY (atividade_id, colaborador_id)
);

-- Atividades de um profissional.
CREATE INDEX ix_atividade_profissional_colaborador ON atividade_profissional (colaborador_id);
