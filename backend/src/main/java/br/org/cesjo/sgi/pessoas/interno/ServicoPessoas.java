package br.org.cesjo.sgi.pessoas.interno;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
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
import br.org.cesjo.sgi.pessoas.CadastroComplementar;
import br.org.cesjo.sgi.pessoas.DadosPessoa;
import br.org.cesjo.sgi.pessoas.DadosSensiveis;
import br.org.cesjo.sgi.pessoas.DocumentoInvalidoException;
import br.org.cesjo.sgi.pessoas.DocumentoPessoa;
import br.org.cesjo.sgi.pessoas.FiltroPessoas;
import br.org.cesjo.sgi.pessoas.DadosResponsavel;
import br.org.cesjo.sgi.pessoas.NovaPessoa;
import br.org.cesjo.sgi.pessoas.Pessoas;
import br.org.cesjo.sgi.pessoas.ResumoPessoa;
import br.org.cesjo.sgi.pessoas.TipoDocumento;

@Service
class ServicoPessoas implements Pessoas {

	/** Ids por consulta na busca em lote, para não passar do limite de parâmetros do banco. */
	static final int LOTE = 1000;

	private static final char ESCAPE = '!';

	private final PessoaRepositorio pessoas;

	private final ResponsavelRepositorio responsaveis;

	private final DocumentoRepositorio documentos;

	private final CadastroComplementarRepositorio cadastros;

	private final Arquivos arquivos;

	private final Auditoria auditoria;

	private final IdentidadeAtual identidadeAtual;

	private final Clock relogio = Clock.systemUTC();

