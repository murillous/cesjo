package br.org.cesjo.sgi.listaespera.interno;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.arquivos.ConteudoArquivo;
import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.seguranca.Identidade;
import br.org.cesjo.sgi.compartilhado.seguranca.IdentidadeAtual;
import br.org.cesjo.sgi.compartilhado.web.RecursoNaoEncontradoException;
import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;
import br.org.cesjo.sgi.listaespera.MotivoSaida;
import br.org.cesjo.sgi.listaespera.SituacaoInscricao;
import br.org.cesjo.sgi.pessoas.DadosPessoa;
import br.org.cesjo.sgi.pessoas.DocumentoPessoa;
import br.org.cesjo.sgi.pessoas.NovaPessoa;
import br.org.cesjo.sgi.pessoas.Pessoas;
import br.org.cesjo.sgi.pessoas.TipoDocumento;

/** Cadastro, ficha, edição, situação e anexos das inscrições na lista de espera (RF001, RF002). */
@Service
class ServicoListaEspera {

	static final String ENTIDADE = "inscricao_lista_espera";

	static final String ENTIDADE_HISTORICO = "historico_situacao_inscricao";

	private final InscricaoRepositorio inscricoes;

	private final HistoricoSituacaoRepositorio historicos;

	private final FilaListaEspera fila;

	private final Pessoas pessoas;

	private final Auditoria auditoria;

	private final IdentidadeAtual identidadeAtual;

	private final Clock relogio = Clock.systemUTC();

	ServicoListaEspera(InscricaoRepositorio inscricoes, HistoricoSituacaoRepositorio historicos, FilaListaEspera fila,
			Pessoas pessoas, Auditoria auditoria, IdentidadeAtual identidadeAtual) {
		this.inscricoes = inscricoes;
		this.historicos = historicos;
		this.fila = fila;
		this.pessoas = pessoas;
		this.auditoria = auditoria;
		this.identidadeAtual = identidadeAtual;
	}

