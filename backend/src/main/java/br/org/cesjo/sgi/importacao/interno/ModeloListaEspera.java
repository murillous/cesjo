package br.org.cesjo.sgi.importacao.interno;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import br.org.cesjo.sgi.listaespera.NovoCandidato;

/**
 * Modelo da planilha da lista de espera atual (seção 5.2, issue #33, ADR 0215). Colunas e regras são as
 * do cadastro pela tela (RF001, ADR 0038), com a data de cadastro original, que define a ordem da fila
 * (ADR 0057).
 */
@Component
class ModeloListaEspera implements ModeloPlanilha {

	// PENDENTE-CESJO(P13): colunas, títulos e valores aceitos supostos até a equipe mandar a planilha que usa
	// hoje (ADR 0215). Para trocar, mude só esta classe: os títulos de COLUNAS e a conversão em converter().

	static final Coluna NOME = Coluna.obrigatoria("Nome completo", "Nome da pessoa com deficiência, até 150 caracteres.",
			"Maria Exemplo da Silva");

	static final Coluna NASCIMENTO = Coluna.opcional("Data de nascimento", "Data no formato dd/mm/aaaa. Pode ficar vazia.",
			"15/03/1990");

	static final Coluna TIPO = Coluna.opcoes("Tipo de deficiência", "Intelectual", CamposCandidato.TIPOS.rotulos());

	static final Coluna TELEFONE = Coluna.obrigatoria("Telefone", "DDD e número, com 10 ou 11 dígitos, com ou sem máscara.",
			"(31) 98765-4321");

	static final Coluna RESPONSAVEL = Coluna.obrigatoria("Nome do responsável",
			"Nome do responsável pela pessoa, até 150 caracteres.", "José Exemplo da Silva");

	static final Coluna ORIGEM = Coluna.opcoes("Origem do encaminhamento", "CRAS", CamposCandidato.ORIGENS.rotulos());

	static final Coluna CADASTRO = Coluna.obrigatoria("Data de cadastro",
			"Data em que a pessoa entrou na lista de espera, no formato dd/mm/aaaa. Define a ordem da fila.",
			"02/01/2024");

	static final List<Coluna> COLUNAS = List.of(NOME, NASCIMENTO, TIPO, TELEFONE, RESPONSAVEL, ORIGEM, CADASTRO);

	private static final CamposCandidato CAMPOS = new CamposCandidato(NOME, NASCIMENTO, TIPO, TELEFONE, RESPONSAVEL,
			ORIGEM, CADASTRO);

	@Override
	public String titulo() {
		return "Lista de espera";
	}

	@Override
	public String nomeArquivo() {
		return "modelo-lista-espera.xlsx";
	}

	@Override
	public List<Coluna> colunas() {
		return COLUNAS;
	}

	/** Converte a linha no candidato, com as regras do cadastro pela tela. Devolve {@code null} se houver erro. */
	NovoCandidato converter(LinhaPlanilha linha, LocalDate hoje) {
		return CAMPOS.ler(linha, hoje, null);
	}

}
