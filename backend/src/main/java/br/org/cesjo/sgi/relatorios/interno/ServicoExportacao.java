package br.org.cesjo.sgi.relatorios.interno;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.relatorios.ArquivoExportado;
import br.org.cesjo.sgi.relatorios.Exportacao;
import br.org.cesjo.sgi.relatorios.FormatoExportacao;
import br.org.cesjo.sgi.relatorios.TabelaExportacao;

/** Implementação de {@link Exportacao} (ADR 0040 a ADR 0043). */
@Service
class ServicoExportacao implements Exportacao {

	/** Tamanho máximo da parte do nome do arquivo que vem do título. */
	static final int TAMANHO_MAXIMO_NOME = 80;

	private final GeradorPdf geradorPdf;

	private final GeradorPlanilha geradorPlanilha;

	private final Auditoria auditoria;

	private final Clock relogio;

	@Autowired
	ServicoExportacao(GeradorPdf geradorPdf, GeradorPlanilha geradorPlanilha, Auditoria auditoria) {
		this(geradorPdf, geradorPlanilha, auditoria, Clock.systemUTC());
	}

	ServicoExportacao(GeradorPdf geradorPdf, GeradorPlanilha geradorPlanilha, Auditoria auditoria, Clock relogio) {
		this.geradorPdf = geradorPdf;
		this.geradorPlanilha = geradorPlanilha;
		this.auditoria = auditoria;
		this.relogio = relogio;
	}

	@Override
	public ArquivoExportado exportar(String entidade, TabelaExportacao tabela, FormatoExportacao formato) {
		Objects.requireNonNull(entidade, "entidade");
		Objects.requireNonNull(tabela, "tabela");
		Objects.requireNonNull(formato, "formato");
		Instant agora = relogio.instant();
		byte[] conteudo = switch (formato) {
			case PDF -> geradorPdf.gerar(tabela, agora);
			case XLSX -> geradorPlanilha.gerar(tabela, agora);
		};
		// Registrado depois de gerar e antes de devolver: falha na geração não deixa registro de algo que
		// não saiu, e falha na auditoria impede o arquivo de sair (ADR 0021 e ADR 0043).
		auditoria.registrar(EventoAuditoria.exportacao(entidade));
		return new ArquivoExportado(nomeArquivo(tabela.titulo(), agora, formato), formato, conteudo);
	}

	/**
	 * Nome do download: título sem acentos e em minúsculas, com hífens, seguido da data de geração, por
	 * exemplo {@code lista-de-espera-2026-11-21.pdf}. O nome não leva dado do conteúdo além do título.
	 */
	static String nomeArquivo(String titulo, Instant geradoEm, FormatoExportacao formato) {
		String semAcento = Normalizer.normalize(titulo, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		String base = semAcento.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
		if (base.length() > TAMANHO_MAXIMO_NOME) {
			base = base.substring(0, TAMANHO_MAXIMO_NOME).replaceAll("-+$", "");
		}
		if (base.isEmpty()) {
			base = "exportacao";
		}
		LocalDate data = LocalDate.ofInstant(geradoEm, IdentidadeVisual.FUSO);
		return base + "-" + data + "." + formato.extensao();
	}

}
