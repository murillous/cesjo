package br.org.cesjo.sgi.service;

import br.org.cesjo.sgi.api.BeneficiarioDetalheResponse;
import br.org.cesjo.sgi.api.CadastroBeneficiarioRequest;
import br.org.cesjo.sgi.api.DashboardResumoResponse;
import br.org.cesjo.sgi.api.DocumentoResponse;
import br.org.cesjo.sgi.api.ListaEsperaItemResponse;
import br.org.cesjo.sgi.api.PosicaoResponse;
import br.org.cesjo.sgi.domain.Beneficiario;
import br.org.cesjo.sgi.domain.ContaAcesso;
import br.org.cesjo.sgi.domain.DocumentoArmazenado;
import br.org.cesjo.sgi.domain.ListaEspera;
import br.org.cesjo.sgi.exception.RecursoNaoEncontradoException;
import br.org.cesjo.sgi.repository.BeneficiarioRepository;
import br.org.cesjo.sgi.repository.ContaAcessoRepository;
import br.org.cesjo.sgi.repository.ListaEsperaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
public class ListaEsperaService {

    static final String EMAIL_ATOR_MVP = "ator-local@cesjo.invalid";

    private final BeneficiarioRepository beneficiarioRepository;
    private final ListaEsperaRepository listaEsperaRepository;
    private final ContaAcessoRepository contaAcessoRepository;
    private final ArmazenamentoDocumentoService armazenamentoDocumentoService;

    public ListaEsperaService(
            BeneficiarioRepository beneficiarioRepository,
            ListaEsperaRepository listaEsperaRepository,
            ContaAcessoRepository contaAcessoRepository,
            ArmazenamentoDocumentoService armazenamentoDocumentoService
    ) {
        this.beneficiarioRepository = beneficiarioRepository;
        this.listaEsperaRepository = listaEsperaRepository;
        this.contaAcessoRepository = contaAcessoRepository;
        this.armazenamentoDocumentoService = armazenamentoDocumentoService;
    }

