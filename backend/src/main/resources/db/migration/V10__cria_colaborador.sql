-- Colaboradores do Centro-Dia (RF014 parcial, issue #50, ADR 0045 e ADR 0046).
-- A ligação com a conta de acesso fica aqui, em conta_acesso_id, e não em conta_acesso.colaborador_id
-- como no esboço do planejamento: assim o módulo Acesso não depende do Institucional (ADR 0046).

CREATE TABLE colaborador (
    id               UUID         PRIMARY KEY,
    nome             VARCHAR(150) NOT NULL,
    funcao           VARCHAR(30)  NOT NULL,
    ativo            BOOLEAN      NOT NULL DEFAULT TRUE,
    conta_acesso_id  UUID         REFERENCES conta_acesso (id),
    criado_em        TIMESTAMPTZ  NOT NULL,
    criado_por       UUID,
    atualizado_em    TIMESTAMPTZ  NOT NULL,
    atualizado_por   UUID,
    versao           BIGINT       NOT NULL DEFAULT 0,
    -- PENDENTE-CESJO(P18): lista provisória de funções, igual ao enum FuncaoColaborador (ADR 0045).
    CONSTRAINT ck_colaborador_funcao
        CHECK (funcao IN ('ASSISTENTE_SOCIAL', 'PSICOLOGO', 'TERAPEUTA_OCUPACIONAL', 'OUTRA')),
    -- Uma conta de acesso liga a no máximo um colaborador.
    CONSTRAINT uk_colaborador_conta_acesso UNIQUE (conta_acesso_id)
);

-- Listagem por situação em ordem de nome e lista de ativos para visitas, escutas e atividades.
CREATE INDEX ix_colaborador_ativo_nome ON colaborador (ativo, nome);
