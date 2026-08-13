package br.org.cesjo.sgi.api;

import br.org.cesjo.sgi.service.ListaEsperaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ListaEsperaController {

    private final ListaEsperaService service;

    public ListaEsperaController(ListaEsperaService service) {
        this.service = service;
    }

    @PostMapping(value = "/beneficiarios", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public BeneficiarioDetalheResponse cadastrar(
            @Valid @RequestPart("dados") CadastroBeneficiarioRequest dados,
            @RequestPart("laudo") MultipartFile laudo,
            @RequestPart("comprovante") MultipartFile comprovante
    ) {
        return service.cadastrar(dados, laudo, comprovante);
    }

    @GetMapping("/beneficiarios/{id}")
    public BeneficiarioDetalheResponse detalhar(@PathVariable Integer id) {
        return service.detalhar(id);
    }

    @GetMapping("/lista-espera")
    public List<ListaEsperaItemResponse> listar(
            @RequestParam(name = "busca", required = false, defaultValue = "") String busca
    ) {
        return service.listar(busca);
    }

    @GetMapping("/lista-espera/{id}/posicao")
    public PosicaoResponse posicao(@PathVariable Integer id) {
        return service.consultarPosicao(id);
    }

    @GetMapping("/dashboard/resumo")
    public DashboardResumoResponse resumo() {
        return service.resumo();
    }
}
