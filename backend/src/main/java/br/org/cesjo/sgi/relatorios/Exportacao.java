package br.org.cesjo.sgi.relatorios;

/**
 * API pública de exportação em PDF e Excel para os outros módulos (ADR 0040 e ADR 0041).
 *
 * <p>
 * O módulo que exporta monta a {@link TabelaExportacao} com os dados que a conta pode ver e devolve
 * {@link ArquivoExportado#comoDownload()} no controlador. A permissão é de quem chama: cada módulo
 * decide quem exporta os seus dados. A auditoria é daqui: toda exportação gerada registra
 * {@code EventoAuditoria.exportacao(entidade)} antes de o arquivo sair (ADR 0023 e ADR 0043).
 */
public interface Exportacao {

	/**
	 * Gera o arquivo e registra a exportação no log de auditoria.
	 *
	 * @param entidade o que foi exportado, como aparece no log de auditoria, por exemplo
	 * {@code lista_espera}
	 * @param tabela conteúdo do arquivo
	 * @param formato PDF ou planilha Excel
	 */
	ArquivoExportado exportar(String entidade, TabelaExportacao tabela, FormatoExportacao formato);

}
