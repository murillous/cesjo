import { Perfil } from './permissao';

/** Nomes dos perfis como aparecem nas telas, iguais aos rótulos do enum `Perfil` do backend. */
export const ROTULOS_PERFIL: Readonly<Record<Perfil, string>> = {
  COORDENADOR: 'Coordenador',
  EQUIPE_TECNICA: 'Equipe Técnica',
  ADMINISTRATIVO: 'Administrativo',
  CONSULTA: 'Consulta',
};

export const PERFIS: readonly Perfil[] = [
  'COORDENADOR',
  'EQUIPE_TECNICA',
  'ADMINISTRATIVO',
  'CONSULTA',
];
