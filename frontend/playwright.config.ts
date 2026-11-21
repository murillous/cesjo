import { defineConfig, devices } from '@playwright/test';

/**
 * Auditoria automatizada de acessibilidade e navegação por teclado (issue #24, ADR 0123). Roda contra
 * o `ng serve`, com a API simulada no navegador (`e2e/api-simulada.ts`), em celular e desktop. O Edge
 * usa o mesmo motor do Chromium; o WebKit é a aproximação do Safari, que ainda passa pela checklist
 * manual em `docs/frontend/checklist-navegadores.md`.
 */
export default defineConfig({
  testDir: 'e2e',
  testMatch: '**/*.e2e.ts',
  fullyParallel: true,
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 1 : 0,
  reporter: process.env['CI'] ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: 'http://localhost:4200',
    locale: 'pt-BR',
    trace: 'retain-on-failure',
  },
  projects: [
    // Celular primeiro (ADR 0035). Chromium com tela e toque de um Pixel 7.
    { name: 'celular', use: { ...devices['Pixel 7'] } },
    {
      name: 'desktop',
      use: { ...devices['Desktop Chrome'], viewport: { width: 1280, height: 800 } },
    },
    { name: 'safari-celular', use: { ...devices['iPhone 13'] } },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'], viewport: { width: 1280, height: 800 } },
    },
  ],
  webServer: {
    command: 'npm start -- --port 4200',
    url: 'http://localhost:4200',
    reuseExistingServer: !process.env['CI'],
    timeout: 180_000,
  },
});
