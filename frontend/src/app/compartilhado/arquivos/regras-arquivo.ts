/**
 * Regras de arquivo do cliente, espelhando o backend (ADR 0027 e ADR 0029). O backend continua sendo
 * a autoridade: aqui elas só evitam enviar o que seria recusado.
 */

/** Limite de `sgi.arquivos.tamanho-maximo` no backend: 10 MB (10 MiB). */
export const TAMANHO_MAXIMO_BYTES = 10 * 1024 * 1024;

export type TipoArquivoAceito = 'application/pdf' | 'image/jpeg' | 'image/png';

interface Assinatura {
  tipo: TipoArquivoAceito;
  bytes: readonly number[];
}

/** Assinaturas dos primeiros bytes, as mesmas do enum `TipoArquivo` do backend. */
const ASSINATURAS: readonly Assinatura[] = [
  { tipo: 'application/pdf', bytes: [0x25, 0x50, 0x44, 0x46, 0x2d] },
  { tipo: 'image/jpeg', bytes: [0xff, 0xd8, 0xff] },
  { tipo: 'image/png', bytes: [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a] },
];

/** Valor do atributo `accept` do `<input type="file">`. */
export const ACEITE_INPUT = '.pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png';

const formatoTamanho = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 });

/** Tamanho legível em pt-BR: `850 bytes`, `12,5 KB`, `2,4 MB`. */
export function formatarTamanho(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} ${bytes === 1 ? 'byte' : 'bytes'}`;
  }
  if (bytes < 1024 * 1024) {
    return `${formatoTamanho.format(bytes / 1024)} KB`;
  }
  return `${formatoTamanho.format(bytes / (1024 * 1024))} MB`;
}

/** Reconhece o tipo pelos primeiros bytes, como o backend. `null` quando não é aceito. */
export async function detectarTipo(arquivo: Blob): Promise<TipoArquivoAceito | null> {
  const inicio = new Uint8Array(await lerInicio(arquivo, 8));
  const assinatura = ASSINATURAS.find(
    ({ bytes }) => inicio.length >= bytes.length && bytes.every((byte, i) => inicio[i] === byte),
  );
  return assinatura?.tipo ?? null;
}

/**
 * Confere o arquivo antes do envio. Devolve a mensagem de erro para quem usa o sistema, ou `null`
 * quando o arquivo pode ser enviado.
 */
export async function validarArquivo(arquivo: File): Promise<string | null> {
  if (arquivo.size === 0) {
    return 'O arquivo está vazio. Escolha outro arquivo.';
  }
  if (arquivo.size > TAMANHO_MAXIMO_BYTES) {
    return `O arquivo tem ${formatarTamanho(arquivo.size)} e passa do limite de ${formatarTamanho(TAMANHO_MAXIMO_BYTES)}.`;
  }
  if ((await detectarTipo(arquivo)) === null) {
    return 'Tipo de arquivo não aceito. Escolha um arquivo PDF, JPG ou PNG.';
  }
  return null;
}

function lerInicio(arquivo: Blob, tamanho: number): Promise<ArrayBuffer> {
  const parte = arquivo.slice(0, tamanho);
  if (typeof parte.arrayBuffer === 'function') {
    return parte.arrayBuffer();
  }
  return new Promise((resolver, rejeitar) => {
    const leitor = new FileReader();
    leitor.onload = () => resolver(leitor.result as ArrayBuffer);
    leitor.onerror = () => rejeitar(leitor.error);
    leitor.readAsArrayBuffer(parte);
  });
}
