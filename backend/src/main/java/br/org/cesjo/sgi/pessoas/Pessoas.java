package br.org.cesjo.sgi.pessoas;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import br.org.cesjo.sgi.arquivos.ConteudoArquivo;

/**
 * API pública do módulo Pessoas (ADR 0037). Os outros módulos (lista de espera, atendidos) guardam só o
 * {@code id} da pessoa e leem os dados por aqui.
 *
 * <p>
 * A permissão é de quem chama: este serviço não checa perfil. A auditoria é daqui.
 */
public interface Pessoas {

	/** Nome da entidade pessoa no log de auditoria. */
	String ENTIDADE = "pessoa";

	/** Nome da entidade responsável no log de auditoria. */
	String ENTIDADE_RESPONSAVEL = "responsavel";

	/** Nome da entidade documento da pessoa no log de auditoria. */
	String ENTIDADE_DOCUMENTO = "documento_pessoa";

	/** Cadastra a pessoa e o seu responsável e devolve o {@code id} da pessoa. */
	UUID cadastrar(NovaPessoa nova);

	Optional<DadosPessoa> obter(UUID id);

	/**
	 * Busca em lote para listagens de outros módulos (ADR 0059): devolve, por {@code id}, o resumo das
	 * pessoas entre {@code ids} que atendem ao filtro. Os ids sem pessoa ou fora do filtro ficam de fora.
	 * Não é auditado, porque não expõe contato nem documento.
	 */
	Map<UUID, ResumoPessoa> resumos(Collection<UUID> ids, FiltroPessoas filtro);

	/**
	 * Liga um arquivo já guardado no módulo Arquivos à pessoa, como documento do tipo informado (ADR 0055).
	 * Se a pessoa já tem documento desse tipo, o arquivo novo substitui o anterior.
	 *
	 * @throws br.org.cesjo.sgi.compartilhado.web.RecursoNaoEncontradoException quando a pessoa não existe
	 * @throws DocumentoInvalidoException quando o arquivo não existe ou já está ligado a outro documento
	 */
	DocumentoPessoa anexarDocumento(UUID pessoaId, TipoDocumento tipo, UUID arquivoId);

	/** Documentos da pessoa, sem o conteúdo. Não é auditado, porque não expõe os documentos. */
	List<DocumentoPessoa> documentos(UUID pessoaId);

	/**
	 * Conteúdo do documento da pessoa. Registra a leitura sensível do documento (o módulo Arquivos
	 * registra também a do arquivo).
	 *
	 * @throws br.org.cesjo.sgi.compartilhado.web.RecursoNaoEncontradoException quando a pessoa não tem
	 * documento desse tipo
	 */
	ConteudoArquivo lerDocumento(UUID pessoaId, TipoDocumento tipo);

}
