package br.org.cesjo.sgi.importacao.interno;

import java.time.LocalDate;
import java.util.regex.Pattern;

import br.org.cesjo.sgi.listaespera.NovoCandidato;
import br.org.cesjo.sgi.listaespera.OrigemEncaminhamento;
import br.org.cesjo.sgi.pessoas.Telefone;
import br.org.cesjo.sgi.pessoas.TipoDeficiencia;

/**
 * Colunas do cadastro na lista de espera (RF001) dentro de um modelo de planilha, e a conversão delas
 * com as regras do cadastro pela tela (ADR 0038). Os modelos da lista de espera e dos usuários
 * atendidos usam a mesma conversão, cada um com os seus títulos (ADR 0215 e ADR 0220).
 *
 * @param cadastro data de cadastro na lista; quando opcional e vazia, vale a data padrão da linha
 */
record CamposCandidato(Coluna nome, Coluna nascimento, Coluna tipo, Coluna telefone, Coluna responsavel,
		Coluna origem, Coluna cadastro) {

	static final Opcoes<TipoDeficiencia> TIPOS = Opcoes.de(TipoDeficiencia.class, TipoDeficiencia::rotulo);

	static final Opcoes<OrigemEncaminhamento> ORIGENS = Opcoes.de(OrigemEncaminhamento.class,
			OrigemEncaminhamento::rotulo);

	private static final int TAMANHO_NOME = 150;

	private static final Pattern FORMATO_TELEFONE = Pattern.compile(Telefone.FORMATO);

	/**
	 * Converte a linha no candidato, registrando nela os erros. Devolve {@code null} se algum campo faltar
	 * ou for inválido.
	 *
	 * @param cadastroPadrao data de cadastro quando a coluna é opcional e está vazia
	 */
	NovoCandidato ler(LinhaPlanilha linha, LocalDate hoje, LocalDate cadastroPadrao) {
		String nomeCompleto = linha.texto(nome, TAMANHO_NOME);
		LocalDate dataNascimento = linha.data(nascimento);
		if (dataNascimento != null && !dataNascimento.isBefore(hoje)) {
			linha.erro(nascimento.titulo() + ": precisa ser anterior a hoje.");
		}
		TipoDeficiencia tipoDeficiencia = linha.opcao(tipo, TIPOS);
		String fone = linha.texto(telefone, 30);
		if (fone != null && !FORMATO_TELEFONE.matcher(fone).matches()) {
			linha.erro(telefone.titulo() + ": " + Telefone.MENSAGEM + ".");
		}
		String nomeResponsavel = linha.texto(responsavel, TAMANHO_NOME);
		OrigemEncaminhamento origemEncaminhamento = linha.opcao(origem, ORIGENS);
		LocalDate dataCadastro = linha.data(cadastro);
		if (dataCadastro == null && linha.exibido(cadastro).isEmpty()) {
			dataCadastro = cadastroPadrao;
		}
		if (dataCadastro != null && dataCadastro.isAfter(hoje)) {
			linha.erro(cadastro.titulo() + ": não pode ser futura.");
		}
		if (dataNascimento != null && dataCadastro != null && dataCadastro.isBefore(dataNascimento)) {
			linha.erro(cadastro.titulo() + ": não pode ser anterior à data de nascimento.");
		}
		if (!linha.valida() || dataCadastro == null) {
			return null;
		}
		return new NovoCandidato(nomeCompleto, dataNascimento, tipoDeficiencia, fone, nomeResponsavel,
				origemEncaminhamento, dataCadastro);
	}

}
