import { StatusVisita } from './visitas-api';

export const ROTULOS_STATUS: Readonly<Record<StatusVisita, string>> = {
  AGENDADA: 'Agendada',
  REALIZADA: 'Realizada',
  CANCELADA: 'Cancelada',
};
