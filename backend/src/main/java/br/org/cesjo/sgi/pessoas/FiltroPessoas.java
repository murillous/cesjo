package br.org.cesjo.sgi.pessoas;

/**
 * Filtro da busca em lote de {@link Pessoas#resumos}. Campo nulo não filtra.
 *
 * @param nome trecho do nome, sem diferenciar maiúsculas
 * @param tipoDeficiencia tipo de deficiência
 */
public record FiltroPessoas(String nome, TipoDeficiencia tipoDeficiencia) {

	public static final FiltroPessoas NENHUM = new FiltroPessoas(null, null);

	public FiltroPessoas {
		nome = nome == null || nome.isBlank() ? null : nome.strip();
	}

	public boolean vazio() {
		return nome == null && tipoDeficiencia == null;
	}

}
