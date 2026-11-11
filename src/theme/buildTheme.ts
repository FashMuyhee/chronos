import { getMaterialColors, type MaterialColors } from '@expo/ui/jetpack-compose';

import type { Palette } from '../../modules/chronos-alarms';
import { burgundyDark, burgundyLight } from './tokens';

export type ChronosTheme = {
  dark: boolean;
  colors: MaterialColors;
  /** Passed to every <Host>; undefined lets Compose use the wallpaper colors. */
  overrides: Partial<MaterialColors> | undefined;
};

export function buildTheme(palette: Palette, dark: boolean): ChronosTheme {
  const base = getMaterialColors({ scheme: dark ? 'dark' : 'light' });
  if (palette === 'device') {
    return { dark, colors: base, overrides: undefined };
  }
  const overrides = (dark ? burgundyDark : burgundyLight) as Partial<MaterialColors>;
  return { dark, colors: { ...base, ...overrides } as MaterialColors, overrides };
}
