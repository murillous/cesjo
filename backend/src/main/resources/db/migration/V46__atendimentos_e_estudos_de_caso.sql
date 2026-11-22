-- Atendimentos e estudos de caso ligados a um usuário atendido (RF010, issue #46, ADR 0154).
-- Entram como mais três tipos na mesma tabela atividade; a descrição já é cifrada na aplicação.

ALTER TABLE atividade DROP CONSTRAINT ck_atividade_tipo;

ALTER TABLE atividade ADD CONSTRAINT ck_atividade_tipo CHECK (tipo IN (
    'COM_FAMILIAS', 'COM_USUARIOS', 'COM_USUARIOS_E_FAMILIAS',
    'ATENDIMENTO_MULTIPROFISSIONAL', 'ATENDIMENTO_TECNICO_INDIVIDUALIZADO', 'ESTUDO_DE_CASO'));
