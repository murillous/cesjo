package br.org.cesjo.sgi.pessoas;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Dados pessoais completos do usuário atendido (RF007, ADR 0140), além dos do cadastro na lista de
 * espera. Este record é a lista de campos: um campo novo entra aqui, na entidade, na migração e no
 * formulário. Todos são opcionais. Crença e saúde ficam à parte, em {@link DadosSensiveis}.
 *
 * <p>
 * CPF, NIS e RG são guardados cifrados (ADR 0141); os demais ficam em claro para busca e dashboards.
 * O construtor tira espaços, troca texto em branco por nulo e guarda CPF, NIS e CEP só com os dígitos.
 *
 * @param rendaFamiliar renda mensal da família, em reais, com duas casas
 * @param recebeBpc recebe o Benefício de Prestação Continuada
 * @param inscritoCadUnico está inscrito no Cadastro Único
 * @param recebeBolsaFamilia recebe o Bolsa Família
 */
// PENDENTE-CESJO(P6): campos provisórios do cadastro completo (ADR 0140).
public record CadastroComplementar(Genero genero, String cpf, String nis, String rg, Escolaridade escolaridade,
		String cep, String logradouro, String numero, String complemento, String bairro, String cidade,
		BigDecimal rendaFamiliar, boolean recebeBpc, boolean inscritoCadUnico, boolean recebeBolsaFamilia) {

	/** Cadastro sem nenhum dado complementar, para quem ainda não foi completado. */
	public static final CadastroComplementar VAZIO = new CadastroComplementar(null, null, null, null, null, null,
			null, null, null, null, null, null, false, false, false);

	public CadastroComplementar {
		cpf = digitos(cpf);
		nis = digitos(nis);
		rg = texto(rg);
		cep = digitos(cep);
		logradouro = texto(logradouro);
		numero = texto(numero);
		complemento = texto(complemento);
		bairro = texto(bairro);
		cidade = texto(cidade);
		rendaFamiliar = rendaFamiliar == null ? null : rendaFamiliar.setScale(2, RoundingMode.HALF_UP);
	}

	static String texto(String valor) {
		return valor == null || valor.isBlank() ? null : valor.strip();
	}

	private static String digitos(String valor) {
		String limpo = texto(valor);
		return limpo == null ? null : limpo.replaceAll("\\D", "");
	}

}
