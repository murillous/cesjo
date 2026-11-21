-- Notificações internas (RF003, issue #19, ADR 0030).
-- Cada notificação vai para um perfil ou para uma conta, nunca para os dois. O estado de lida fica
-- em notificacao_leitura, uma linha por conta que leu, para que a leitura de uma pessoa não marque a
-- notificação de perfil para as outras.

CREATE TABLE notificacao (
    id              UUID         PRIMARY KEY,
    perfil_destino  VARCHAR(30),
    conta_destino   UUID,
    tipo            VARCHAR(60)  NOT NULL,
    mensagem        VARCHAR(500) NOT NULL,
    referencia      VARCHAR(300),
    criada_em       TIMESTAMPTZ  NOT NULL,
    criado_por      UUID,
    atualizado_em   TIMESTAMPTZ  NOT NULL,
    atualizado_por  UUID,
    CONSTRAINT ck_notificacao_um_destino
        CHECK ((perfil_destino IS NULL) <> (conta_destino IS NULL)),
    CONSTRAINT ck_notificacao_perfil_destino
        CHECK (perfil_destino IN ('COORDENADOR', 'EQUIPE_TECNICA', 'ADMINISTRATIVO', 'CONSULTA'))
);

CREATE INDEX ix_notificacao_conta_destino ON notificacao (conta_destino, criada_em DESC)
    WHERE conta_destino IS NOT NULL;
CREATE INDEX ix_notificacao_perfil_destino ON notificacao (perfil_destino, criada_em DESC)
    WHERE perfil_destino IS NOT NULL;

CREATE TABLE notificacao_leitura (
    notificacao_id  UUID        NOT NULL REFERENCES notificacao (id) ON DELETE CASCADE,
    conta_id        UUID        NOT NULL,
    lida_em         TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (notificacao_id, conta_id)
);

CREATE INDEX ix_notificacao_leitura_conta ON notificacao_leitura (conta_id);
