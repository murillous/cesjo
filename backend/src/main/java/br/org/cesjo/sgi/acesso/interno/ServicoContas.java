package br.org.cesjo.sgi.acesso.interno;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.acesso.ContaAtual;
import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.web.RecursoNaoEncontradoException;
import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;

/**
 * Gestão das contas de acesso pelo Coordenador (seção 4.1, ADR 0013). Toda conta criada ou com senha
 * redefinida recebe uma senha temporária gerada aqui e precisa trocá-la no próximo acesso.
 */
@Service
class ServicoContas {

	private static final String ENTIDADE = ServicoAutenticacao.ENTIDADE;

	private final ContaAcessoRepositorio contas;

	private final PasswordEncoder codificador;

	private final RegraSenha regraSenha;

	private final Auditoria auditoria;

	private final ContaAtual contaAtual;

	private final Clock relogio = Clock.systemUTC();

	ServicoContas(ContaAcessoRepositorio contas, PasswordEncoder codificador, RegraSenha regraSenha,
			Auditoria auditoria, ContaAtual contaAtual) {
		this.contas = contas;
		this.codificador = codificador;
		this.regraSenha = regraSenha;
		this.auditoria = auditoria;
		this.contaAtual = contaAtual;
	}

	@Transactional(readOnly = true)
	Page<ContaResposta> listar(Pageable paginacao) {
		Instant agora = relogio.instant();
		return contas.findAll(paginacao).map(conta -> ContaResposta.de(conta, agora));
	}

	@Transactional(readOnly = true)
	ContaResposta obter(UUID id) {
		return ContaResposta.de(buscar(id), relogio.instant());
	}

	@Transactional
	ContaCriada criar(NovaConta nova) {
		String login = ContaAcesso.normalizarLogin(nova.login());
		if (contas.existsByLogin(login)) {
			throw new RegraNegocioException("Já existe uma conta com este login.");
		}
		String senhaTemporaria = regraSenha.gerarTemporaria();
		Instant agora = relogio.instant();
		ContaAcesso conta = new ContaAcesso(login, nova.nome(), nova.perfil(), codificador.encode(senhaTemporaria),
				responsavel(), agora);
		contas.save(conta);
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE, conta.getId()));
		return new ContaCriada(ContaResposta.de(conta, agora), senhaTemporaria);
	}

	/** Altera nome e perfil. Só registra na auditoria os campos que mudaram de fato. */
	@Transactional
	ContaResposta editar(UUID id, EdicaoConta edicao) {
		ContaAcesso conta = buscar(id);
		UUID responsavel = responsavel();
		Instant agora = relogio.instant();
		Set<String> alterados = new LinkedHashSet<>();
		if (!conta.getNome().equals(edicao.nome().strip())) {
			conta.alterarNome(edicao.nome(), responsavel, agora);
			alterados.add("nome");
		}
		if (conta.getPerfil() != edicao.perfil()) {
			exigirOutraConta(conta, "Você não pode alterar o perfil da sua própria conta.");
			if (conta.isAtiva() && conta.getPerfil() == Perfil.COORDENADOR) {
				exigirOutroCoordenadorAtivo(conta,
						"A última conta ativa de Coordenador não pode perder o perfil de Coordenador.");
			}
			conta.alterarPerfil(edicao.perfil(), responsavel, agora);
			alterados.add("perfil");
		}
		if (!alterados.isEmpty()) {
			auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE, conta.getId(), alterados));
		}
		return ContaResposta.de(conta, agora);
	}

	@Transactional
	ContaResposta desativar(UUID id) {
		ContaAcesso conta = buscar(id);
		if (!conta.isAtiva()) {
			return ContaResposta.de(conta, relogio.instant());
		}
		exigirOutraConta(conta, "Você não pode desativar a sua própria conta.");
		if (conta.getPerfil() == Perfil.COORDENADOR) {
			exigirOutroCoordenadorAtivo(conta, "A última conta ativa de Coordenador não pode ser desativada.");
		}
		Instant agora = relogio.instant();
		conta.desativar(responsavel(), agora);
		auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE, conta.getId(), Set.of("ativa")));
		return ContaResposta.de(conta, agora);
	}

	@Transactional
	ContaResposta reativar(UUID id) {
		ContaAcesso conta = buscar(id);
		Instant agora = relogio.instant();
		if (!conta.isAtiva()) {
			conta.reativar(responsavel(), agora);
			auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE, conta.getId(), Set.of("ativa")));
		}
		return ContaResposta.de(conta, agora);
	}

	/** Gera uma senha temporária, exige troca no próximo acesso e desfaz o bloqueio. */
	@Transactional
	SenhaTemporaria redefinirSenha(UUID id) {
		ContaAcesso conta = buscar(id);
		String senhaTemporaria = regraSenha.gerarTemporaria();
		conta.redefinirSenha(codificador.encode(senhaTemporaria), responsavel(), relogio.instant());
		auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE, conta.getId(),
				Set.of("senha_hash", "troca_senha_obrigatoria")));
		return new SenhaTemporaria(senhaTemporaria);
	}

	private ContaAcesso buscar(UUID id) {
		return contas.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada."));
	}

	private UUID responsavel() {
		return contaAtual.id().orElse(null);
	}

	private void exigirOutraConta(ContaAcesso conta, String mensagem) {
		if (conta.getId().equals(responsavel())) {
			throw new RegraNegocioException(mensagem);
		}
	}

	/**
	 * Garante que, tirando esta conta, sobra outro Coordenador ativo. As contas ativas de Coordenador
	 * ficam travadas até o fim da transação.
	 */
	private void exigirOutroCoordenadorAtivo(ContaAcesso conta, String mensagem) {
		boolean haOutro = contas.findByPerfilAndAtivaTrue(Perfil.COORDENADOR)
			.stream()
			.anyMatch(outra -> !outra.getId().equals(conta.getId()));
		if (!haOutro) {
			throw new RegraNegocioException(mensagem);
		}
	}

}
