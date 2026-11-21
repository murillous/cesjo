package br.org.cesjo.sgi.acesso.interno;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.acesso.Contas;
import br.org.cesjo.sgi.acesso.ResumoConta;

@Service
class ConsultaContas implements Contas {

	private final ContaAcessoRepositorio contas;

	ConsultaContas(ContaAcessoRepositorio contas) {
		this.contas = contas;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ResumoConta> obter(UUID id) {
		return contas.findById(id)
			.map(conta -> new ResumoConta(conta.getId(), conta.getLogin(), conta.getNome(), conta.isAtiva()));
	}

}
