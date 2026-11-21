package br.org.cesjo.sgi.pessoas.interno;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.seguranca.Identidade;
import br.org.cesjo.sgi.compartilhado.seguranca.IdentidadeAtual;
import br.org.cesjo.sgi.pessoas.DadosPessoa;
import br.org.cesjo.sgi.pessoas.NovaPessoa;
import br.org.cesjo.sgi.pessoas.Pessoas;

@Service
class ServicoPessoas implements Pessoas {

	private final PessoaRepositorio pessoas;

	private final ResponsavelRepositorio responsaveis;

	private final Auditoria auditoria;

	private final IdentidadeAtual identidadeAtual;

	private final Clock relogio = Clock.systemUTC();

	ServicoPessoas(PessoaRepositorio pessoas, ResponsavelRepositorio responsaveis, Auditoria auditoria,
			IdentidadeAtual identidadeAtual) {
		this.pessoas = pessoas;
		this.responsaveis = responsaveis;
		this.auditoria = auditoria;
		this.identidadeAtual = identidadeAtual;
	}

	@Override
	@Transactional
	public UUID cadastrar(NovaPessoa nova) {
		UUID autor = identidadeAtual.obter().map(Identidade::id).orElse(null);
		Instant agora = relogio.instant();
		Pessoa pessoa = pessoas.save(new Pessoa(nova, autor, agora));
		Responsavel responsavel = responsaveis
			.save(new Responsavel(pessoa.getId(), nova.nomeResponsavel(), autor, agora));
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE, pessoa.getId()));
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE_RESPONSAVEL, responsavel.getId()));
		return pessoa.getId();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<DadosPessoa> obter(UUID id) {
		return pessoas.findById(id).map(this::dados);
	}

	@Override
	@Transactional(readOnly = true)
	public Map<UUID, String> nomes(Collection<UUID> ids) {
		return pessoas.findAllById(ids).stream().collect(Collectors.toMap(Pessoa::getId, Pessoa::getNomeCompleto));
	}

	private DadosPessoa dados(Pessoa pessoa) {
		String responsavel = responsaveis.findFirstByPessoaIdOrderByCriadoEmAsc(pessoa.getId())
			.map(Responsavel::getNomeCompleto)
			.orElse(null);
		return new DadosPessoa(pessoa.getId(), pessoa.getNomeCompleto(), pessoa.getDataNascimento(),
				pessoa.getTipoDeficiencia(), pessoa.getTelefone(), responsavel);
	}

}
