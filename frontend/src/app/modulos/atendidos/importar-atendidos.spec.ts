import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { provideRouter } from '@angular/router';

import { ImportarAtendidos } from './importar-atendidos';

describe('ImportarAtendidos', () => {
  it('usa a importação dos usuários atendidos no backend', async () => {
    TestBed.configureTestingModule({
      imports: [ImportarAtendidos],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(ImportarAtendidos);
    fixture.detectChanges();

    http.expectOne('/api/importacao/atendidos/colunas').flush([]);
    await fixture.whenStable();

    const elemento = fixture.nativeElement as HTMLElement;
    expect(elemento.querySelector('h1')?.textContent).toContain('Importar usuários atendidos');
    expect(elemento.querySelectorAll('.passo').length).toBe(3);
    http.verify();
  });
});
