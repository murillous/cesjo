package br.org.cesjo.sgi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "beneficiarios")
public class Beneficiario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_beneficiario")
    private Integer id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(nullable = false, length = 150)
    private String responsavel;

    @Column(nullable = false, length = 20)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_deficiencia", nullable = false, length = 20)
    private TipoDeficiencia tipoDeficiencia;

    @Column(name = "beneficiario_bpc", nullable = false)
    private boolean beneficiarioBpc;

    @Column(name = "inscrito_cadunico", nullable = false)
    private boolean inscritoCadunico;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_encaminhamento", nullable = false, length = 30)
    private OrigemEncaminhamento origemEncaminhamento;

    @Column(name = "laudo_referencia", nullable = false, length = 500)
    private String laudoReferencia;

    @Column(name = "laudo_nome_original", nullable = false, length = 255)
    private String laudoNomeOriginal;

    @Column(name = "laudo_tipo_conteudo", nullable = false, length = 100)
    private String laudoTipoConteudo;

    @Column(name = "laudo_tamanho", nullable = false)
    private long laudoTamanho;

    @Column(name = "comprovante_referencia", nullable = false, length = 500)
    private String comprovanteReferencia;

    @Column(name = "comprovante_nome_original", nullable = false, length = 255)
    private String comprovanteNomeOriginal;

    @Column(name = "comprovante_tipo_conteudo", nullable = false, length = 100)
    private String comprovanteTipoConteudo;

    @Column(name = "comprovante_tamanho", nullable = false)
    private long comprovanteTamanho;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_conta_cadastro", nullable = false)
    private ContaAcesso contaCadastro;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;

    protected Beneficiario() {
    }

    public Beneficiario(
            String nome,
            LocalDate dataNascimento,
            String responsavel,
            String telefone,
            TipoDeficiencia tipoDeficiencia,
            boolean beneficiarioBpc,
            boolean inscritoCadunico,
            OrigemEncaminhamento origemEncaminhamento,
            DocumentoArmazenado laudo,
            DocumentoArmazenado comprovante,
            ContaAcesso contaCadastro
    ) {
        this.nome = nome;
        this.dataNascimento = dataNascimento;
        this.responsavel = responsavel;
        this.telefone = telefone;
        this.tipoDeficiencia = tipoDeficiencia;
        this.beneficiarioBpc = beneficiarioBpc;
        this.inscritoCadunico = inscritoCadunico;
        this.origemEncaminhamento = origemEncaminhamento;
        this.laudoReferencia = laudo.referencia();
        this.laudoNomeOriginal = laudo.nomeOriginal();
        this.laudoTipoConteudo = laudo.tipoConteudo();
        this.laudoTamanho = laudo.tamanho();
        this.comprovanteReferencia = comprovante.referencia();
        this.comprovanteNomeOriginal = comprovante.nomeOriginal();
        this.comprovanteTipoConteudo = comprovante.tipoConteudo();
        this.comprovanteTamanho = comprovante.tamanho();
        this.contaCadastro = contaCadastro;
        this.dataCriacao = LocalDateTime.now();
        this.dataAtualizacao = this.dataCriacao;
    }

    public Integer getId() { return id; }
    public String getNome() { return nome; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public String getResponsavel() { return responsavel; }
    public String getTelefone() { return telefone; }
    public TipoDeficiencia getTipoDeficiencia() { return tipoDeficiencia; }
    public boolean isBeneficiarioBpc() { return beneficiarioBpc; }
    public boolean isInscritoCadunico() { return inscritoCadunico; }
    public OrigemEncaminhamento getOrigemEncaminhamento() { return origemEncaminhamento; }
    public String getLaudoNomeOriginal() { return laudoNomeOriginal; }
    public String getLaudoTipoConteudo() { return laudoTipoConteudo; }
    public long getLaudoTamanho() { return laudoTamanho; }
    public String getComprovanteNomeOriginal() { return comprovanteNomeOriginal; }
    public String getComprovanteTipoConteudo() { return comprovanteTipoConteudo; }
    public long getComprovanteTamanho() { return comprovanteTamanho; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
}
