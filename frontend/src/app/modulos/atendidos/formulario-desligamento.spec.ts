import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter, Router } from '@angular/router';

import { FormularioDesligamento } from './formulario-desligamento';

interface Componente {
  formulario: { setValue(v: object): void };
  salvar(): void;
}

describe('FormularioDesligamento', () => {
  let http: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FormularioDesligamento],
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
    const fixture = TestBed.createComponent(FormularioDesligamento);
    fixture.componentRef.setInput('id', 'ins-1');
    await fixture.whenStable();
    return { fixture, componente: fixture.componentInstance as unknown as Componente };
  }

  it('registra o desligamento e volta para a ficha', async () => {
    const { componente } = await criar();
    componente.formulario.setValue({
      data: new Date(2026, 10, 20),
      motivo: 'EVASAO',
      observacao: '  ',
    });
    componente.salvar();

    const pedido = http.expectOne('/api/atendidos/ins-1/desligamento');
    expect(pedido.request.body).toEqual({ data: '2026-11-20', motivo: 'EVASAO', observacao: null });
    pedido.flush({ id: 'des-1' }, { status: 201, statusText: 'Created' });
    expect(navegar).toHaveBeenCalledWith(['/atendidos', 'ins-1']);
  });

  it('exige observação no motivo Outros', async () => {
    const { componente } = await criar();
    componente.formulario.setValue({
      data: new Date(2026, 10, 20),
      motivo: 'OUTROS',
      observacao: '',
    });
    componente.salvar();
    http.expectNone('/api/atendidos/ins-1/desligamento');

    componente.formulario.setValue({
      data: new Date(2026, 10, 20),
      motivo: 'OUTROS',
      observacao: ' Motivo fictício. ',
    });
    componente.salvar();
    const pedido = http.expectOne('/api/atendidos/ins-1/desligamento');
    expect(pedido.request.body.observacao).toBe('Motivo fictício.');
    pedido.flush({ id: 'des-2' });
  });

  it('mostra o detail do erro do backend', async () => {
    const { fixture, componente } = await criar();
    componente.formulario.setValue({
      data: new Date(2026, 10, 20),
      motivo: 'EVASAO',
      observacao: '',
    });
    componente.salvar();
    http
      .expectOne('/api/atendidos/ins-1/desligamento')
      .flush(
        { detail: 'Este usuário já foi desligado do serviço.' },
        { status: 422, statusText: 'Unprocessable Content' },
      );
    await fixture.whenStable();

    const alerta = (fixture.nativeElement as HTMLElement).querySelector('[role="alert"]');
    expect(alerta?.textContent).toContain('já foi desligado');
    expect(navegar).not.toHaveBeenCalled();
  });
});
