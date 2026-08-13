package br.org.cesjo.sgi.config;

import br.org.cesjo.sgi.domain.Beneficiario;
import br.org.cesjo.sgi.domain.ContaAcesso;
import br.org.cesjo.sgi.domain.DocumentoArmazenado;
import br.org.cesjo.sgi.domain.ListaEspera;
import br.org.cesjo.sgi.domain.OrigemEncaminhamento;
import br.org.cesjo.sgi.domain.Perfil;
import br.org.cesjo.sgi.domain.TipoDeficiencia;
import br.org.cesjo.sgi.repository.BeneficiarioRepository;
import br.org.cesjo.sgi.repository.ContaAcessoRepository;
import br.org.cesjo.sgi.repository.ListaEsperaRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Configuration
public class DadosIniciaisConfig {

    private static final String EMAIL_ATOR = "ator-local@cesjo.invalid";

    @Bean
    ApplicationRunner carregarDadosIniciais(
            ContaAcessoRepository contaRepository,
            BeneficiarioRepository beneficiarioRepository,
            ListaEsperaRepository listaRepository
    ) {
        return args -> inicializar(contaRepository, beneficiarioRepository, listaRepository);
    }

    @Transactional
    void inicializar(
            ContaAcessoRepository contaRepository,
            BeneficiarioRepository beneficiarioRepository,
            ListaEsperaRepository listaRepository
    ) {
        ContaAcesso conta = contaRepository.findByEmail(EMAIL_ATOR)
                .orElseGet(() -> contaRepository.save(new ContaAcesso(
                        "Ator técnico local",
                        EMAIL_ATOR,
                        "SEM_LOGIN_NO_MVP",
                        Perfil.ADMINISTRATIVO
                )));

        if (beneficiarioRepository.count() > 0) {
            return;
        }

        criarCenario(
                beneficiarioRepository, listaRepository, conta,
                "Ana Martins (demonstração)", LocalDate.of(1996, 4, 12),
                "Carlos Martins", "98990001001", TipoDeficiencia.FISICA,
                true, true, OrigemEncaminhamento.CRAS, LocalDate.of(2026, 6, 3)
        );
        criarCenario(
                beneficiarioRepository, listaRepository, conta,
                "Bruno Costa (demonstração)", LocalDate.of(1987, 9, 25),
                "Marina Costa", "98990001002", TipoDeficiencia.TEA,
                false, true, OrigemEncaminhamento.SAUDE, LocalDate.of(2026, 6, 18)
        );
        criarCenario(
                beneficiarioRepository, listaRepository, conta,
                "Clara Santos (demonstração)", LocalDate.of(2002, 1, 8),
                "Joana Santos", "98990001003", TipoDeficiencia.INTELECTUAL,
                true, false, OrigemEncaminhamento.DEMANDA_ESPONTANEA, LocalDate.of(2026, 7, 2)
        );
    }

    private void criarCenario(
            BeneficiarioRepository beneficiarioRepository,
            ListaEsperaRepository listaRepository,
            ContaAcesso conta,
            String nome,
            LocalDate nascimento,
            String responsavel,
            String telefone,
            TipoDeficiencia tipo,
            boolean bpc,
            boolean cadunico,
            OrigemEncaminhamento origem,
            LocalDate entrada
    ) {
        DocumentoArmazenado laudo = new DocumentoArmazenado(
                "demo-laudo.pdf", "laudo-demonstracao.pdf", "application/pdf", 1024
        );
        DocumentoArmazenado comprovante = new DocumentoArmazenado(
                "demo-comprovante.pdf", "comprovante-demonstracao.pdf", "application/pdf", 1024
        );
        Beneficiario beneficiario = beneficiarioRepository.save(new Beneficiario(
                nome, nascimento, responsavel, telefone, tipo, bpc, cadunico, origem,
                laudo, comprovante, conta
        ));
        listaRepository.save(new ListaEspera(beneficiario, entrada, conta));
    }
}
