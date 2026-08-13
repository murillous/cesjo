package br.org.cesjo.sgi.domain;

public enum OrigemEncaminhamento {
    CRAS("CRAS"),
    CREAS("CREAS"),
    JUDICIARIO("Judiciário"),
    SAUDE("Saúde"),
    EDUCACAO("Educação"),
    DEMANDA_ESPONTANEA("Demanda espontânea");

    private final String descricao;

    OrigemEncaminhamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}