    @Transactional
    public BeneficiarioDetalheResponse cadastrar(
            CadastroBeneficiarioRequest request,
            MultipartFile laudo,
            MultipartFile comprovante
    ) {
        RegraBeneficiario.validarIdade(request.dataNascimento(), request.dataEntrada());
        String telefone = RegraBeneficiario.normalizarTelefone(request.telefone());
        ContaAcesso conta = obterAtorLocal();

        DocumentoArmazenado laudoArmazenado = null;
        DocumentoArmazenado comprovanteArmazenado = null;
        try {
            laudoArmazenado = armazenamentoDocumentoService.armazenar(laudo, "Laudo médico");
            comprovanteArmazenado = armazenamentoDocumentoService.armazenar(comprovante, "Comprovante de residência");

            Beneficiario beneficiario = beneficiarioRepository.save(new Beneficiario(
                    request.nome().trim(),
                    request.dataNascimento(),
                    request.responsavel().trim(),
                    telefone,
                    request.tipoDeficiencia(),
                    request.beneficiarioBpc(),
                    request.inscritoCadunico(),
                    request.origemEncaminhamento(),
                    laudoArmazenado,
                    comprovanteArmazenado,
                    conta
            ));

            ListaEspera entrada = listaEsperaRepository.saveAndFlush(
                    new ListaEspera(beneficiario, request.dataEntrada(), conta)
            );
            int posicao = calcularPosicao(entrada.getId());
            return mapearDetalhe(beneficiario, entrada, posicao);
        } catch (RuntimeException exception) {
            armazenamentoDocumentoService.removerSilenciosamente(laudoArmazenado);
            armazenamentoDocumentoService.removerSilenciosamente(comprovanteArmazenado);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<ListaEsperaItemResponse> listar(String busca) {
        List<ListaEspera> entradas = listaEsperaRepository.listarAtivasOrdenadas();
        String termo = normalizar(busca);

        return entradas.stream()
                .map(entrada -> new EntradaPosicionada(entrada, entradas.indexOf(entrada) + 1))
                .filter(item -> termo.isBlank() || normalizar(item.entrada().getBeneficiario().getNome()).contains(termo))
                .map(item -> mapearItem(item.entrada(), item.posicao()))
                .toList();
    }

    @Transactional(readOnly = true)
    public BeneficiarioDetalheResponse detalhar(Integer beneficiarioId) {
        ListaEspera entrada = listaEsperaRepository.buscarAtivaPorBeneficiario(beneficiarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Beneficiário não encontrado na lista de espera"));
        return mapearDetalhe(entrada.getBeneficiario(), entrada, calcularPosicao(entrada.getId()));
    }

    @Transactional(readOnly = true)
    public PosicaoResponse consultarPosicao(Integer entradaId) {
        ListaEspera entrada = listaEsperaRepository.listarAtivasOrdenadas().stream()
                .filter(item -> item.getId().equals(entradaId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Entrada ativa não encontrada"));
        Beneficiario beneficiario = entrada.getBeneficiario();
        return new PosicaoResponse(
                entrada.getId(), beneficiario.getId(), beneficiario.getNome(), calcularPosicao(entradaId)
        );
    }

    @Transactional(readOnly = true)
    public DashboardResumoResponse resumo() {
        return new DashboardResumoResponse(listaEsperaRepository.countByDataSaidaIsNull());
    }

    private ContaAcesso obterAtorLocal() {
        return contaAcessoRepository.findByEmail(EMAIL_ATOR_MVP)
                .orElseThrow(() -> new IllegalStateException("Ator técnico local não foi inicializado"));
    }

    private int calcularPosicao(Integer entradaId) {
        List<ListaEspera> entradas = listaEsperaRepository.listarAtivasOrdenadas();
        for (int indice = 0; indice < entradas.size(); indice++) {
            if (entradas.get(indice).getId().equals(entradaId)) {
                return indice + 1;
            }
        }
        throw new RecursoNaoEncontradoException("Entrada ativa não encontrada");
    }

    private ListaEsperaItemResponse mapearItem(ListaEspera entrada, int posicao) {
        Beneficiario beneficiario = entrada.getBeneficiario();
        return new ListaEsperaItemResponse(
                entrada.getId(),
                beneficiario.getId(),
                posicao,
                beneficiario.getNome(),
                beneficiario.getTipoDeficiencia().getDescricao(),
                entrada.getDataEntrada(),
                beneficiario.isBeneficiarioBpc(),
                beneficiario.isInscritoCadunico(),
                beneficiario.getOrigemEncaminhamento().getDescricao()
        );
    }

    private BeneficiarioDetalheResponse mapearDetalhe(Beneficiario beneficiario, ListaEspera entrada, int posicao) {
        return new BeneficiarioDetalheResponse(
                beneficiario.getId(),
                entrada.getId(),
                posicao,
                beneficiario.getNome(),
                beneficiario.getDataNascimento(),
                beneficiario.getResponsavel(),
                beneficiario.getTelefone(),
                beneficiario.getTipoDeficiencia().getDescricao(),
                beneficiario.isBeneficiarioBpc(),
                beneficiario.isInscritoCadunico(),
                beneficiario.getOrigemEncaminhamento().getDescricao(),
                entrada.getDataEntrada(),
                new DocumentoResponse(
                        beneficiario.getLaudoNomeOriginal(),
                        beneficiario.getLaudoTipoConteudo(),
                        beneficiario.getLaudoTamanho()
                ),
                new DocumentoResponse(
                        beneficiario.getComprovanteNomeOriginal(),
                        beneficiario.getComprovanteTipoConteudo(),
                        beneficiario.getComprovanteTamanho()
                ),
                beneficiario.getDataCriacao()
        );
    }

    private String normalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private record EntradaPosicionada(ListaEspera entrada, int posicao) {
    }
}
