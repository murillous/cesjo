-- Importação de planilhas (seção 5.2, issue #33, ADR 0217): a auditoria ganha a ação IMPORTAR, uma por
-- planilha confirmada. Cada registro criado pela importação continua com o seu CRIAR.
ALTER TABLE auditoria DROP CONSTRAINT auditoria_acao_valida;
ALTER TABLE auditoria ADD CONSTRAINT auditoria_acao_valida CHECK (acao IN ('CRIAR', 'ALTERAR', 'EXCLUIR',
    'LER_SENSIVEL', 'EXPORTAR', 'IMPORTAR', 'LOGIN', 'LOGIN_FALHO', 'LOGOUT'));