	ServicoPessoas(PessoaRepositorio pessoas, ResponsavelRepositorio responsaveis, DocumentoRepositorio documentos,
			CadastroComplementarRepositorio cadastros, Arquivos arquivos, Auditoria auditoria, IdentidadeAtual identidadeAtual) {
		this.pessoas = pessoas;
		this.responsaveis = responsaveis;
		this.documentos = documentos;
		this.cadastros = cadastros;
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
	@Transactional
	public DadosPessoa atualizar(UUID id, NovaPessoa dados) {
		Pessoa pessoa = pessoas.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada."));
		UUID autor = identidadeAtual.obter().map(Identidade::id).orElse(null);
		Instant agora = relogio.instant();
		Set<String> campos = pessoa.atualizar(dados, autor, agora);
		if (!campos.isEmpty()) {
			auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE, id, campos));
		}
		Optional<Responsavel> responsavel = responsaveis.findFirstByPessoaIdOrderByCriadoEmAsc(id);
		if (responsavel.isPresent()) {
			if (responsavel.get().renomear(dados.nomeResponsavel(), autor, agora)) {
				auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE_RESPONSAVEL, responsavel.get().getId(),
						Set.of("nome_completo")));
			}
		}
		else {
			Responsavel novo = responsaveis.save(new Responsavel(id, dados.nomeResponsavel(), autor, agora));
			auditoria.registrar(EventoAuditoria.criacao(ENTIDADE_RESPONSAVEL, novo.getId()));
		}
		return dados(pessoa);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<DadosPessoa> obter(UUID id) {
		return pessoas.findById(id).map(this::dados);
	}

	@Override
	@Transactional(readOnly = true)
	public Map<UUID, String> nomes(Collection<UUID> ids) {
		return pessoas.findAllById(ids).stream().collect(Collectors.toMap(Pessoa::getId, Pessoa::getNomeCompleto));
	}

	@Override
	@Transactional(readOnly = true)
	public List<DadosResponsavel> responsaveis(UUID pessoaId) {
		List<DadosResponsavel> dados = new ArrayList<>();
		for (Responsavel responsavel : responsaveis.findByPessoaIdOrderByCriadoEmAscIdAsc(pessoaId)) {
			dados.add(new DadosResponsavel(responsavel.getId(), pessoaId, responsavel.getNomeCompleto(),
					dados.isEmpty()));
		}
		return dados;
	}

	@Override
	@Transactional
	public DadosResponsavel cadastrarResponsavel(UUID pessoaId, String nomeCompleto) {
		if (!pessoas.existsById(pessoaId)) {
			throw new IllegalArgumentException("Pessoa não encontrada: " + pessoaId);
		}
		boolean principal = responsaveis.findFirstByPessoaIdOrderByCriadoEmAsc(pessoaId).isEmpty();
		UUID autor = identidadeAtual.obter().map(Identidade::id).orElse(null);
		Responsavel responsavel = responsaveis
			.save(new Responsavel(pessoaId, nomeCompleto.strip(), autor, relogio.instant()));
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE_RESPONSAVEL, responsavel.getId()));
		return new DadosResponsavel(responsavel.getId(), pessoaId, responsavel.getNomeCompleto(), principal);
	}

	@Override
	@Transactional(readOnly = true)
	public Map<UUID, ResumoPessoa> resumos(Collection<UUID> ids, FiltroPessoas filtro) {
		List<UUID> lista = ids.stream().distinct().toList();
		Map<UUID, ResumoPessoa> resumos = new HashMap<>();
		for (int inicio = 0; inicio < lista.size(); inicio += LOTE) {
			List<UUID> lote = lista.subList(inicio, Math.min(inicio + LOTE, lista.size()));
			List<Pessoa> encontradas = pessoas.findAll(filtrar(lote, filtro));
			Map<UUID, Set<TipoDocumento>> tipos = new HashMap<>();
			documentos.findByPessoaIdIn(encontradas.stream().map(Pessoa::getId).toList())
				.forEach(documento -> tipos.computeIfAbsent(documento.getPessoaId(), id -> EnumSet.noneOf(TipoDocumento.class))
					.add(documento.getTipo()));
			for (Pessoa pessoa : encontradas) {
				resumos.put(pessoa.getId(), new ResumoPessoa(pessoa.getId(), pessoa.getNomeCompleto(),
						pessoa.getTipoDeficiencia(), tipos.getOrDefault(pessoa.getId(), Set.of())));
			}
		}
		return resumos;
	}

	private static Specification<Pessoa> filtrar(Collection<UUID> ids, FiltroPessoas filtro) {
		return (raiz, consulta, cb) -> {
			List<Predicate> condicoes = new ArrayList<>();
			condicoes.add(raiz.get("id").in(ids));
			if (filtro.tipoDeficiencia() != null) {
				condicoes.add(cb.equal(raiz.get("tipoDeficiencia"), filtro.tipoDeficiencia()));
			}
			if (filtro.nome() != null) {
				String padrao = "%" + escaparLike(filtro.nome().toLowerCase(Locale.ROOT)) + "%";
				condicoes.add(cb.like(cb.lower(raiz.get("nomeCompleto")), padrao, ESCAPE));
			}
			return cb.and(condicoes.toArray(Predicate[]::new));
		};
	}

	/** Busca por nome é por trecho: {@code %} e {@code _} digitados valem como texto. */
	private static String escaparLike(String texto) {
		return texto.replace(String.valueOf(ESCAPE), ESCAPE + "" + ESCAPE)
			.replace("%", ESCAPE + "%")
			.replace("_", ESCAPE + "_");
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
			// PENDENTE-CESJO(P14): o arquivo substituído continua guardado no módulo Arquivos, sem ligação
			// com a pessoa, até a equipe definir a regra de retenção (ADR 0055).
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

	@Override
	@Transactional(readOnly = true)
	public CadastroComplementar cadastroComplementar(UUID pessoaId) {
		return cadastros.findById(pessoaId)
			.map(CadastroComplementarPessoa::complementar)
			.orElse(CadastroComplementar.VAZIO);
	}

	@Override
	@Transactional(readOnly = true)
	public DadosSensiveis dadosSensiveis(UUID pessoaId) {
		DadosSensiveis dados = cadastros.findById(pessoaId)
			.map(CadastroComplementarPessoa::sensiveis)
			.orElse(DadosSensiveis.VAZIO);
		if (!dados.vazio()) {
			auditoria.registrar(EventoAuditoria.leituraSensivel(ENTIDADE_CADASTRO, pessoaId));
		}
		return dados;
	}

	@Override
	@Transactional
	public void atualizarCadastroComplementar(UUID pessoaId, CadastroComplementar dados) {
		gravarCadastro(pessoaId, (cadastro, autor, agora) -> cadastro.atualizar(dados, autor, agora));
	}

	@Override
	@Transactional
	public void atualizarDadosSensiveis(UUID pessoaId, DadosSensiveis dados) {
		gravarCadastro(pessoaId, (cadastro, autor, agora) -> cadastro.atualizar(dados, autor, agora));
	}

	/** Cria a linha do cadastro na primeira gravação e audita só o que mudou. */
	private void gravarCadastro(UUID pessoaId, AlteracaoCadastro alteracao) {
		if (!pessoas.existsById(pessoaId)) {
			throw new RecursoNaoEncontradoException("Pessoa não encontrada.");
		}
		UUID autor = identidadeAtual.obter().map(Identidade::id).orElse(null);
		Instant agora = relogio.instant();
		Optional<CadastroComplementarPessoa> atual = cadastros.findById(pessoaId);
		CadastroComplementarPessoa cadastro = atual.orElseGet(() -> new CadastroComplementarPessoa(pessoaId, autor, agora));
		Set<String> campos = alteracao.aplicar(cadastro, autor, agora);
		if (atual.isEmpty()) {
			cadastros.save(cadastro);
			auditoria.registrar(EventoAuditoria.criacao(ENTIDADE_CADASTRO, pessoaId));
		}
		else if (!campos.isEmpty()) {
			auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE_CADASTRO, pessoaId, campos));
		}
	}

	@FunctionalInterface
	private interface AlteracaoCadastro {

		Set<String> aplicar(CadastroComplementarPessoa cadastro, UUID autor, Instant agora);

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
