package br.org.cesjo.sgi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lista_espera")
public class ListaEspera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lista_espera")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_beneficiario", nullable = false)
    private Beneficiario beneficiario;

    @Column(name = "data_entrada", nullable = false)
    private LocalDate dataEntrada;

    @Column(name = "data_saida")
    private LocalDate dataSaida;

    @Column(name = "motivo_saida", length = 255)
    private String motivoSaida;

    @Column
    private String observacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_conta_cadastro", nullable = false)
    private ContaAcesso contaCadastro;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    protected ListaEspera() {
    }

    public ListaEspera(Beneficiario beneficiario, LocalDate dataEntrada, ContaAcesso contaCadastro) {
        this.beneficiario = beneficiario;
        this.dataEntrada = dataEntrada;
        this.contaCadastro = contaCadastro;
        this.dataCriacao = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public Beneficiario getBeneficiario() { return beneficiario; }
    public LocalDate getDataEntrada() { return dataEntrada; }
    public LocalDate getDataSaida() { return dataSaida; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
}
