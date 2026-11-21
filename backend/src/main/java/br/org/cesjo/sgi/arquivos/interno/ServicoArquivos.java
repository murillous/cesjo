package br.org.cesjo.sgi.arquivos.interno;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.unit.DataSize;

import br.org.cesjo.sgi.arquivos.ArquivoNaoEncontradoException;
import br.org.cesjo.sgi.arquivos.ArquivoRejeitadoException;
import br.org.cesjo.sgi.arquivos.Arquivos;
import br.org.cesjo.sgi.arquivos.ConteudoArquivo;
import br.org.cesjo.sgi.arquivos.MetadadosArquivo;
import br.org.cesjo.sgi.arquivos.NovoArquivo;
import br.org.cesjo.sgi.arquivos.TipoArquivo;
import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.criptografia.Cifrador;
import br.org.cesjo.sgi.compartilhado.seguranca.Identidade;
import br.org.cesjo.sgi.compartilhado.seguranca.IdentidadeAtual;

/** Implementação de {@link Arquivos} (ADR 0026, ADR 0027 e ADR 0028). */
@Service
class ServicoArquivos implements Arquivos {

	/** Tamanho máximo do nome guardado, contando a extensão. */
	static final int TAMANHO_MAXIMO_NOME = 255;

	private final ArquivoRepository repositorio;

	private final ArmazenamentoArquivos armazenamento;

	private final Cifrador cifrador;

	private final Auditoria auditoria;

	private final IdentidadeAtual identidadeAtual;

	private final DataSize tamanhoMaximo;

	private final Clock relogio = Clock.systemUTC();

	ServicoArquivos(ArquivoRepository repositorio, ArmazenamentoArquivos armazenamento, Cifrador cifrador,
			Auditoria auditoria, IdentidadeAtual identidadeAtual, PropriedadesArquivos propriedades) {
		this.repositorio = repositorio;
		this.armazenamento = armazenamento;
		this.cifrador = cifrador;
		this.auditoria = auditoria;
		this.identidadeAtual = identidadeAtual;
		this.tamanhoMaximo = propriedades.tamanhoMaximo();
	}

	@Override
	@Transactional
	public MetadadosArquivo guardar(NovoArquivo novo) {
		byte[] conteudo = novo.conteudo();
		if (conteudo.length == 0) {
			throw new ArquivoRejeitadoException("O arquivo está vazio.");
		}
		if (conteudo.length > tamanhoMaximo.toBytes()) {
			throw new ArquivoRejeitadoException("O arquivo ultrapassa o limite de " + descrever(tamanhoMaximo) + ".");
		}
		TipoArquivo tipo = TipoArquivo.detectar(conteudo)
			.orElseThrow(() -> new ArquivoRejeitadoException(
					"Tipo de arquivo não aceito. Envie um arquivo PDF, JPG ou PNG."));

		UUID id = UUID.randomUUID();
		String caminho = ArmazenamentoArquivos.caminhoDe(id);
		byte[] cifrado = cifrador.cifrar(conteudo);
		armazenamento.gravar(caminho, cifrado);
		apagarSeDesfizer(caminho);

		Arquivo arquivo = new Arquivo(id, normalizarNome(novo.nomeOriginal(), tipo), tipo, conteudo.length,
				sha256(conteudo), caminho, cifrador.chaveDoValor(cifrado),
				identidadeAtual.obter().map(Identidade::id).orElse(null),
				Instant.now(relogio).truncatedTo(ChronoUnit.MICROS));
		repositorio.save(arquivo);
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE, id));
		return arquivo.metadados();
	}

	@Override
	@Transactional(readOnly = true)
	public ConteudoArquivo ler(UUID id) {
		Arquivo arquivo = repositorio.findById(id).orElseThrow(() -> new ArquivoNaoEncontradoException(id));
		MetadadosArquivo metadados = arquivo.metadados();
		byte[] conteudo = cifrador.decifrar(armazenamento.ler(arquivo.caminhoArmazenamento()));
		if (!sha256(conteudo).equals(metadados.sha256())) {
			throw new IllegalStateException("SHA-256 do arquivo " + id + " não confere com o registrado.");
		}
		auditoria.registrar(EventoAuditoria.leituraSensivel(ENTIDADE, id));
		return new ConteudoArquivo(metadados, conteudo);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<MetadadosArquivo> metadados(UUID id) {
		return repositorio.findById(id).map(Arquivo::metadados);
	}

	/** Se a transação for desfeita, o arquivo já gravado no disco é apagado. */
	private void apagarSeDesfizer(String caminho) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

			@Override
			public void afterCompletion(int status) {
				if (status != STATUS_COMMITTED) {
					armazenamento.apagar(caminho);
				}
			}

		});
	}

	/**
	 * Tira o caminho e os caracteres de controle do nome, limita o tamanho e garante a extensão do tipo
	 * reconhecido, para que o download nunca saia com uma extensão enganosa (ADR 0027).
	 */
	static String normalizarNome(String nomeOriginal, TipoArquivo tipo) {
		String nome = nomeOriginal == null ? "" : nomeOriginal;
		nome = nome.substring(Math.max(nome.lastIndexOf('/'), nome.lastIndexOf('\\')) + 1);
		nome = nome.replaceAll("\\p{Cntrl}", "").strip();
		String extensao = tipo.extensoes().getFirst();
		if (nome.isEmpty()) {
			nome = "arquivo";
		}
		if (!tipo.temExtensao(nome)) {
			nome = nome + extensao;
		}
		if (nome.length() > TAMANHO_MAXIMO_NOME) {
			nome = nome.substring(0, TAMANHO_MAXIMO_NOME - extensao.length()) + extensao;
		}
		return nome;
	}

	static String sha256(byte[] conteudo) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo));
		}
		catch (NoSuchAlgorithmException erro) {
			throw new IllegalStateException("SHA-256 indisponível na JVM", erro);
		}
	}

	private static String descrever(DataSize tamanho) {
		long bytes = tamanho.toBytes();
		if (bytes % DataSize.ofMegabytes(1).toBytes() == 0) {
			return tamanho.toMegabytes() + " MB";
		}
		if (bytes % DataSize.ofKilobytes(1).toBytes() == 0) {
			return tamanho.toKilobytes() + " KB";
		}
		return bytes + " bytes";
	}

}
