import { Text, type TextProps } from '@expo/ui/jetpack-compose';

import { fonts } from '../theme/tokens';

type TypographyStyle = NonNullable<NonNullable<TextProps['style']>['typography']>;

/** Which Google Sans Flex cut each Material type role uses. */
function familyFor(variant: TypographyStyle): string {
  if (variant.startsWith('display')) return fonts.display;
  if (variant.startsWith('title') || variant.startsWith('label')) return fonts.textMedium;
  return fonts.text;
}

type TxtProps = Omit<TextProps, 'style'> & {
  variant?: TypographyStyle;
  /** Overrides the role's family, e.g. `fonts.display` for clock digits. */
  family?: string;
  style?: TextProps['style'];
};

/**
 * expo-ui `Text` with the Chronos fonts. Every text in the app goes through this so the
 * Material type scale renders in Google Sans Flex instead of the system Roboto.
 */
export function Txt({ variant = 'bodyLarge', family, style, ...rest }: TxtProps) {
  return <Text {...rest} style={{ typography: variant, fontFamily: family ?? familyFor(variant), ...style }} />;
}
