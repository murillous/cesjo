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

- Mobile first: estilos base para o celular e media queries só com `min-width`.
- `src/styles/_tokens.scss`: tokens do guia (cores, tipografia, espaçamento base 8px, raios, sombras,
  camadas, alturas das barras) e paletas tonais do Material 3. É o único lugar com valores HEX; componentes usam só as variáveis `--sgi-*`.
- `src/styles/_breakpoints.scss`: breakpoints globais em Sass.
- Fonte: Atkinson Hyperlegible Next (pacote `@fontsource-variable/atkinson-hyperlegible-next`).
- `src/styles/_tema.scss`: tema Material 3 (`mat.theme` com paletas próprias, fonte do guia, densidade
  padrão) e overrides das cores de sistema com os tokens do guia. Ação, foco e seleção em
  primary-dark; turquesa só em ícones e indicadores. Sem modo escuro.
- Ícones: Material Symbols (estilo outlined, pacote npm `material-symbols`), usados com `<mat-icon>nome</mat-icon>`.
- Locale pt-BR: `LOCALE_ID`, `MAT_DATE_LOCALE` e `provideNativeDateAdapter` em `src/app/app.config.ts`.
- `src/app/layout/`: barra superior (logo, notificações, conta), navegação principal e área de
  conteúdo, num grid CSS. A navegação (`layout/menu/`) é barra inferior no celular, trilho a partir de
  768px e menu lateral a partir de 992px. Com mais de cinco módulos, a barra inferior mostra quatro e
  o item "Mais", que abre `layout/menu-mais/` num `MatBottomSheet`. A troca de formato é só CSS.
- `src/app/modulos/`: um diretório por módulo. Cada módulo exporta um `ModuloFrontend` com caminho,
  rotas carregadas sob demanda e item de menu.

## Como adicionar um módulo

1. Crie `src/app/modulos/<modulo>/<modulo>.routes.ts` com as rotas internas do módulo.
2. Crie `src/app/modulos/<modulo>/<modulo>.modulo.ts`:

   ```ts
   export const listaEsperaModulo: ModuloFrontend = {
     caminho: 'lista-espera',
     rotas: () => import('./lista-espera.routes').then((m) => m.LISTA_ESPERA_ROUTES),
     menu: { rotulo: 'Lista de espera', rotuloCurto: 'Fila', icone: 'list', ordem: 10 },
   };
   ```

3. Acrescente o módulo à lista `MODULOS` em `src/app/modulos/registro.ts`.

A rota e o item de menu passam a existir sem outras alterações. `rotuloCurto` é opcional: use quando
o rótulo passar de uns 10 caracteres, porque é ele que aparece na barra inferior do celular.
