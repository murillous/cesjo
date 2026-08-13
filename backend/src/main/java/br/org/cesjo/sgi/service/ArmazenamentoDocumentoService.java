package br.org.cesjo.sgi.service;

import br.org.cesjo.sgi.domain.DocumentoArmazenado;
import br.org.cesjo.sgi.exception.RegraNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ArmazenamentoDocumentoService {

    private static final long TAMANHO_MAXIMO = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSOES = Map.of(
            "application/pdf", ".pdf",
            "image/jpeg", ".jpg",
            "image/png", ".png"
    );
    private static final Map<String, Set<String>> EXTENSOES_PERMITIDAS = Map.of(
            "application/pdf", Set.of(".pdf"),
            "image/jpeg", Set.of(".jpg", ".jpeg"),
            "image/png", Set.of(".png")
    );

    private final Path diretorio;

    public ArmazenamentoDocumentoService(@Value("${app.upload-dir}") String uploadDir) {
        this.diretorio = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.diretorio);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível preparar o diretório de documentos", exception);
        }
    }

    public DocumentoArmazenado armazenar(MultipartFile arquivo, String nomeCampo) {
        validar(arquivo, nomeCampo);
        String tipoConteudo = arquivo.getContentType();
        String referencia = UUID.randomUUID() + EXTENSOES.get(tipoConteudo);
        Path destino = diretorio.resolve(referencia).normalize();

        if (!destino.startsWith(diretorio)) {
            throw new RegraNegocioException("Referência de documento inválida");
        }

        try {
            Files.copy(arquivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível armazenar o documento", exception);
        }

        return new DocumentoArmazenado(
                referencia,
                nomeOriginalSeguro(arquivo.getOriginalFilename()),
                tipoConteudo,
                arquivo.getSize()
        );
    }

    public void removerSilenciosamente(DocumentoArmazenado documento) {
        if (documento == null) {
            return;
        }
        try {
            Files.deleteIfExists(diretorio.resolve(documento.referencia()).normalize());
        } catch (IOException ignored) {
            // A falha de limpeza não deve ocultar o erro original do cadastro.
        }
    }

    private void validar(MultipartFile arquivo, String nomeCampo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraNegocioException(nomeCampo + " é obrigatório");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new RegraNegocioException(nomeCampo + " deve ter no máximo 5 MB");
        }
        String tipoConteudo = arquivo.getContentType();
        if (tipoConteudo == null || !EXTENSOES.containsKey(tipoConteudo)) {
            throw new RegraNegocioException(nomeCampo + " deve ser PDF, JPEG ou PNG");
        }
        String nomeOriginal = nomeOriginalSeguro(arquivo.getOriginalFilename()).toLowerCase();
        boolean extensaoValida = EXTENSOES_PERMITIDAS.get(tipoConteudo).stream()
                .anyMatch(nomeOriginal::endsWith);
        if (!extensaoValida) {
            throw new RegraNegocioException(nomeCampo + " possui extensão incompatível com o arquivo");
        }
    }

    private String nomeOriginalSeguro(String nomeOriginal) {
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            return "documento";
        }
        String nome = Path.of(nomeOriginal).getFileName().toString();
        return nome.length() <= 255 ? nome : nome.substring(nome.length() - 255);
    }
}
