package br.org.cesjo.sgi.estatisticas.interno;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import br.org.cesjo.sgi.atendidos.FaixaEtaria;
import br.org.cesjo.sgi.atendidos.PerfilAtendido;
import br.org.cesjo.sgi.atendidos.Turno;
import br.org.cesjo.sgi.atendidos.UsuariosAtendidos;
import br.org.cesjo.sgi.pessoas.Genero;
import br.org.cesjo.sgi.pessoas.TipoDeficiencia;

/**
 * Dashboards de RF017 (ADR 0195). Lê os outros módulos só pelas APIs Java públicas e recalcula a cada
 * acesso, sem cache. A permissão fica no controlador.
 */
@Service
class ServicoDashboards {

	private final UsuariosAtendidos atendidos;

	private final Clock relogio;

	ServicoDashboards(UsuariosAtendidos atendidos) {
		this.atendidos = atendidos;
		this.relogio = Clock.systemUTC();
	}

	PainelPerfil perfil() {
		Instant agora = relogio.instant();
		LocalDate hoje = LocalDate.ofInstant(agora, Fuso.CENTRO_DIA);
		List<PerfilAtendido> perfis = atendidos.perfisDosAtivos();
		return new PainelPerfil(agora, perfis.size(),
				Contagens.porCategoria(TipoDeficiencia.class, TipoDeficiencia::rotulo,
						perfis.stream().map(PerfilAtendido::tipoDeficiencia).toList(), "Não informado"),
				Contagens.porCategoria(Genero.class, Genero::rotulo,
						perfis.stream().map(PerfilAtendido::genero).toList(), "Não informado"),
				Contagens.porCategoria(FaixaEtaria.class, FaixaEtaria::rotulo,
						perfis.stream().map(perfil -> perfil.faixaEtaria(hoje).orElse(null)).toList(),
						"Sem data de nascimento ou fora de 18 a 59 anos"),
				Contagens.porCategoria(Turno.class, Turno::rotulo, perfis.stream().map(PerfilAtendido::turno).toList(),
						"Não informado"));
	}

}
