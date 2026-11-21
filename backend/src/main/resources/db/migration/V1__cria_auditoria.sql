-- Log de auditoria (RNF 3.3, issue #16, ADR 0020 e ADR 0021).
-- Guarda só nomes de campos, nunca valores. A tabela é só de inserção: os gatilhos abaixo recusam
-- UPDATE, DELETE e TRUNCATE.
CREATE TABLE auditoria (
    id               UUID         PRIMARY KEY,
    ocorrido_em      TIMESTAMPTZ  NOT NULL,
    conta_id         UUID,
    conta_login      VARCHAR(100),
    acao             VARCHAR(20)  NOT NULL,
    entidade         VARCHAR(100) NOT NULL,
    entidade_id      VARCHAR(100),
    campos_alterados TEXT[]       NOT NULL DEFAULT '{}',
    ip               VARCHAR(45),
    CONSTRAINT auditoria_acao_valida CHECK (acao IN ('CRIAR', 'ALTERAR', 'EXCLUIR', 'LER_SENSIVEL', 'EXPORTAR',
        'LOGIN', 'LOGIN_FALHO', 'LOGOUT'))
);

-- A consulta sempre ordena por ocorrido_em decrescente e filtra por período, login, entidade e ação.
CREATE INDEX auditoria_ocorrido_em_idx ON auditoria (ocorrido_em DESC, id DESC);
CREATE INDEX auditoria_login_idx ON auditoria (lower(conta_login), ocorrido_em DESC);
CREATE INDEX auditoria_entidade_idx ON auditoria (entidade, ocorrido_em DESC);

CREATE FUNCTION auditoria_recusa_alteracao() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    RAISE EXCEPTION 'O log de auditoria é só de inserção: % não é permitido', TG_OP
        USING ERRCODE = 'insufficient_privilege';
END;
$$;

CREATE TRIGGER auditoria_sem_update_delete
    BEFORE UPDATE OR DELETE ON auditoria
    FOR EACH ROW EXECUTE FUNCTION auditoria_recusa_alteracao();

CREATE TRIGGER auditoria_sem_truncate
    BEFORE TRUNCATE ON auditoria
    FOR EACH STATEMENT EXECUTE FUNCTION auditoria_recusa_alteracao();
