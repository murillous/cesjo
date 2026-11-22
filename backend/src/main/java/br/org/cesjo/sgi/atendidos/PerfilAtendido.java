package br.org.cesjo.sgi.atendidos;

import java.time.LocalDate;
import java.util.Optional;

import br.org.cesjo.sgi.pessoas.Genero;
import br.org.cesjo.sgi.pessoas.TipoDeficiencia;

/**
 * Perfil de um usuário atendido para os dashboards (RF017, ADR 0195): só as categorias que entram nas
 * contagens, sem nome, id nem contato.
 *
 * @param genero {@code null} quando o cadastro completo ainda não informa
 * @param dataNascimento {@code null} quando não foi informada
 */
public record PerfilAtendido(Turno turno, TipoDeficiencia tipoDeficiencia, Genero genero, LocalDate dataNascimento) {

	/** Faixa etária em {@code hoje}, vazia sem data de nascimento ou fora de 18 a 59 anos. */
	public Optional<FaixaEtaria> faixaEtaria(LocalDate hoje) {
		return FaixaEtaria.de(dataNascimento, hoje);
	}

}
