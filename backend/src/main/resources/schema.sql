CREATE TABLE IF NOT EXISTS contas_acesso (
    id_conta INTEGER PRIMARY KEY,
    nome TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    senha_hash TEXT NOT NULL,
    perfil TEXT NOT NULL CHECK (
        perfil IN ('COORDENADOR', 'EQUIPE_TECNICA', 'ADMINISTRATIVO', 'CONSULTA')
    )
);

CREATE TABLE IF NOT EXISTS beneficiarios (
    id_beneficiario INTEGER PRIMARY KEY,
    nome TEXT NOT NULL,
    data_nascimento DATE NOT NULL,
    responsavel TEXT NOT NULL,
    telefone TEXT NOT NULL,
    tipo_deficiencia TEXT NOT NULL CHECK (
        tipo_deficiencia IN ('SENSORIAL', 'FISICA', 'TEA', 'INTELECTUAL', 'MULTIPLA', 'T21')
    ),
    beneficiario_bpc BOOLEAN NOT NULL DEFAULT 0,
    inscrito_cadunico BOOLEAN NOT NULL DEFAULT 0,
    origem_encaminhamento TEXT NOT NULL CHECK (
        origem_encaminhamento IN (
            'CRAS', 'CREAS', 'JUDICIARIO', 'SAUDE', 'EDUCACAO', 'DEMANDA_ESPONTANEA'
        )
    ),
    laudo_referencia TEXT NOT NULL,
    laudo_nome_original TEXT NOT NULL,
    laudo_tipo_conteudo TEXT NOT NULL,
    laudo_tamanho BIGINT NOT NULL CHECK (laudo_tamanho > 0),
    comprovante_referencia TEXT NOT NULL,
    comprovante_nome_original TEXT NOT NULL,
    comprovante_tipo_conteudo TEXT NOT NULL,
    comprovante_tamanho BIGINT NOT NULL CHECK (comprovante_tamanho > 0),
    id_conta_cadastro INTEGER NOT NULL,
    data_criacao TIMESTAMP NOT NULL,
    data_atualizacao TIMESTAMP NOT NULL,
    FOREIGN KEY (id_conta_cadastro) REFERENCES contas_acesso(id_conta)
);

CREATE TABLE IF NOT EXISTS lista_espera (
    id_lista_espera INTEGER PRIMARY KEY,
    id_beneficiario INTEGER NOT NULL,
    data_entrada DATE NOT NULL,
    data_saida DATE,
    motivo_saida TEXT,
    observacao TEXT,
    id_conta_cadastro INTEGER NOT NULL,
    data_criacao TIMESTAMP NOT NULL,
    FOREIGN KEY (id_beneficiario) REFERENCES beneficiarios(id_beneficiario),
    FOREIGN KEY (id_conta_cadastro) REFERENCES contas_acesso(id_conta),
    CHECK (data_saida IS NULL OR data_saida >= data_entrada)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_lista_espera_beneficiario_ativo
    ON lista_espera (id_beneficiario)
    WHERE data_saida IS NULL;

CREATE INDEX IF NOT EXISTS ix_lista_espera_ordenacao
    ON lista_espera (data_entrada, id_lista_espera)
    WHERE data_saida IS NULL;
