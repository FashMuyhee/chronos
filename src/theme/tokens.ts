/**
 * Chronos burnt-burgundy Material 3 roles.
 * Keep in sync with modules/chronos-alarms/android/.../ui/Theme.kt (the ringing screen).
 */
export const burgundyLight = {
  primary: '#8C1D30',
  onPrimary: '#FFFFFF',
  primaryContainer: '#FFDADB',
  onPrimaryContainer: '#3F0010',
  inversePrimary: '#FFB2B7',
  secondary: '#775656',
  onSecondary: '#FFFFFF',
  secondaryContainer: '#F6DCDB',
  onSecondaryContainer: '#2C1516',
  tertiary: '#755A2F',
  onTertiary: '#FFFFFF',
  tertiaryContainer: '#FFDDB1',
  onTertiaryContainer: '#2A1800',
  background: '#FFF8F7',
  onBackground: '#22191A',
  surface: '#FFF8F7',
  onSurface: '#22191A',
  surfaceVariant: '#F4DDDD',
  onSurfaceVariant: '#534343',
  outline: '#857373',
  outlineVariant: '#D8C2C2',
  inverseSurface: '#382E2E',
  inverseOnSurface: '#FFEDEC',
  surfaceContainerLowest: '#FFFFFF',
  surfaceContainerLow: '#FFF0F0',
  surfaceContainer: '#FCEAEA',
  surfaceContainerHigh: '#F6E4E4',
  surfaceContainerHighest: '#F0DEDE',
} as const;

export const burgundyDark = {
  primary: '#FFB2B7',
  onPrimary: '#5F1121',
  primaryContainer: '#7A1E2C',
  onPrimaryContainer: '#FFDADB',
  inversePrimary: '#8C1D30',
  secondary: '#E6BDBC',
  onSecondary: '#44292A',
  secondaryContainer: '#5D3F40',
  onSecondaryContainer: '#FFDAD9',
  tertiary: '#E5C18D',
  onTertiary: '#422C05',
  tertiaryContainer: '#5C4200',
  onTertiaryContainer: '#FFDDB1',
  background: '#1A1112',
  onBackground: '#F0DEDF',
  surface: '#1A1112',
  onSurface: '#F0DEDF',
  surfaceVariant: '#534343',
  onSurfaceVariant: '#D8C2C2',
  outline: '#A08C8C',
  outlineVariant: '#534343',
  inverseSurface: '#F0DEDF',
  inverseOnSurface: '#382E2E',
  surfaceContainerLowest: '#140C0D',
  surfaceContainerLow: '#22191A',
  surfaceContainer: '#271D1E',
  surfaceContainerHigh: '#322828',
  surfaceContainerHighest: '#3D3232',
} as const;

/**
 * Google Sans Flex cuts embedded by the expo-font config plugin (assets/fonts, OFL).
 * Android can't set variable-font axes through expo-ui, so each cut is a static instance.
 */
export const fonts = {
  text: 'ChronosText',
  textMedium: 'ChronosTextMedium',
  /** opsz 72: large clock digits. */
  display: 'ChronosDisplay',
  /** opsz 36, weight 500: the next-alarm countdown. */
  displayMedium: 'ChronosDisplayMedium',
} as const;
