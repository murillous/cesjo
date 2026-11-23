import { Permissao } from '../../acesso/permissao';

/**
 * Abas da página de relatórios, na ordem em que aparecem. Uma aba nova é uma linha aqui e uma rota.
 * `permissao` é a exigida além de `RELATORIOS_LER`: a aba some para quem não a tem.
 */
export const ABAS: readonly { caminho: string; rotulo: string; permissao?: Permissao }[] = [
  { caminho: 'perfil', rotulo: 'Perfil dos atendidos' },
  { caminho: 'atendimento', rotulo: 'Violações, atividades e desligamentos' },
  { caminho: 'mensal', rotulo: 'Relatório mensal' },
  { caminho: 'intervencao', rotulo: 'Relatório de intervenção', permissao: 'VIOLACOES_LER' },
];
