-- Reuniões e eventos institucionais (RF012, issue #52, ADR 0175).
-- A descrição é cifrada na aplicação (formato v1 do ADR 0025), por isso é text: reuniões da equipe
-- podem discutir casos de usuários atendidos.

CREATE TABLE evento_institucional (
    id              UUID         PRIMARY KEY,
    tipo            VARCHAR(40)  NOT NULL,
    direcao         VARCHAR(20),
    data            DATE         NOT NULL,
    descricao       TEXT         NOT NULL,
    participantes   VARCHAR(2000),
    parceiro_id     UUID         REFERENCES parceiro (id),
    criado_em       TIMESTAMPTZ  NOT NULL,
    criado_por      UUID,
    atualizado_em   TIMESTAMPTZ  NOT NULL,
    atualizado_por  UUID,
    versao          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_evento_tipo CHECK (tipo IN ('REUNIAO_COORDENACAO_EQUIPE', 'FORMACAO_MULTIPROFISSIONAL',
        'REUNIAO_DIRECAO_GERAL', 'MONITORAMENTO_ORGAO_GESTOR', 'VISITA_INSTITUCIONAL', 'EVENTO',
        'REUNIAO_PARCEIROS')),
    -- Visitas são realizadas ou recebidas; eventos, realizados ou participados; os demais tipos não têm direção.
    CONSTRAINT ck_evento_direcao CHECK (
        (tipo = 'VISITA_INSTITUCIONAL' AND direcao IN ('REALIZADO', 'RECEBIDO'))
        OR (tipo = 'EVENTO' AND direcao IN ('REALIZADO', 'PARTICIPADO'))
        OR (tipo NOT IN ('VISITA_INSTITUCIONAL', 'EVENTO') AND direcao IS NULL))
);

-- Listagem por período e por tipo (relatórios do RF016).
CREATE INDEX ix_evento_data ON evento_institucional (data DESC);
CREATE INDEX ix_evento_tipo_data ON evento_institucional (tipo, data DESC);
CREATE INDEX ix_evento_parceiro ON evento_institucional (parceiro_id) WHERE parceiro_id IS NOT NULL;
