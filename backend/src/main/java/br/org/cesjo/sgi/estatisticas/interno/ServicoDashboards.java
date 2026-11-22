package br.org.cesjo.sgi.estatisticas.interno;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import br.org.cesjo.sgi.atendidos.FaixaEtaria;
import br.org.cesjo.sgi.atendidos.MotivoDesligamento;
import br.org.cesjo.sgi.atendidos.PerfilAtendido;
import br.org.cesjo.sgi.atendidos.TipoViolacao;
import br.org.cesjo.sgi.atendidos.Turno;
import br.org.cesjo.sgi.atendidos.UsuariosAtendidos;
import br.org.cesjo.sgi.atividades.Atividades;
import br.org.cesjo.sgi.atividades.TipoAtividade;
import br.org.cesjo.sgi.pessoas.Genero;
import br.org.cesjo.sgi.pessoas.TipoDeficiencia;

/**
 * Dashboards de RF017 (ADR 0195). Lê os outros módulos só pelas APIs Java públicas e recalcula a cada
 * acesso, sem cache. A permissão fica no controlador.
 */
@Service
class ServicoDashboards {

	private final UsuariosAtendidos atendidos;

	private final Atividades atividades;

	private final Clock relogio;

	ServicoDashboards(UsuariosAtendidos atendidos, Atividades atividades) {
		this.atendidos = atendidos;
		this.atividades = atividades;
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

	/**
	 * Violações por categoria, atividades por tipo e desligamentos por motivo no período (ADR 0198).
	 *
	 * @param comViolacoes falso para quem não lê violações de direitos: o gráfico de violações não vem
	 * (ADR 0199)
	 */
	PainelAtendimento atendimento(Periodo periodo, boolean comViolacoes) {
		List<Contagem> violacoes = null;
		if (comViolacoes) {
			violacoes = Contagens.doMapa(TipoViolacao.class, TipoViolacao::rotulo,
					atendidos.contarViolacoesPorTipo(periodo.inicio(), periodo.fim()));
		}
		Map<TipoAtividade, Long> porTipo = atividades.contarPorTipo(periodo.inicio(), periodo.fim());
		long totalAtividades = porTipo.values().stream().mapToLong(Long::longValue).sum();

		long atendidosNoPeriodo = atendidos.contarAtendidosNoPeriodo(periodo.inicio(), periodo.fim());
		Map<MotivoDesligamento, Long> porMotivo = atendidos.contarDesligamentosPorMotivo(periodo.inicio(),
				periodo.fim());
		List<TaxaDesligamento> desligamentos = new ArrayList<>();
		for (MotivoDesligamento motivo : MotivoDesligamento.values()) {
			long total = porMotivo.getOrDefault(motivo, 0L);
			desligamentos.add(new TaxaDesligamento(motivo.name(), motivo.rotulo(), total, taxa(total, atendidosNoPeriodo)));
		}
		long totalDesligamentos = porMotivo.values().stream().mapToLong(Long::longValue).sum();

		return new PainelAtendimento(relogio.instant(), periodo.inicio(), periodo.fim(), violacoes,
				Contagens.doMapa(TipoAtividade.class, RotulosAtividade::de, porTipo), totalAtividades,
				atendidosNoPeriodo, totalDesligamentos, taxa(totalDesligamentos, atendidosNoPeriodo),
				List.copyOf(desligamentos));
	}

	/**
	 * Taxa de desligamento: desligamentos do período sobre os usuários atendidos no período, de 0 a 1.
	 */
	// PENDENTE-CESJO(P32): a base da taxa são os usuários atendidos em algum dia do período (ADR 0198).
	static double taxa(long desligamentos, long atendidosNoPeriodo) {
		return atendidosNoPeriodo == 0 ? 0 : (double) desligamentos / atendidosNoPeriodo;
	}

}
