package br.org.cesjo.sgi.atendidos.interno;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.compartilhado.web.RecursoNaoEncontradoException;
import br.org.cesjo.sgi.pessoas.DadosPessoa;
import br.org.cesjo.sgi.pessoas.NovaPessoa;
import br.org.cesjo.sgi.pessoas.Pessoas;

/**
 * Completar e corrigir o cadastro do usuário atendido (RF007, ADR 0140). Os dados são da pessoa e
 * ficam no módulo Pessoas, que também audita. A permissão é conferida no controlador.
 */
@Service
class ServicoCadastroAtendido {

	private final InsercaoRepositorio insercoes;

	private final Pessoas pessoas;

	ServicoCadastroAtendido(InsercaoRepositorio insercoes, Pessoas pessoas) {
		this.insercoes = insercoes;
		this.pessoas = pessoas;
	}

	/** Grava tudo numa transação; sem {@code dadosSensiveis}, a crença e a saúde ficam como estão. */
	@Transactional
	void atualizar(UUID insercaoId, EdicaoCadastro edicao) {
		Insercao insercao = insercoes.findById(insercaoId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Usuário atendido não encontrado."));
		UUID pessoaId = insercao.getPessoaId();
		DadosPessoa atual = pessoas.obter(pessoaId)
			.orElseThrow(() -> new IllegalStateException("Inserção sem pessoa: " + pessoaId));
		pessoas.atualizar(pessoaId, new NovaPessoa(edicao.nomeCompleto(), edicao.dataNascimento(),
				edicao.tipoDeficiencia(), edicao.telefone(), atual.nomeResponsavel()));
		pessoas.atualizarCadastroComplementar(pessoaId, edicao.cadastro().paraPessoas());
		if (edicao.dadosSensiveis() != null) {
			pessoas.atualizarDadosSensiveis(pessoaId, edicao.dadosSensiveis().paraPessoas());
		}
	}

}