	/**
	 * Cadastra a pessoa e a inscreve na fila, com situação aguardando, e anexa os documentos informados,
	 * numa transação só. A entrada na fila é a primeira linha do histórico.
	 */
	@Transactional
	InscricaoResposta inscrever(NovaInscricao nova) {
		UUID pessoaId = pessoas.cadastrar(new NovaPessoa(nova.nomeCompleto(), nova.dataNascimento(),
				nova.tipoDeficiencia(), nova.telefone(), nova.nomeResponsavel()));
		UUID autor = autor();
		Instant agora = agora();
		InscricaoListaEspera inscricao = inscricoes
			.save(new InscricaoListaEspera(pessoaId, nova.dataCadastro(), nova.origemEncaminhamento(), autor, agora));
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE, inscricao.getId()));
		registrarHistorico(new HistoricoSituacao(inscricao.getId(), null, SituacaoInscricao.AGUARDANDO,
				nova.dataCadastro(), null, null, autor, agora));
		if (nova.laudoMedicoArquivoId() != null) {
			pessoas.anexarDocumento(pessoaId, TipoDocumento.LAUDO_MEDICO, nova.laudoMedicoArquivoId());
		}
		if (nova.comprovanteResidenciaArquivoId() != null) {
			pessoas.anexarDocumento(pessoaId, TipoDocumento.COMPROVANTE_RESIDENCIA,
					nova.comprovanteResidenciaArquivoId());
		}
		return resposta(inscricao);
	}

	@Transactional(readOnly = true)
	InscricaoResposta obter(UUID id) {
		return resposta(inscricao(id));
	}

	/** Edita os dados da pessoa e da inscrição (ADR 0090). A auditoria leva só os nomes dos campos. */
	@Transactional
	InscricaoResposta editar(UUID id, EdicaoInscricao edicao) {
		InscricaoListaEspera inscricao = inscricao(id);
		pessoas.atualizar(inscricao.getPessoaId(), edicao.pessoa());
		Set<String> campos = inscricao.atualizar(edicao.origemEncaminhamento(), edicao.dataCadastro(), autor(), agora());
		if (!campos.isEmpty()) {
			auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE, id, campos));
		}
		return resposta(inscricao);
	}

	/**
	 * Muda a situação da inscrição e registra no histórico (ADR 0091). Daqui só se sai da fila ou se
	 * volta para ela; a inserção no serviço tem caminho próprio.
	 */
	@Transactional
	InscricaoResposta mudarSituacao(UUID id, MudancaSituacao mudanca) {
		InscricaoListaEspera inscricao = inscricao(id);
		SituacaoInscricao anterior = inscricao.getSituacao();
		if (mudanca.data().isBefore(inscricao.getDataCadastro())) {
			throw new RegraNegocioException("A data da mudança não pode ser anterior à data de cadastro.");
		}
		UUID autor = autor();
		Instant agora = agora();
		MotivoSaida motivo = null;
		// PENDENTE-CESJO(P4): só sai da fila quem está ativo (aguardando ou em avaliação), e quem saiu pode
		// voltar para a posição original, com observação (ADR 0091).
		switch (mudanca.situacao()) {
			case SAIU -> {
				if (!anterior.ativa()) {
					throw new RegraNegocioException("Só sai da lista quem está na fila.");
				}
				if (mudanca.motivoSaida() == null) {
					throw new RegraNegocioException("Informe o motivo da saída.");
				}
				if (mudanca.motivoSaida() == MotivoSaida.OUTRO && mudanca.observacao() == null) {
					throw new RegraNegocioException("Descreva o motivo da saída na observação.");
				}
				motivo = mudanca.motivoSaida();
				inscricao.sair(motivo, mudanca.data(), autor, agora);
			}
			case AGUARDANDO -> {
				if (anterior != SituacaoInscricao.SAIU) {
					throw new RegraNegocioException("Só volta para a fila quem saiu da lista.");
				}
				if (mudanca.observacao() == null) {
					throw new RegraNegocioException("Explique na observação por que o candidato volta para a fila.");
				}
				inscricao.voltarParaFila(autor, agora);
			}
			case INSERIDO -> throw new RegraNegocioException(
					"A inserção no serviço é registrada no cadastro do usuário atendido.");
			case EM_AVALIACAO -> throw new RegraNegocioException(
					"O candidato fica em avaliação quando a visita domiciliar é agendada.");
		}
		auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE, id, Set.of("situacao", "motivo_saida", "data_saida")));
		registrarHistorico(new HistoricoSituacao(id, anterior, mudanca.situacao(), mudanca.data(), motivo,
				mudanca.observacao(), autor, agora));
		return resposta(inscricao);
	}

	/** Anexa ou substitui um documento da pessoa inscrita e devolve a inscrição atualizada. */
	@Transactional
	InscricaoResposta anexar(UUID id, TipoDocumento tipo, UUID arquivoId) {
		InscricaoListaEspera inscricao = inscricao(id);
		pessoas.anexarDocumento(inscricao.getPessoaId(), tipo, arquivoId);
		return resposta(inscricao);
	}

	/** Conteúdo de um documento da pessoa inscrita. A leitura é auditada no módulo Pessoas. */
	@Transactional(readOnly = true)
	ConteudoArquivo documento(UUID id, TipoDocumento tipo) {
		return pessoas.lerDocumento(inscricao(id).getPessoaId(), tipo);
	}

	private void registrarHistorico(HistoricoSituacao historico) {
		historicos.save(historico);
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE_HISTORICO, historico.getId()));
	}

	private InscricaoListaEspera inscricao(UUID id) {
		return inscricoes.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Inscrição não encontrada."));
	}

	private InscricaoResposta resposta(InscricaoListaEspera inscricao) {
		inscricoes.flush();
		List<DocumentoPessoa> documentos = pessoas.documentos(inscricao.getPessoaId());
		List<HistoricoResposta> historico = historicos.findByInscricaoIdOrderByRegistradoEmAscIdAsc(inscricao.getId())
			.stream()
			.map(HistoricoResposta::de)
			.toList();
		return InscricaoResposta.de(inscricao, pessoa(inscricao.getPessoaId()), documentos,
				fila.posicao(inscricao.getId()), historico);
	}

	private DadosPessoa pessoa(UUID pessoaId) {
		return pessoas.obter(pessoaId)
			.orElseThrow(() -> new IllegalStateException("Inscrição sem pessoa: " + pessoaId));
	}

	private UUID autor() {
		return identidadeAtual.obter().map(Identidade::id).orElse(null);
	}

	private Instant agora() {
		return relogio.instant().truncatedTo(ChronoUnit.MICROS);
	}

}
