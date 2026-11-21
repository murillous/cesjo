import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter, Router } from '@angular/router';

import { FormularioViolacao } from './formulario-violacao';

interface Componente {
  formulario: { setValue(v: object): void };
  salvar(): void;
}

describe('FormularioViolacao', () => {
  let http: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FormularioViolacao],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNativeDateAdapter(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => http.verify());

  async function criar() {
    const fixture = TestBed.createComponent(FormularioViolacao);
    fixture.componentRef.setInput('id', 'ins-1');
    await fixture.whenStable();
    return {
      fixture,
      componente: fixture.componentInstance as unknown as Componente,
      elemento: fixture.nativeElement as HTMLElement,
    };
  }

  it('lista as 12 categorias do RF009', async () => {
    const { elemento } = await criar();
    expect(elemento.querySelectorAll('mat-radio-button')).toHaveLength(12);
  });

  it('exige o âmbito para violência e o envia', async () => {
    const { fixture, componente, elemento } = await criar();
    componente.formulario.setValue({
      tipo: 'VIOLENCIA_FISICA_PSICOLOGICA',
      ambito: '',
      dataIdentificacao: new Date(2026, 10, 20),
      descricao: 'Relato fictício.',
    });
    componente.salvar();
    http.expectNone('/api/atendidos/ins-1/violacoes');
    await fixture.whenStable();
    expect(elemento.textContent).toContain('Âmbito');

    componente.formulario.setValue({
      tipo: 'VIOLENCIA_FISICA_PSICOLOGICA',
      ambito: 'EXTRAFAMILIAR',
      dataIdentificacao: new Date(2026, 10, 20),
      descricao: ' Relato fictício. ',
    });
    componente.salvar();
    const pedido = http.expectOne('/api/atendidos/ins-1/violacoes');
    expect(pedido.request.body).toEqual({
      tipo: 'VIOLENCIA_FISICA_PSICOLOGICA',
      ambito: 'EXTRAFAMILIAR',
      dataIdentificacao: '2026-11-20',
      descricao: 'Relato fictício.',
    });
    pedido.flush({ id: 'vio-1' }, { status: 201, statusText: 'Created' });
    expect(navegar).toHaveBeenCalledWith(['/atendidos', 'ins-1']);
  });

  it('não manda âmbito para categoria que não pede', async () => {
    const { componente } = await criar();
    componente.formulario.setValue({
      tipo: 'ISOLAMENTO_SOCIAL',
      ambito: 'INTRAFAMILIAR',
      dataIdentificacao: new Date(2026, 10, 20),
      descricao: 'Relato fictício.',
    });
    componente.salvar();
    const pedido = http.expectOne('/api/atendidos/ins-1/violacoes');
    expect(pedido.request.body.ambito).toBeNull();
    pedido.flush({ id: 'vio-2' });
  });
});
