-- Ofícios, declarações e encaminhamentos (RF011, issue #49, ADR 0164).
-- Os três tipos novos têm direção (enviada ou recebida: expedição ou recebimento do ofício ou da
-- declaração, encaminhamento enviado ou recebido); os contatos e atendimentos da V50 não têm.
-- O anexo é opcional: arquivo_id aponta para o módulo Arquivos, que guarda o conteúdo cifrado.

ALTER TABLE comunicacao
    ADD COLUMN direcao     VARCHAR(10),
    ADD COLUMN arquivo_id  UUID REFERENCES arquivo (id);

ALTER TABLE comunicacao DROP CONSTRAINT ck_comunicacao_tipo;

ALTER TABLE comunicacao
    ADD CONSTRAINT ck_comunicacao_tipo CHECK (tipo IN ('CONTATO_CUIDADOR_FAMILIAR', 'CONTATO_INSTITUCIONAL',
        'ATENDIMENTO_PUBLICO_NAO_ACOMPANHADO', 'OFICIO', 'DECLARACAO', 'ENCAMINHAMENTO')),
    ADD CONSTRAINT ck_comunicacao_direcao CHECK (
        (tipo IN ('OFICIO', 'DECLARACAO', 'ENCAMINHAMENTO') AND direcao IN ('ENVIADA', 'RECEBIDA'))
        OR (tipo NOT IN ('OFICIO', 'DECLARACAO', 'ENCAMINHAMENTO') AND direcao IS NULL));

