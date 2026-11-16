import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import * as SystemUI from 'expo-system-ui';
import { useEffect, useMemo } from 'react';
import { useColorScheme } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { ChronosContext, useChronosState } from '../features/alarms';
import { buildTheme } from '../theme/buildTheme';
import { ThemeContext } from '../ui';

/** Links straight to an inner screen (the "New alarm" shortcut) still get Home underneath. */
export const unstable_settings = { anchor: 'index' };

/**
 * Root of the app: one native-state source (ChronosContext), the theme built from the
 * user's palette and light/dark choice, and a header-less stack (each screen draws its own).
 */
export default function RootLayout() {
  const data = useChronosState();
  const system = useColorScheme();
  const { palette, theme: mode } = data.settings;
  const dark = mode === 'system' ? system === 'dark' : mode === 'dark';
  const theme = useMemo(() => buildTheme(palette, dark), [palette, dark]);
  const surface = theme.colors.surface;

  // The window behind the screens shows during transitions; keep it the theme surface.
  useEffect(() => {
    void SystemUI.setBackgroundColorAsync(surface);
  }, [surface]);

  return (
    <SafeAreaProvider>
      <ChronosContext.Provider value={data}>
        <ThemeContext.Provider value={theme}>
          <StatusBar style={dark ? 'light' : 'dark'} />
          <Stack
            screenOptions={{
              headerShown: false,
              animation: 'fade_from_bottom',
              contentStyle: { backgroundColor: surface },
            }}
          />
        </ThemeContext.Provider>
      </ChronosContext.Provider>
    </SafeAreaProvider>
  );
}
