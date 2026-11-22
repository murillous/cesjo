import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';

import { PainelPerfil as Painel } from './estatisticas-api';
import { PainelPerfil } from './painel-perfil';

const URL = '/api/estatisticas/dashboards/perfil';

const PAINEL: Painel = {
  geradoEm: '2026-11-22T13:00:00Z',
  totalAtivos: 4,
  porTipoDeficiencia: [
    { chave: 'FISICA', rotulo: 'Física', total: 3 },
    { chave: 'TEA', rotulo: 'TEA', total: 1 },
  ],
  porGenero: [
    { chave: 'FEMININO', rotulo: 'Feminino', total: 1 },
    { chave: 'MASCULINO', rotulo: 'Masculino', total: 0 },
    { chave: 'SEM_VALOR', rotulo: 'Não informado', total: 3 },
  ],
  porFaixaEtaria: [
    { chave: 'JOVEM', rotulo: 'Jovem (18 a 29 anos)', total: 2 },
    { chave: 'ADULTO', rotulo: 'Adulto (30 a 59 anos)', total: 2 },
  ],
  porTurno: [
    { chave: 'MATUTINO', rotulo: 'Matutino', total: 4 },
    { chave: 'VESPERTINO', rotulo: 'Vespertino', total: 0 },
  ],
};

describe('PainelPerfil', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PainelPerfil],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function renderizar() {
    const fixture = TestBed.createComponent(PainelPerfil);
    fixture.detectChanges();
    http.expectOne(URL).flush(PAINEL);
    await fixture.whenStable();
    fixture.detectChanges();
    return { fixture, elemento: fixture.nativeElement as HTMLElement };
  }

  it('mostra o total e um gráfico por categoria', async () => {
    const { elemento } = await renderizar();

    expect(elemento.querySelector('.total')?.textContent).toContain('4');
    const titulos = Array.from(elemento.querySelectorAll('h2')).map((h) => h.textContent?.trim());
    expect(titulos).toEqual(['Tipo de deficiência', 'Gênero', 'Faixa etária', 'Turno']);
  });

  it('dá a maior barra a largura toda e escreve valor e porcentagem', async () => {
    const { elemento } = await renderizar();
    const genero = elemento.querySelectorAll('app-grafico-barras')[1];
    const barras = Array.from(genero.querySelectorAll<HTMLElement>('.barra'));

    const fracoes = barras.map((barra) => Number(barra.style.getPropertyValue('--fracao')));
    expect(fracoes.map((f) => f.toFixed(2))).toEqual(['0.33', '0.00', '1.00']);
    expect(genero.querySelector('.sem-valor .valor')?.textContent).toContain('75%');
    expect(genero.querySelectorAll('tbody tr').length).toBe(3);
  });

  it('busca de novo ao atualizar', async () => {
    const { elemento, fixture } = await renderizar();

    elemento.querySelector<HTMLButtonElement>('.atualizado button')?.click();
    http.expectOne(URL).flush({ ...PAINEL, totalAtivos: 5 });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(elemento.querySelector('.total strong')?.textContent).toContain('5');
  });

  it('avisa quando não carrega', async () => {
    const fixture = TestBed.createComponent(PainelPerfil);
    fixture.detectChanges();
    http.expectOne(URL).flush(null, { status: 500, statusText: 'Erro' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('[role="alert"]')).not.toBeNull();
  });
});
