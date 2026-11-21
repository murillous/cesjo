import { arquivoDeTeste, BYTES_JPEG, BYTES_PDF, BYTES_PNG } from './arquivos-de-teste';
import {
  detectarTipo,
  formatarTamanho,
  TAMANHO_MAXIMO_BYTES,
  validarArquivo,
} from './regras-arquivo';

describe('regras de arquivo', () => {
  it('reconhece PDF, JPG e PNG pela assinatura dos bytes', async () => {
    expect(await detectarTipo(arquivoDeTeste('a', BYTES_PDF))).toBe('application/pdf');
    expect(await detectarTipo(arquivoDeTeste('a', BYTES_JPEG))).toBe('image/jpeg');
    expect(await detectarTipo(arquivoDeTeste('a', BYTES_PNG))).toBe('image/png');
  });

  it('ignora extensão e tipo informados', async () => {
    const falso = arquivoDeTeste('laudo.pdf', [0x3c, 0x68, 0x74, 0x6d, 0x6c], 'application/pdf');

    expect(await detectarTipo(falso)).toBeNull();
    expect(await validarArquivo(falso)).toBe(
      'Tipo de arquivo não aceito. Escolha um arquivo PDF, JPG ou PNG.',
    );
  });

  it('aceita arquivo válido', async () => {
    expect(await validarArquivo(arquivoDeTeste('foto.png', BYTES_PNG))).toBeNull();
  });

  it('recusa arquivo vazio', async () => {
    expect(await validarArquivo(arquivoDeTeste('vazio.pdf', []))).toBe(
      'O arquivo está vazio. Escolha outro arquivo.',
    );
  });

  it('aceita exatamente 10 MB e recusa um byte a mais', async () => {
    const limite = new Uint8Array(TAMANHO_MAXIMO_BYTES);
    limite.set(BYTES_PDF);
    const acima = new Uint8Array(TAMANHO_MAXIMO_BYTES + 1);
    acima.set(BYTES_PDF);

    expect(await validarArquivo(new File([limite], 'limite.pdf'))).toBeNull();
    expect(await validarArquivo(new File([acima], 'grande.pdf'))).toContain(
      'passa do limite de 10 MB',
    );
    const muitoGrande = new Uint8Array(Math.round(12.3 * 1024 * 1024));
    muitoGrande.set(BYTES_PDF);
    expect(await validarArquivo(new File([muitoGrande], 'enorme.pdf'))).toBe(
      'O arquivo tem 12,3 MB e passa do limite de 10 MB.',
    );
  });

  it('formata tamanhos em pt-BR', () => {
    expect(formatarTamanho(1)).toBe('1 byte');
    expect(formatarTamanho(850)).toBe('850 bytes');
    expect(formatarTamanho(12_800)).toBe('12,5 KB');
    expect(formatarTamanho(2.4 * 1024 * 1024)).toBe('2,4 MB');
    expect(formatarTamanho(TAMANHO_MAXIMO_BYTES)).toBe('10 MB');
  });
});
