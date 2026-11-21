/** Conteúdos fictícios com a assinatura de cada tipo aceito, só para os testes. */
export const BYTES_PDF = [0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e, 0x37];
export const BYTES_JPEG = [0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10];
export const BYTES_PNG = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00];

export function arquivoDeTeste(nome: string, bytes: readonly number[], tipo = ''): File {
  return new File([new Uint8Array(bytes)], nome, { type: tipo });
}
