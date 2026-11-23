-- Índices das consultas mais lentas no teste de desempenho (issue #25, ADR 0235), medidos com
-- auto_explain na massa de 10 vezes o volume esperado (docs/desempenho.md).

-- Log de auditoria filtrado por login: o índice antigo achava as linhas, mas a contagem da paginação
-- lia a tabela. Este cobre a contagem e a página (Index Only Scan) e substitui o antigo (ADR 0234).
CREATE INDEX auditoria_login_cobertura_idx
    ON auditoria (lower(conta_login), ocorrido_em DESC, id DESC) INCLUDE (conta_login);
DROP INDEX auditoria_login_idx;

-- Log de auditoria filtrado por entidade, ação e período: o índice antigo (entidade, data) lia todas
-- as ações da entidade e descartava as outras na tabela. A ação entra na chave; o filtro só por
-- entidade continua usando este índice pela primeira coluna.
CREATE INDEX auditoria_entidade_acao_idx ON auditoria (entidade, acao, ocorrido_em DESC, id DESC);
DROP INDEX auditoria_entidade_idx;

-- Inserções de uma pessoa: não havia índice em pessoa_id (só o parcial da inserção ativa), e o
-- relatório de intervenção e a lista de violações liam a tabela inteira para cada violação, procurando
-- a inserção vigente na data.
CREATE INDEX ix_insercao_pessoa ON insercao (pessoa_id, data_insercao DESC);
