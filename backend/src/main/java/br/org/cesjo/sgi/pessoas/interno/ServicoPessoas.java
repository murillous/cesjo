package br.org.cesjo.sgi.pessoas.interno;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.arquivos.Arquivos;
import br.org.cesjo.sgi.arquivos.ConteudoArquivo;
import br.org.cesjo.sgi.arquivos.MetadadosArquivo;
import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.seguranca.Identidade;
import br.org.cesjo.sgi.compartilhado.seguranca.IdentidadeAtual;
import br.org.cesjo.sgi.compartilhado.web.RecursoNaoEncontradoException;
import br.org.cesjo.sgi.pessoas.DadosPessoa;
import br.org.cesjo.sgi.pessoas.DocumentoInvalidoException;
import br.org.cesjo.sgi.pessoas.DocumentoPessoa;
import br.org.cesjo.sgi.pessoas.NovaPessoa;
import br.org.cesjo.sgi.pessoas.Pessoas;
import br.org.cesjo.sgi.pessoas.TipoDocumento;

@Service
class ServicoPessoas implements Pessoas {

	private final PessoaRepositorio pessoas;

	private final ResponsavelRepositorio responsaveis;

	private final DocumentoRepositorio documentos;

	private final Arquivos arquivos;

	private final Auditoria auditoria;

	private final IdentidadeAtual identidadeAtual;

	private final Clock relogio = Clock.systemUTC();

	ServicoPessoas(PessoaRepositorio pessoas, ResponsavelRepositorio responsaveis, DocumentoRepositorio documentos,
			Arquivos arquivos, Auditoria auditoria, IdentidadeAtual identidadeAtual) {
		this.pessoas = pessoas;
		this.responsaveis = responsaveis;
		this.documentos = documentos;
		this.arquivos = arquivos;
		this.auditoria = auditoria;
		this.identidadeAtual = identidadeAtual;
	}

	@Override
	@Transactional
	public UUID cadastrar(NovaPessoa nova) {
		UUID autor = identidadeAtual.obter().map(Identidade::id).orElse(null);
		Instant agora = relogio.instant();
		Pessoa pessoa = pessoas.save(new Pessoa(nova, autor, agora));
		Responsavel responsavel = responsaveis
			.save(new Responsavel(pessoa.getId(), nova.nomeResponsavel(), autor, agora));
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE, pessoa.getId()));
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE_RESPONSAVEL, responsavel.getId()));
		return pessoa.getId();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<DadosPessoa> obter(UUID id) {
		return pessoas.findById(id).map(this::dados);
	}

	@Override
	@Transactional
	public DocumentoPessoa anexarDocumento(UUID pessoaId, TipoDocumento tipo, UUID arquivoId) {
		if (!pessoas.existsById(pessoaId)) {
			throw new RecursoNaoEncontradoException("Pessoa não encontrada.");
		}
		MetadadosArquivo arquivo = arquivos.metadados(arquivoId)
			.orElseThrow(() -> new DocumentoInvalidoException("O arquivo enviado não foi encontrado. Envie de novo."));
		Optional<Documento> atual = documentos.findByPessoaIdAndTipo(pessoaId, tipo);
		if (atual.map(documento -> documento.getArquivoId().equals(arquivoId)).orElse(false)) {
			return dados(atual.get(), arquivo);
		}
		if (documentos.existsByArquivoId(arquivoId)) {
			throw new DocumentoInvalidoException("Este arquivo já está anexado a outro documento.");
		}
		UUID autor = identidadeAtual.obter().map(Identidade::id).orElse(null);
		Instant agora = relogio.instant().truncatedTo(ChronoUnit.MICROS);
		Documento documento;
		if (atual.isPresent()) {
			documento = atual.get();
			documento.trocarArquivo(arquivoId, autor, agora);
			auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE_DOCUMENTO, documento.getId(), Set.of("arquivo_id")));
		}
		else {
			documento = documentos.save(new Documento(pessoaId, tipo, arquivoId, autor, agora));
			auditoria.registrar(EventoAuditoria.criacao(ENTIDADE_DOCUMENTO, documento.getId()));
		}
		return dados(documento, arquivo);
	}

	@Override
	@Transactional(readOnly = true)
	public List<DocumentoPessoa> documentos(UUID pessoaId) {
		return documentos.findByPessoaIdOrderByTipo(pessoaId)
			.stream()
			.map(documento -> dados(documento, arquivos.metadados(documento.getArquivoId())
				.orElseThrow(() -> new IllegalStateException("Documento sem arquivo: " + documento.getId()))))
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public ConteudoArquivo lerDocumento(UUID pessoaId, TipoDocumento tipo) {
		Documento documento = documentos.findByPessoaIdAndTipo(pessoaId, tipo)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Documento não encontrado."));
		ConteudoArquivo conteudo = arquivos.ler(documento.getArquivoId());
		auditoria.registrar(EventoAuditoria.leituraSensivel(ENTIDADE_DOCUMENTO, documento.getId()));
		return conteudo;
	}

	private static DocumentoPessoa dados(Documento documento, MetadadosArquivo arquivo) {
		return new DocumentoPessoa(documento.getId(), documento.getTipo(), arquivo.id(), arquivo.nomeOriginal(),
				arquivo.tipo().tipoMime(), arquivo.tamanho(), documento.getAtualizadoEm());
	}

	private DadosPessoa dados(Pessoa pessoa) {
		String responsavel = responsaveis.findFirstByPessoaIdOrderByCriadoEmAsc(pessoa.getId())
			.map(Responsavel::getNomeCompleto)
			.orElse(null);
		return new DadosPessoa(pessoa.getId(), pessoa.getNomeCompleto(), pessoa.getDataNascimento(),
				pessoa.getTipoDeficiencia(), pessoa.getTelefone(), responsavel);
	}

}
