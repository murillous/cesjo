# CESJO SGI — frontend

Aplicação Angular 22 (componentes standalone, sem SSR) com Angular Material 22 + Angular CDK e tema institucional
construído a partir do [guia de estilo](../docs/frontend/guiaDeEstilo.md).

## Pré-requisitos

- Node.js na versão de `.nvmrc` (`nvm use`).
- npm (vem com o Node).

## Comandos

```bash
npm ci              # instala as dependências do package-lock.json
npm start           # ng serve em http://localhost:4200, com proxy de /api para http://localhost:8080
npm run lint        # angular-eslint
npm test            # testes unitários (Vitest) em modo watch
npm run test:ci     # testes unitários uma vez, sem watch (usado no CI)
npm run build       # build de produção em dist/sgi-frontend
```

## Organização

- `src/styles/_tokens.scss`: tokens do guia (cores, tipografia, espaçamento base 8px, raios, sombras)
  e paletas tonais do Material 3. É o único lugar com valores HEX; componentes usam só as variáveis `--sgi-*`.
- `src/styles/_breakpoints.scss`: breakpoints globais em Sass.
- `src/styles/_tema.scss`: tema Material 3 (`mat.theme` com paletas próprias, fonte Inter, densidade
  padrão) e overrides das cores de sistema com os tokens do guia. Ação, foco e seleção em
  primary-dark; turquesa só em ícones e indicadores. Sem modo escuro.
- Ícones: Material Symbols (estilo outlined, pacote npm `material-symbols`), usados com `<mat-icon>nome</mat-icon>`.
- Locale pt-BR: `LOCALE_ID`, `MAT_DATE_LOCALE` e `provideNativeDateAdapter` em `src/app/app.config.ts`.
- `src/app/layout/`: `MatToolbar` (logo, notificações, conta) e `MatSidenav` (modo `side` a partir de
  992px, modo `over` abaixo disso, fechando com Escape ou clique fora) e área de conteúdo.
- `src/app/modulos/`: um diretório por módulo. Cada módulo exporta um `ModuloFrontend` com caminho,
  rotas carregadas sob demanda e item de menu.

## Como adicionar um módulo

1. Crie `src/app/modulos/<modulo>/<modulo>.routes.ts` com as rotas internas do módulo.
2. Crie `src/app/modulos/<modulo>/<modulo>.modulo.ts`:

   ```ts
   export const listaEsperaModulo: ModuloFrontend = {
     caminho: 'lista-espera',
     rotas: () => import('./lista-espera.routes').then((m) => m.LISTA_ESPERA_ROUTES),
     menu: { rotulo: 'Lista de espera', icone: 'list', ordem: 10 },
   };
   ```

3. Acrescente o módulo à lista `MODULOS` em `src/app/modulos/registro.ts`.

A rota e o item de menu passam a existir sem outras alterações.
