import { AssistChip, Box, Column, Row } from '@expo/ui/jetpack-compose';
import { background, clickable, clip, fillMaxWidth, padding, Shapes, size } from '@expo/ui/jetpack-compose/modifiers';

import { Days } from '../../../../modules/chronos-alarms';
import { Txt, useTheme } from '../../../ui';

const letters = ['M', 'T', 'W', 'T', 'F', 'S', 'S'];
const names = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

/** Seven day circles (Mon = bit 0) with Weekdays / Weekends / Every day presets. */
export function DayPicker({ days, onChange }: { days: number; onChange: (days: number) => void }) {
  const { colors } = useTheme();
  return (
    <Column
      verticalArrangement={{ spacedBy: 14 }}
      modifiers={[fillMaxWidth(), padding(16, 12, 16, 0), clip(Shapes.RoundedCorner(20)), background(colors.surfaceContainer), padding(12, 16, 12, 16)]}>
      <Row modifiers={[fillMaxWidth()]} horizontalArrangement="spaceBetween">
        {letters.map((letter, i) => {
          const on = (days & (1 << i)) !== 0;
          return (
            <Box
              key={i}
              contentAlignment="center"
              modifiers={[
                size(44, 44),
                clip(Shapes.Circle),
                background(on ? colors.primary : colors.surfaceContainerHighest, { animationSpec: { $type: 'tween', durationMillis: 150 } }),
                clickable(() => onChange(days ^ (1 << i))),
              ]}>
              <Txt variant="labelLarge" color={on ? colors.onPrimary : colors.onSurfaceVariant}>
                {letter}
              </Txt>
            </Box>
          );
        })}
      </Row>
      <Row horizontalArrangement={{ spacedBy: 8 }}>
        {(
          [
            ['Weekdays', Days.weekdays],
            ['Weekends', Days.weekends],
            ['Every day', Days.everyDay],
          ] as const
        ).map(([text, mask]) => (
          <AssistChip key={text} onClick={() => onChange(mask)}>
            <AssistChip.Label>
              <Txt variant="labelLarge">{text}</Txt>
            </AssistChip.Label>
          </AssistChip>
        ))}
      </Row>
      <Txt variant="bodySmall" color={colors.onSurfaceVariant}>
        {`Rings on ${names.filter((_, i) => days & (1 << i)).join(', ') || 'no days yet'}`}
      </Txt>
    </Column>
  );
}
