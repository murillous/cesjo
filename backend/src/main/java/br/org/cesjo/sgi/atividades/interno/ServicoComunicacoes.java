package br.org.cesjo.sgi.atividades.interno;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.arquivos.Arquivos;
import br.org.cesjo.sgi.arquivos.ConteudoArquivo;
import br.org.cesjo.sgi.arquivos.MetadadosArquivo;
import br.org.cesjo.sgi.arquivos.NovoArquivo;
import br.org.cesjo.sgi.atividades.Comunicacoes;
import br.org.cesjo.sgi.atividades.DirecaoComunicacao;
import br.org.cesjo.sgi.atividades.TipoComunicacao;
import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.seguranca.Identidade;
import br.org.cesjo.sgi.compartilhado.seguranca.IdentidadeAtual;
import br.org.cesjo.sgi.compartilhado.web.RecursoNaoEncontradoException;
import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;

/**
 * Registro, correção, anexo, listagem e contagem dos contatos e comunicações (RF011, ADRs 0160, 0161 e
 * 0164).
 */
@Service
class ServicoComunicacoes implements Comunicacoes {

	/** "Hoje" é o dia em São Paulo, mesmo com o servidor em UTC. */
	private final Clock relogio = Clock.system(ZoneId.of("America/Sao_Paulo"));

	private final ComunicacaoRepositorio comunicacoes;

	private final Arquivos arquivos;

	private final Auditoria auditoria;

	private final IdentidadeAtual identidadeAtual;

	ServicoComunicacoes(ComunicacaoRepositorio comunicacoes, Arquivos arquivos, Auditoria auditoria,
			IdentidadeAtual identidadeAtual) {
		this.comunicacoes = comunicacoes;
		this.arquivos = arquivos;
		this.auditoria = auditoria;
		this.identidadeAtual = identidadeAtual;
	}

	@Transactional
	ComunicacaoResposta registrar(DadosComunicacao dados) {
		exigirDataValida(dados.data());
		Comunicacao comunicacao = comunicacoes.save(new Comunicacao(dados, autor(), relogio.instant()));
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE, comunicacao.getId()));
		return resposta(comunicacao);
	}

	@Transactional
	ComunicacaoResposta corrigir(UUID id, DadosComunicacao dados) {
		Comunicacao comunicacao = buscar(id);
		exigirDataValida(dados.data());
		Set<String> alterados = comunicacao.corrigir(dados, autor(), relogio.instant());
		if (!alterados.isEmpty()) {
			auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE, id, alterados));
		}
		return resposta(comunicacao);
	}

	/**
	 * Guarda o arquivo cifrado no módulo Arquivos e o liga ao registro, no lugar do anterior. O arquivo
	 * anterior continua guardado, sem ligação.
	 */
	// PENDENTE-CESJO(P31): um anexo só por registro, em qualquer tipo, e sempre opcional (ADR 0164).
	@Transactional
	ComunicacaoResposta anexar(UUID id, NovoArquivo arquivo) {
		Comunicacao comunicacao = buscar(id);
		MetadadosArquivo guardado = arquivos.guardar(arquivo);
		comunicacao.anexar(guardado.id(), autor(), relogio.instant());
		auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE, id, Set.of("arquivo_id")));
		return resposta(comunicacao);
	}

	@Transactional
	ComunicacaoResposta removerAnexo(UUID id) {
		Comunicacao comunicacao = buscar(id);
		if (comunicacao.removerAnexo(autor(), relogio.instant())) {
			auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE, id, Set.of("arquivo_id")));
		}
		return resposta(comunicacao);
	}

	/** Conteúdo do anexo. O módulo Arquivos registra a leitura na auditoria. */
	@Transactional(readOnly = true)
	ConteudoArquivo lerAnexo(UUID id) {
		UUID arquivoId = buscar(id).getArquivoId();
		if (arquivoId == null) {
			throw new RecursoNaoEncontradoException("Este registro não tem anexo.");
		}
		return arquivos.ler(arquivoId);
	}

	@Transactional(readOnly = true)
	ComunicacaoResposta ler(UUID id) {
		return resposta(buscar(id));
	}

	/** Comunicações que atendem ao filtro, sem a descrição. */
	@Transactional(readOnly = true)
	Page<ResumoComunicacao> listar(FiltroComunicacoes filtro, Pageable pagina) {
		if (filtro.inicio() != null && filtro.fim() != null && filtro.fim().isBefore(filtro.inicio())) {
			throw new RegraNegocioException("O fim do período não pode ser anterior ao início.");
		}
		return comunicacoes.findAll(filtro.especificacao(), pagina)
			.map(c -> new ResumoComunicacao(c.getId(), c.getTipo(), c.getDirecao(), c.getData(), c.getContraparte(),
					c.getAssunto(), c.getArquivoId() != null));
	}

	@Override
	@Transactional(readOnly = true)
	public Map<TipoComunicacao, Long> contarPorTipo(LocalDate inicio, LocalDate fim) {
		exigirPeriodo(inicio, fim);
		Map<TipoComunicacao, Long> contagem = new EnumMap<>(TipoComunicacao.class);
		for (TipoComunicacao tipo : TipoComunicacao.values()) {
			contagem.put(tipo, 0L);
		}
		for (Object[] linha : comunicacoes.contarPorTipo(inicio, fim)) {
			contagem.put((TipoComunicacao) linha[0], (Long) linha[1]);
		}
		return contagem;
	}

	@Override
	@Transactional(readOnly = true)
	public Map<DirecaoComunicacao, Long> contarPorDirecao(TipoComunicacao tipo, LocalDate inicio, LocalDate fim) {
		exigirPeriodo(inicio, fim);
		if (!tipo.temDirecao()) {
			throw new IllegalArgumentException("O tipo " + tipo + " não tem direção.");
		}
		Map<DirecaoComunicacao, Long> contagem = new EnumMap<>(DirecaoComunicacao.class);
		for (DirecaoComunicacao direcao : DirecaoComunicacao.values()) {
			contagem.put(direcao, 0L);
		}
		for (Object[] linha : comunicacoes.contarPorDirecao(tipo, inicio, fim)) {
			contagem.put((DirecaoComunicacao) linha[0], (Long) linha[1]);
		}
		return contagem;
	}

	private static void exigirPeriodo(LocalDate inicio, LocalDate fim) {
		if (fim.isBefore(inicio)) {
			throw new IllegalArgumentException("O fim do período não pode ser anterior ao início.");
		}
	}

	// PENDENTE-CESJO(P30): só se registra contato ou comunicação que já aconteceu; a data não pode ser futura
	// (ADR 0160).
	private void exigirDataValida(LocalDate data) {
		if (data.isAfter(LocalDate.now(relogio))) {
			throw new RegraNegocioException("A data não pode ser futura.");
		}
	}

	private ComunicacaoResposta resposta(Comunicacao c) {
		AnexoComunicacao anexo = c.getArquivoId() == null ? null
				: arquivos.metadados(c.getArquivoId()).map(AnexoComunicacao::de).orElse(null);
		return new ComunicacaoResposta(c.getId(), c.getTipo(), c.getDirecao(), c.getData(), c.getContraparte(),
				c.getAssunto(), c.getDescricao(), anexo, c.getCriadoEm(), c.getAtualizadoEm());
	}

	private Comunicacao buscar(UUID id) {
		return comunicacoes.findById(id)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Contato ou comunicação não encontrado."));
	}

	private UUID autor() {
		return identidadeAtual.obter().map(Identidade::id).orElse(null);
	}

}
