# CESJO SGI — frontend

Aplicação Angular 22 (componentes standalone, sem SSR) com PrimeNG 22 e tema institucional
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

- `src/styles/_tokens.scss`: tokens do guia (cores, tipografia, espaçamento base 8px, raios, sombras).
  É o único lugar com valores HEX; componentes usam só as variáveis `--sgi-*`.
- `src/styles/_breakpoints.scss`: breakpoints globais em Sass.
- `src/app/tema/preset-cesjo.ts`: preset do PrimeNG derivado dos tokens. Modo escuro desligado.
- `src/app/layout/`: barra superior, menu lateral (gaveta em telas menores que 992px) e área de conteúdo.
- `src/app/modulos/`: um diretório por módulo. Cada módulo exporta um `ModuloFrontend` com caminho,
  rotas carregadas sob demanda e item de menu.

## Como adicionar um módulo

1. Crie `src/app/modulos/<modulo>/<modulo>.routes.ts` com as rotas internas do módulo.
2. Crie `src/app/modulos/<modulo>/<modulo>.modulo.ts`:

   ```ts
   export const listaEsperaModulo: ModuloFrontend = {
     caminho: 'lista-espera',
     rotas: () => import('./lista-espera.routes').then((m) => m.LISTA_ESPERA_ROUTES),
     menu: { rotulo: 'Lista de espera', icone: 'pi pi-list', ordem: 10 },
   };
   ```

3. Acrescente o módulo à lista `MODULOS` em `src/app/modulos/registro.ts`.

A rota e o item de menu passam a existir sem outras alterações.
