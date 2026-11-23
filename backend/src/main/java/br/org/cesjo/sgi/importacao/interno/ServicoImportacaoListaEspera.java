package br.org.cesjo.sgi.importacao.interno;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;
import br.org.cesjo.sgi.listaespera.CandidatoAtivo;
import br.org.cesjo.sgi.listaespera.ListaEspera;
import br.org.cesjo.sgi.listaespera.NovoCandidato;

/**
 * Importação da lista de espera atual de planilha Excel (seção 5.2, issue #33, ADR 0215). A
 * pré-visualização não grava nada; a confirmação lê o mesmo arquivo de novo e grava só as linhas válidas,
 * numa transação só, na ordem da planilha.
 */
@Service
class ServicoImportacaoListaEspera {

	/** Nome da importação no log de auditoria (ADR 0217). */
	static final String ENTIDADE = "planilha_lista_espera";

	private final LeitorPlanilha leitor;

	private final ModeloListaEspera modelo;

	private final ListaEspera listaEspera;

	private final Auditoria auditoria;

	private final Clock relogio = Clock.system(ZoneId.of("America/Sao_Paulo"));

	ServicoImportacaoListaEspera(LeitorPlanilha leitor, ModeloListaEspera modelo, ListaEspera listaEspera,
			Auditoria auditoria) {
		this.leitor = leitor;
		this.modelo = modelo;
		this.listaEspera = listaEspera;
		this.auditoria = auditoria;
	}

	@Transactional(readOnly = true)
	Previa previa(byte[] arquivo) {
		return analisar(arquivo).previa();
	}

	@Transactional
	ResultadoImportacao importar(byte[] arquivo) {
		Analise<NovoCandidato> analise = analisar(arquivo);
		List<NovoCandidato> validos = analise.validos();
		if (validos.isEmpty()) {
			throw new RegraNegocioException("Nenhuma linha da planilha está pronta para importar. Corrija os erros e envie de novo.");
		}
		validos.forEach(listaEspera::inscrever);
		auditoria.registrar(EventoAuditoria.importacao(ENTIDADE));
		return new ResultadoImportacao(validos.size(), analise.comErro());
	}

	/**
	 * Lê e converte a planilha. Além das regras do cadastro, recusa a linha cujo nome já está na fila ou
	 * aparece numa linha anterior da planilha, para que enviar a mesma planilha duas vezes não duplique a
	 * fila (ADR 0215).
	 */
	private Analise<NovoCandidato> analisar(byte[] arquivo) {
		List<LinhaPlanilha> linhas = leitor.ler(arquivo, modelo.colunas());
		LocalDate hoje = LocalDate.now(relogio);
		NomesRepetidos repetidos = new NomesRepetidos().cadastrados(
				listaEspera.listarCandidatosAtivos().stream().map(CandidatoAtivo::nomeCompleto).toList(),
				"Já há um candidato com este nome na lista de espera.");
		return Analise.de(modelo, linhas, linha -> {
			NovoCandidato candidato = modelo.converter(linha, hoje);
			repetidos.conferir(linha, ModeloListaEspera.NOME);
			return linha.valida() ? candidato : null;
		});
	}

}
