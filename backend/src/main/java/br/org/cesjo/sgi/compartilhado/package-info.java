/**
 * Infraestrutura compartilhada, sem regra de negócio: tratamento de erros, configuração web e
 * utilitários técnicos. É um módulo aberto, que todos os demais módulos podem usar.
 */
@ApplicationModule(displayName = "Compartilhado", type = ApplicationModule.Type.OPEN)
package br.org.cesjo.sgi.compartilhado;

import org.springframework.modulith.ApplicationModule;
