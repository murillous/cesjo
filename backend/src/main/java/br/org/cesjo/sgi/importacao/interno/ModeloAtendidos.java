package br.org.cesjo.sgi.importacao.interno;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import br.org.cesjo.sgi.atendidos.Turno;
import br.org.cesjo.sgi.listaespera.NovoCandidato;

/**
 * Modelo da planilha dos usuários atendidos atuais (seção 5.2, issue #62, ADR 0220). Cada linha é uma
 * pessoa que já frequenta o Centro-Dia: os dados do cadastro na lista de espera (RF001) e os da inserção
 * no serviço (RF007).
 */
@Component
class ModeloAtendidos implements ModeloPlanilha {

	// PENDENTE-CESJO(P13): colunas, títulos e valores aceitos supostos até a equipe mandar a planilha de usuários
	// que usa hoje (ADR 0220). Para trocar, mude só esta classe: os títulos de COLUNAS e a conversão em converter().

	static final Opcoes<Turno> TURNOS = Opcoes.de(Turno.class, Turno::rotulo);

	static final Coluna NOME = ModeloListaEspera.NOME;

	static final Coluna RESPONSAVEL = Coluna.obrigatoria("Nome do cuidador responsável",
			"Nome do cuidador familiar responsável pela pessoa, até 150 caracteres.", "José Exemplo da Silva");

	static final Coluna CADASTRO = Coluna.opcional("Data de cadastro na lista de espera",
			"Data em que a pessoa entrou na lista de espera, no formato dd/mm/aaaa. Vazia, vale a data de inserção.",
			"02/01/2023");

	static final Coluna INSERCAO = Coluna.obrigatoria("Data de inserção",
			"Data em que a pessoa começou a frequentar o serviço, no formato dd/mm/aaaa.", "01/03/2024");

	static final Coluna TURNO = Coluna.opcoes("Turno", "Matutino", TURNOS.rotulos());

	static final List<Coluna> COLUNAS = List.of(NOME, ModeloListaEspera.NASCIMENTO, ModeloListaEspera.TIPO,
			ModeloListaEspera.TELEFONE, RESPONSAVEL, ModeloListaEspera.ORIGEM, CADASTRO, INSERCAO, TURNO);

	private static final CamposCandidato CAMPOS = new CamposCandidato(NOME, ModeloListaEspera.NASCIMENTO,
			ModeloListaEspera.TIPO, ModeloListaEspera.TELEFONE, RESPONSAVEL, ModeloListaEspera.ORIGEM, CADASTRO);

	/** Uma linha válida: o candidato a cadastrar na lista e a inserção no serviço. */
	record AtendidoImportado(NovoCandidato candidato, LocalDate dataInsercao, Turno turno) {
	}

	@Override
	public String titulo() {
		return "Usuários atendidos";
	}

	@Override
	public String nomeArquivo() {
		return "modelo-usuarios-atendidos.xlsx";
	}

	@Override
	public List<Coluna> colunas() {
		return COLUNAS;
	}

	/** Converte a linha, com as regras do cadastro e da inserção pela tela. Devolve {@code null} se houver erro. */
	AtendidoImportado converter(LinhaPlanilha linha, LocalDate hoje) {
		LocalDate dataInsercao = linha.data(INSERCAO);
		if (dataInsercao != null && dataInsercao.isAfter(hoje)) {
			linha.erro("Data de inserção: não pode ser futura.");
		}
		NovoCandidato candidato = CAMPOS.ler(linha, hoje, dataInsercao);
		Turno turno = linha.opcao(TURNO, TURNOS);
		if (candidato != null && dataInsercao != null && dataInsercao.isBefore(candidato.dataCadastro())) {
			linha.erro("Data de inserção: não pode ser anterior à data de cadastro na lista de espera.");
		}
		if (!linha.valida()) {
			return null;
		}
		return new AtendidoImportado(candidato, dataInsercao, turno);
	}

}
