package br.org.cesjo.sgi.importacao.interno;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.atendidos.UsuarioAtivo;
import br.org.cesjo.sgi.atendidos.UsuariosAtendidos;
import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;
import br.org.cesjo.sgi.importacao.interno.ModeloAtendidos.AtendidoImportado;
import br.org.cesjo.sgi.listaespera.CandidatoAtivo;
import br.org.cesjo.sgi.listaespera.ListaEspera;

/**
 * Importação dos usuários atendidos atuais de planilha Excel (seção 5.2, issue #62, ADR 0220). Mesmo
 * fluxo da lista de espera (ADR 0215): a pré-visualização não grava nada, e a confirmação lê o arquivo de
 * novo e grava só as linhas válidas, numa transação só. Cada linha cadastra a pessoa na lista de espera,
 * com a data de cadastro da planilha, e a insere no serviço, com o responsável como cuidador.
 */
@Service
class ServicoImportacaoAtendidos {

	/** Nome da importação no log de auditoria (ADR 0217). */
	static final String ENTIDADE = "planilha_atendidos";

	private final LeitorPlanilha leitor;

	private final ModeloAtendidos modelo;

	private final ListaEspera listaEspera;

	private final UsuariosAtendidos atendidos;

	private final Auditoria auditoria;

	private final Clock relogio = Clock.system(ZoneId.of("America/Sao_Paulo"));

	ServicoImportacaoAtendidos(LeitorPlanilha leitor, ModeloAtendidos modelo, ListaEspera listaEspera,
			UsuariosAtendidos atendidos, Auditoria auditoria) {
		this.leitor = leitor;
		this.modelo = modelo;
		this.listaEspera = listaEspera;
		this.atendidos = atendidos;
		this.auditoria = auditoria;
	}

	@Transactional(readOnly = true)
	Previa previa(byte[] arquivo) {
		return analisar(arquivo).previa();
	}

	@Transactional
	ResultadoImportacao importar(byte[] arquivo) {
		Analise<AtendidoImportado> analise = analisar(arquivo);
		List<AtendidoImportado> validos = analise.validos();
		if (validos.isEmpty()) {
			throw new RegraNegocioException("Nenhuma linha da planilha está pronta para importar. Corrija os erros e envie de novo.");
		}
		for (AtendidoImportado atendido : validos) {
			var inscricaoId = listaEspera.inscrever(atendido.candidato());
			atendidos.inserirComOResponsavel(inscricaoId, atendido.dataInsercao(), atendido.turno());
		}
		auditoria.registrar(EventoAuditoria.importacao(ENTIDADE));
		return new ResultadoImportacao(validos.size(), analise.comErro());
	}

	/**
	 * Lê e converte a planilha. Recusa a linha cujo nome já é de um usuário ativo, já está na lista de
	 * espera (que se insere pela fila) ou repete uma linha anterior da planilha.
	 */
	private Analise<AtendidoImportado> analisar(byte[] arquivo) {
		List<LinhaPlanilha> linhas = leitor.ler(arquivo, modelo.colunas());
		LocalDate hoje = LocalDate.now(relogio);
		NomesRepetidos repetidos = new NomesRepetidos()
			.cadastrados(atendidos.listarAtivos().stream().map(UsuarioAtivo::nomeCompleto).toList(),
					"Já há um usuário atendido ativo com este nome.")
			.cadastrados(listaEspera.listarCandidatosAtivos().stream().map(CandidatoAtivo::nomeCompleto).toList(),
					"Esta pessoa já está na lista de espera: insira no serviço pela fila.");
		return Analise.de(modelo, linhas, linha -> {
			AtendidoImportado atendido = modelo.converter(linha, hoje);
			repetidos.conferir(linha, ModeloAtendidos.NOME);
			return linha.valida() ? atendido : null;
		});
	}

}
