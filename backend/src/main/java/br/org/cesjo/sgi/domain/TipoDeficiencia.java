package br.org.cesjo.sgi.domain;

public enum TipoDeficiencia {
    SENSORIAL("Sensorial"),
    FISICA("Física"),
    TEA("TEA"),
    INTELECTUAL("Intelectual"),
    MULTIPLA("Múltipla"),
    T21("T21");

    private final String descricao;

    TipoDeficiencia(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}

