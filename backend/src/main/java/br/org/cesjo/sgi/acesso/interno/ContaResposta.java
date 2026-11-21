package br.org.cesjo.sgi.acesso.interno;

import java.time.Instant;
import java.util.UUID;

import br.org.cesjo.sgi.acesso.Perfil;

/** Conta como a API devolve ao Coordenador. Nunca leva a senha nem o hash. */
record ContaResposta(UUID id, String login, String nome, Perfil perfil, boolean ativa, boolean bloqueada,
		boolean trocaSenhaObrigatoria, Instant criadoEm, Instant atualizadoEm) {

	static ContaResposta de(ContaAcesso conta, Instant agora) {
		return new ContaResposta(conta.getId(), conta.getLogin(), conta.getNome(), conta.getPerfil(), conta.isAtiva(),
				conta.bloqueada(agora), conta.isTrocaSenhaObrigatoria(), conta.getCriadoEm(), conta.getAtualizadoEm());
	}

}
