package br.org.cesjo.sgi.auditoria.interno;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.seguranca.Identidade;
import br.org.cesjo.sgi.compartilhado.seguranca.IdentidadeAtual;

/**
 * Grava cada {@link EventoAuditoria} na tabela {@code auditoria}.
 *
 * <p>
 * Transação (ADR 0021): dentro de uma transação de escrita, o registro entra nela e só vale se a
 * operação for confirmada. Sem transação, ou numa transação só de leitura (leitura de dado sensível,
 * exportação, falha de login), o registro é gravado numa transação própria. Se a gravação falhar, a
 * exceção sobe e a operação falha junto: nada acontece sem registro.
 */
@Component
class AuditoriaEmBanco implements Auditoria {

	/** Tamanho máximo de login, entidade e identificador, igual às colunas da migração V1. */
	static final int TAMANHO_MAXIMO = 100;

	/** Tamanho máximo de um endereço IP em texto (IPv6 com IPv4 embutido). */
	static final int TAMANHO_MAXIMO_IP = 45;

	/**
	 * Nomes de entidade e de campo aceitos: identificadores, como {@code conta_acesso} ou
	 * {@code endereco.cep}. Barra texto livre, para que nenhum valor entre no log por engano (ADR 0020).
	 */
	private static final Pattern NOME = Pattern.compile("[A-Za-z][A-Za-z0-9_.]{0," + (TAMANHO_MAXIMO - 1) + "}");

	private static final Logger log = LoggerFactory.getLogger(AuditoriaEmBanco.class);

	@PersistenceContext
	private EntityManager entityManager;

	private final IdentidadeAtual identidadeAtual;

	private final TransactionTemplate transacaoPropria;

	AuditoriaEmBanco(IdentidadeAtual identidadeAtual, PlatformTransactionManager transacoes) {
		this.identidadeAtual = identidadeAtual;
		this.transacaoPropria = new TransactionTemplate(transacoes);
		this.transacaoPropria.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	@Override
	public void registrar(EventoAuditoria evento) {
		RegistroAuditoria registro = montar(evento);
		try {
			if (TransactionSynchronizationManager.isActualTransactionActive()
					&& !TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
				entityManager.persist(registro);
			}
			else {
				transacaoPropria.executeWithoutResult(status -> entityManager.persist(registro));
			}
		}
		catch (RuntimeException erro) {
			log.error("Falha ao gravar o log de auditoria: acao={} entidade={} id={}", evento.acao(),
					evento.entidade(), evento.entidadeId(), erro);
			throw erro;
		}
	}

	private RegistroAuditoria montar(EventoAuditoria evento) {
		exigirNome("entidade", evento.entidade());
		evento.camposAlterados().forEach(campo -> exigirNome("campo", campo));
		if (evento.entidadeId() != null && evento.entidadeId().length() > TAMANHO_MAXIMO) {
			throw new IllegalArgumentException("Identificador da entidade com mais de " + TAMANHO_MAXIMO + " caracteres");
		}

		// O evento que já traz conta ou login (login, falha de login) descreve outra conta que não a da
		// requisição; nos demais casos vale quem está logado.
		UUID contaId = evento.contaId();
		String login = evento.login();
		if (contaId == null && login == null) {
			Optional<Identidade> logada = identidadeAtual.obter();
			contaId = logada.map(Identidade::id).orElse(null);
			login = logada.map(Identidade::login).orElse(null);
		}

		List<String> campos = evento.camposAlterados().stream().sorted().toList();
		return new RegistroAuditoria(Instant.now(), contaId, cortar(login, TAMANHO_MAXIMO), evento.acao(),
				evento.entidade(), evento.entidadeId(), campos, cortar(ipDaRequisicao(), TAMANHO_MAXIMO_IP));
	}

	private static void exigirNome(String tipo, String nome) {
		if (!NOME.matcher(nome).matches()) {
			throw new IllegalArgumentException(
					"Nome de " + tipo + " inválido para o log de auditoria; use só o nome, nunca o valor");
		}
	}

	private static String ipDaRequisicao() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
			return atributos.getRequest().getRemoteAddr();
		}
		return null;
	}

	private static String cortar(String texto, int tamanho) {
		return texto == null || texto.length() <= tamanho ? texto : texto.substring(0, tamanho);
	}

}
