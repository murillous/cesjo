package br.org.cesjo.sgi.service;

import br.org.cesjo.sgi.api.CadastroBeneficiarioRequest;
import br.org.cesjo.sgi.domain.Beneficiario;
import br.org.cesjo.sgi.domain.ListaEspera;
import br.org.cesjo.sgi.domain.OrigemEncaminhamento;
import br.org.cesjo.sgi.domain.TipoDeficiencia;
import br.org.cesjo.sgi.exception.RegraNegocioException;
import br.org.cesjo.sgi.repository.BeneficiarioRepository;
import br.org.cesjo.sgi.repository.ContaAcessoRepository;
import br.org.cesjo.sgi.repository.ListaEsperaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:target/cesjo-test.db",
        "app.upload-dir=target/test-uploads"
})
@Transactional
class ListaEsperaServiceIntegrationTest {

    static {
        try {
            Files.deleteIfExists(Path.of("target/cesjo-test.db"));
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @Autowired
    private ListaEsperaService service;

    @Autowired
    private ListaEsperaRepository listaRepository;

    @Autowired
    private BeneficiarioRepository beneficiarioRepository;

    @Autowired
    private ContaAcessoRepository contaRepository;

    @Test
    void carregaDadosIniciaisEmOrdemCronologica() {
        var itens = service.listar("");

        assertEquals(3, itens.size());
        assertEquals(1, itens.getFirst().posicao());
        assertTrue(itens.getFirst().dataEntrada().isBefore(itens.getLast().dataEntrada()));
    }

    @Test
    void cadastraBeneficiarioDocumentosEEntradaEmUmaOperacao() {
        var request = new CadastroBeneficiarioRequest(
                "Pessoa de Teste", LocalDate.of(1994, 3, 20), "Responsável de Teste",
                "(98) 99123-4567", TipoDeficiencia.MULTIPLA, true, true,
                OrigemEncaminhamento.CREAS, LocalDate.of(2026, 8, 13)
        );
        var pdf = new MockMultipartFile("arquivo", "documento.pdf", "application/pdf", "pdf".getBytes());

        var resposta = service.cadastrar(request, pdf, pdf);

        assertEquals("Pessoa de Teste", resposta.nome());
        assertEquals(4, resposta.posicao());
        assertEquals("98991234567", resposta.telefone());
        assertEquals(4, service.resumo().entradasAtivas());
    }

    @Test
    void rejeitaArquivoForaDosFormatosPermitidosSemPersistir() {
        long quantidadeAnterior = beneficiarioRepository.count();
        var request = new CadastroBeneficiarioRequest(
                "Outro Teste", LocalDate.of(1990, 1, 1), "Responsável",
                "98991234567", TipoDeficiencia.FISICA, false, false,
                OrigemEncaminhamento.CRAS, LocalDate.of(2026, 8, 13)
        );
        var texto = new MockMultipartFile("arquivo", "documento.txt", "text/plain", "texto".getBytes());

        assertThrows(RegraNegocioException.class, () -> service.cadastrar(request, texto, texto));
        assertEquals(quantidadeAnterior, beneficiarioRepository.count());
    }

    @Test
    void rejeitaExtensaoIncompativelComOTipoDoArquivo() {
        var request = new CadastroBeneficiarioRequest(
                "Teste de Extensão", LocalDate.of(1990, 1, 1), "Responsável",
                "98991234567", TipoDeficiencia.FISICA, false, false,
                OrigemEncaminhamento.CRAS, LocalDate.of(2026, 8, 13)
        );
        var arquivo = new MockMultipartFile("arquivo", "documento.exe", "application/pdf", "pdf".getBytes());

        assertThrows(RegraNegocioException.class, () -> service.cadastrar(request, arquivo, arquivo));
    }

    @Test
    void bancoImpedeDuasEntradasAtivasParaMesmoBeneficiario() {
        Beneficiario beneficiario = beneficiarioRepository.findAll().getFirst();
        var conta = contaRepository.findByEmail("ator-local@cesjo.invalid").orElseThrow();

        assertThrows(DataAccessException.class, () ->
                listaRepository.saveAndFlush(new ListaEspera(beneficiario, LocalDate.of(2026, 8, 13), conta))
        );
    }
}
