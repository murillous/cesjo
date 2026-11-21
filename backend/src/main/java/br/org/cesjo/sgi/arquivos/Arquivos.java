package br.org.cesjo.sgi.arquivos;

import java.util.Optional;
import java.util.UUID;

/**
 * API pública do módulo Arquivos para os outros módulos guardarem e lerem arquivos sem passar pelo
 * HTTP (ADR 0028). O módulo que chama guarda só o {@code id} devolvido.
 *
 * <p>
 * A permissão é de quem chama: este serviço não checa perfil, porque cada módulo decide quem pode
 * anexar e ler os seus documentos. A auditoria é daqui: guardar registra criação e ler o conteúdo
 * registra leitura de dado sensível.
 */
public interface Arquivos {

	/** Nome da entidade no log de auditoria. */
	String ENTIDADE = "arquivo";

	/**
	 * Valida, cifra e guarda o arquivo.
	 *
	 * @throws ArquivoRejeitadoException quando o arquivo está vazio, passa do tamanho máximo ou não é
	 * PDF, JPG ou PNG
	 */
	MetadadosArquivo guardar(NovoArquivo arquivo);

	/**
	 * Lê e decifra o conteúdo, conferindo o SHA-256. Registra leitura de dado sensível na auditoria.
	 *
	 * @throws ArquivoNaoEncontradoException quando não há arquivo com o id
	 */
	ConteudoArquivo ler(UUID id);

	/** Metadados sem o conteúdo. Não é auditado, porque não expõe o documento. */
	Optional<MetadadosArquivo> metadados(UUID id);

}
