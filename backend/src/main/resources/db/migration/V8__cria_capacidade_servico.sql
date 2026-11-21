-- Capacidade do serviço (RF003, issue #30, ADR 0092). Cada definição é uma linha nova; a vigente é a
-- mais recente. Uma capacidade total, sem divisão por turno (pergunta 3).

CREATE TABLE capacidade_servico (
    id            UUID         PRIMARY KEY,
    vagas         INTEGER      NOT NULL,
    definida_por  UUID,
    definida_em   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_capacidade_vagas CHECK (vagas >= 0 AND vagas <= 10000)
);

CREATE INDEX ix_capacidade_vigente ON capacidade_servico (definida_em DESC);
