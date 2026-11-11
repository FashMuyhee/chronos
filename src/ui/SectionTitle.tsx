import { padding } from '@expo/ui/jetpack-compose/modifiers';

import { useTheme } from './ThemeContext';
import { Txt } from './Txt';

export function SectionTitle({ children }: { children: string }) {
  const { colors } = useTheme();
  return (
    <Txt
      color={colors.primary}
      variant="titleSmall"
      modifiers={[padding(28, 20, 28, 8)]}>
      {children}
    </Txt>
  );
}
