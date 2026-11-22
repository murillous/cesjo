package br.org.cesjo.sgi.atendidos.interno;

import org.springframework.stereotype.Component;

import br.org.cesjo.sgi.atendidos.UsuariosAtendidos;
import br.org.cesjo.sgi.listaespera.OcupacaoDeVagas;

/**
 * Vagas ocupadas para o painel de vagas da lista de espera (RF003, ADR 0135): as inserções ativas. Um
 * desligamento libera a vaga na hora, porque a inserção deixa de ser ativa.
 */
@Component
class OcupacaoPelosAtendidosAtivos implements OcupacaoDeVagas {

	private final UsuariosAtendidos usuariosAtendidos;

	OcupacaoPelosAtendidosAtivos(UsuariosAtendidos usuariosAtendidos) {
		this.usuariosAtendidos = usuariosAtendidos;
	}

	@Override
	public long vagasOcupadas() {
		return usuariosAtendidos.contarAtivos();
	}

}
