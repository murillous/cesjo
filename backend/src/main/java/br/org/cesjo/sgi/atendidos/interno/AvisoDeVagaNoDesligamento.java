package br.org.cesjo.sgi.atendidos.interno;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import br.org.cesjo.sgi.atendidos.UsuarioDesligado;
import br.org.cesjo.sgi.listaespera.AberturaDeVaga;

/**
 * Ouve {@link UsuarioDesligado} e avisa a lista de espera da vaga liberada (RF003, RF008, ADR 0111 e
 * ADR 0135). Ouvinte síncrono: roda na transação do desligamento, depois de a inserção já estar
 * desligada, e a notificação é desfeita junto se o desligamento falhar.
 */
@Component
class AvisoDeVagaNoDesligamento {

	private final AberturaDeVaga aberturaDeVaga;

	AvisoDeVagaNoDesligamento(AberturaDeVaga aberturaDeVaga) {
		this.aberturaDeVaga = aberturaDeVaga;
	}

	@EventListener
	void usuarioDesligado(UsuarioDesligado evento) {
		aberturaDeVaga.vagasLiberadas(1);
	}

}
