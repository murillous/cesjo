import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';

/** Mistura uma variável de cor do guia com branco (tons claros) ou preto (tons escuros). */
function tom(variavel: string, mistura: 'white' | 'black', percentual: number): string {
  return `color-mix(in srgb, var(${variavel}) ${percentual}%, ${mistura})`;
}

/**
 * Escala 50–950 derivada de uma cor do guia. O 500 é a própria cor; os demais são
 * misturas com branco ou preto, para não espalhar HEX fora de `styles/_tokens.scss`.
 */
function escala(variavel: string): Record<string, string> {
  return {
    50: tom(variavel, 'white', 8),
    100: tom(variavel, 'white', 18),
    200: tom(variavel, 'white', 35),
    300: tom(variavel, 'white', 55),
    400: tom(variavel, 'white', 78),
    500: `var(${variavel})`,
    600: tom(variavel, 'black', 85),
    700: tom(variavel, 'black', 70),
    800: tom(variavel, 'black', 55),
    900: tom(variavel, 'black', 40),
    950: tom(variavel, 'black', 25),
  };
}

/**
 * Preset do PrimeNG construído a partir dos tokens semânticos do guia de estilo.
 *
 * - A escala `primary` vem do turquesa institucional, usado em ícones, bordas e indicadores.
 * - A cor de ação (`primary.color`, botões, foco, item selecionado) é o `primary-dark`, porque
 *   o turquesa não tem contraste WCAG AA com branco para texto pequeno.
 * - Superfícies e textos usam fundo secundário, borda e cores de texto do guia.
 * - Modo escuro desligado: só o esquema claro é definido (ver `darkModeSelector` em app.config).
 */
export const PresetCesjo = definePreset(Aura, {
  primitive: {
    borderRadius: {
      none: '0',
      xs: '2px',
      sm: 'var(--sgi-radius-sm)',
      md: 'var(--sgi-radius-md)',
      lg: 'var(--sgi-radius-lg)',
      xl: 'var(--sgi-radius-xl)',
    },
  },
  semantic: {
    primary: {
      ...escala('--sgi-color-primary'),
      color: 'var(--sgi-color-primary-dark)',
      contrastColor: 'var(--sgi-color-white)',
      hoverColor: tom('--sgi-color-primary-dark', 'black', 85),
      activeColor: tom('--sgi-color-primary-dark', 'black', 70),
    },
    focusRing: {
      width: '2px',
      style: 'solid',
      color: 'var(--sgi-color-primary-dark)',
      offset: '2px',
      shadow: 'none',
    },
    surface: {
      0: 'var(--sgi-color-background-primary)',
      50: 'var(--sgi-color-background-secondary)',
      100: tom('--sgi-color-border', 'white', 60),
      200: 'var(--sgi-color-border)',
      300: tom('--sgi-color-border', 'black', 90),
      400: tom('--sgi-color-text-secondary', 'white', 60),
      500: 'var(--sgi-color-text-secondary)',
      600: tom('--sgi-color-text-secondary', 'black', 85),
      700: 'var(--sgi-color-text-primary)',
      800: tom('--sgi-color-text-primary', 'black', 80),
      900: tom('--sgi-color-text-primary', 'black', 60),
      950: tom('--sgi-color-text-primary', 'black', 40),
    },
    highlight: {
      background: tom('--sgi-color-primary', 'white', 15),
      focusBackground: tom('--sgi-color-primary', 'white', 25),
      color: 'var(--sgi-color-primary-dark)',
      focusColor: 'var(--sgi-color-primary-dark)',
    },
    text: {
      color: 'var(--sgi-color-text-primary)',
      hoverColor: 'var(--sgi-color-text-primary)',
      mutedColor: 'var(--sgi-color-text-secondary)',
      hoverMutedColor: 'var(--sgi-color-text-primary)',
    },
    formField: {
      borderColor: 'var(--sgi-color-border)',
      focusBorderColor: 'var(--sgi-color-primary-dark)',
      color: 'var(--sgi-color-text-primary)',
    },
  },
});
