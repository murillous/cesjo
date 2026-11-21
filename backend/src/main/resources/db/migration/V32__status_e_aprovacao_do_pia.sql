-- Status e aprovação do PIA (RF006, issue #38, ADR 0106). Cada passo guarda a data em que aconteceu;
-- a aprovação guarda também a conta do Coordenador que aprovou.

ALTER TABLE pia
    ADD COLUMN apresentado_familia_em  DATE,
    ADD COLUMN apresentado_equipe_em   DATE,
    ADD COLUMN aprovado_em             DATE,
    ADD COLUMN aprovado_por            UUID,
    ADD CONSTRAINT ck_pia_apresentado_familia
        CHECK (status = 'ELABORADO' OR apresentado_familia_em IS NOT NULL),
    ADD CONSTRAINT ck_pia_apresentado_equipe
        CHECK (status IN ('ELABORADO', 'APRESENTADO_FAMILIA') OR apresentado_equipe_em IS NOT NULL),
    ADD CONSTRAINT ck_pia_aprovado
        CHECK (status <> 'APROVADO' OR (aprovado_em IS NOT NULL AND aprovado_por IS NOT NULL));
