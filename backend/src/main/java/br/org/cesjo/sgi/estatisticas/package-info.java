/**
 * Módulo Estatísticas: dashboards (RF017) e relatórios estatísticos (RF016). Lê os outros módulos só
 * pelas APIs Java públicas e exporta pelo módulo Relatórios. Fica separado do Relatórios porque a lista
 * de espera usa a exportação, e os dashboards leem a lista de espera: no mesmo módulo, seria um ciclo
 * (ADR 0195).
 */
@ApplicationModule(displayName = "Estatísticas")
package br.org.cesjo.sgi.estatisticas;

import org.springframework.modulith.ApplicationModule;
