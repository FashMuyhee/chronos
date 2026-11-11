import { FilledTonalIconButton, Icon, Row } from '@expo/ui/jetpack-compose';
import { size } from '@expo/ui/jetpack-compose/modifiers';

import { icons } from './icons';
import { useTheme } from './ThemeContext';
import { Txt } from './Txt';

/** − value + with tonal round buttons. */
export function Stepper({
  value,
  min,
  max,
  onChange,
  label,
}: {
  value: number;
  min: number;
  max: number;
  onChange: (value: number) => void;
  label: string;
}) {
  const { colors } = useTheme();
  return (
    <Row verticalAlignment="center" horizontalArrangement={{ spacedBy: 4 }}>
      <FilledTonalIconButton enabled={value > min} onClick={() => onChange(Math.max(min, value - 1))} modifiers={[size(44, 44)]}>
        <Icon source={icons.remove} size={20} contentDescription={`Fewer ${label}`} />
      </FilledTonalIconButton>
      <Txt
        color={colors.onSurface}
        variant="titleLarge"
        style={{ textAlign: 'center' }}
        modifiers={[size(36, 28)]}>
        {String(value)}
      </Txt>
      <FilledTonalIconButton enabled={value < max} onClick={() => onChange(Math.min(max, value + 1))} modifiers={[size(44, 44)]}>
        <Icon source={icons.add} size={20} contentDescription={`More ${label}`} />
      </FilledTonalIconButton>
    </Row>
  );
}
