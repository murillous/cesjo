package br.org.cesjo.sgi.acesso.interno;

import java.util.List;
import java.util.UUID;

import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.acesso.Permissao;

/** Corpo de {@code GET /api/sessao}, no formato de {@code ContaSessao} do frontend. */
record SessaoResposta(UUID id, String login, String nome, Perfil perfil, List<Permissao> permissoes,
		boolean trocaSenhaObrigatoria) {

	static SessaoResposta de(ContaPrincipal conta) {
		return new SessaoResposta(conta.id(), conta.login(), conta.nome(), conta.perfil(),
				conta.perfil().permissoes().stream().sorted().toList(), conta.trocaSenhaObrigatoria());
	}

}
