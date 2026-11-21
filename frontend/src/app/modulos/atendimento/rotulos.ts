import { StatusPia } from './atendimento-api';

export const ROTULOS_STATUS_PIA: Readonly<Record<StatusPia, string>> = {
  ELABORADO: 'Em elaboração',
  APRESENTADO_FAMILIA: 'Apresentado à família',
  APRESENTADO_EQUIPE: 'Apresentado à equipe',
  APROVADO: 'Aprovado',
};
