import { Box, Column, Icon, Row } from '@expo/ui/jetpack-compose';
import {
  background,
  clickable,
  clip,
  fillMaxWidth,
  padding,
  Shapes,
  size,
  weight,
} from '@expo/ui/jetpack-compose/modifiers';

import ChronosAlarms, { Days, type AlarmInput } from '../../../../modules/chronos-alarms';
import { formatTime } from '../../../lib/format';
import { fonts } from '../../../theme/tokens';
import { groupShape, icons, positionOf, SectionTitle, TickClock, Txt, useTheme, withAlpha } from '../../../ui';

const starters: { input: AlarmInput; schedule: string }[] = [
  { input: { hour: 6, minute: 30, kind: 'repeat', days: Days.weekdays, label: 'Work' }, schedule: 'Weekdays' },
  { input: { hour: 7, minute: 0, kind: 'once' }, schedule: 'Once, at the next 7:00' },
  { input: { hour: 9, minute: 0, kind: 'repeat', days: Days.weekends, label: 'Weekend' }, schedule: 'Weekends' },
];

type Props = {
  now: number;
  onCreated: () => void;
};

/**
 * First run: the live Chronos mark, then one-tap starter alarms shown the way they'll look
 * in the list, so an empty screen already teaches the layout.
 */
export function EmptyState({ now, onCreated }: Props) {
  const { colors } = useTheme();

  const add = (input: AlarmInput) => {
    ChronosAlarms.saveAlarm({ ...input, enabled: true });
    onCreated();
  };

  return (
    <Column modifiers={[fillMaxWidth()]}>
      <Column horizontalAlignment="center" modifiers={[fillMaxWidth(), padding(24, 24, 24, 8)]} verticalArrangement={{ spacedBy: 8 }}>
        <Box
          contentAlignment="center"
          modifiers={[size(224, 224), clip(Shapes.Material.Cookie9Sided), background(colors.primaryContainer)]}>
          <TickClock
            size={176}
            date={new Date(now)}
            lit={colors.primary}
            dim={withAlpha(colors.onPrimaryContainer, 0.28)}
            hand={colors.onPrimaryContainer}
            minute={colors.primary}
          />
        </Box>
        <Txt variant="headlineSmall" color={colors.onSurface} style={{ textAlign: 'center' }} modifiers={[padding(0, 16, 0, 0)]}>
          No alarms yet
        </Txt>
        <Txt variant="bodyLarge" color={colors.onSurfaceVariant} style={{ textAlign: 'center' }}>
          Start with one of these, or tap New alarm and pick any time.
        </Txt>
      </Column>

      <SectionTitle>Quick start</SectionTitle>
      {starters.map(({ input, schedule }, i) => (
        <Row
          key={schedule}
          verticalAlignment="center"
          horizontalArrangement={{ spacedBy: 12 }}
          modifiers={[
            fillMaxWidth(),
            padding(16, 1, 16, 1),
            clip(groupShape(positionOf(i, starters.length))),
            background(colors.surfaceContainer),
            clickable(() => add(input)),
            padding(20, 12, 12, 12),
          ]}>
          <Column modifiers={[weight(1)]}>
            <Row verticalAlignment="bottom" horizontalArrangement={{ spacedBy: 10 }}>
              <Txt variant="displaySmall" family={fonts.display} color={colors.onSurface} style={{ fontSize: 36, lineHeight: 42 }}>
                {formatTime(input.hour, input.minute)}
              </Txt>
              {input.label ? (
                <Txt variant="bodyLarge" color={colors.onSurfaceVariant} modifiers={[padding(0, 0, 0, 4)]}>
                  {input.label}
                </Txt>
              ) : null}
            </Row>
            <Txt variant="bodyMedium" color={colors.onSurfaceVariant}>
              {schedule}
            </Txt>
          </Column>
          <Box
            contentAlignment="center"
            modifiers={[size(40, 40), clip(Shapes.Circle), background(colors.secondaryContainer)]}>
            <Icon source={icons.add} size={20} tint={colors.onSecondaryContainer} contentDescription={`Add ${formatTime(input.hour, input.minute)} ${schedule}`} />
          </Box>
        </Row>
      ))}
    </Column>
  );
}
