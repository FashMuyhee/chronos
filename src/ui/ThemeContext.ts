import { createContext, useContext } from 'react';

import type { ChronosTheme } from '../theme/buildTheme';

export const ThemeContext = createContext<ChronosTheme | null>(null);

export function useTheme(): ChronosTheme {
  const theme = useContext(ThemeContext);
  if (!theme) throw new Error('useTheme must be used inside ThemeContext');
  return theme;
}

/** Material colors come back as #RRGGBBAA; this swaps the alpha byte. */
export function withAlpha(color: string, alpha: number): string {
  const a = Math.round(Math.min(1, Math.max(0, alpha)) * 255)
    .toString(16)
    .padStart(2, '0');
  return `${color.slice(0, 7)}${a}`;
}
