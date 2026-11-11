import { Column, TextButton } from '@expo/ui/jetpack-compose';
import { background, clip, fillMaxWidth, padding, Shapes } from '@expo/ui/jetpack-compose/modifiers';

import ChronosAlarms, { type Alarm, type NextAlarm } from '../../../../modules/chronos-alarms';
import { formatDay, formatTime, formatUntil } from '../../../lib/format';
import { fonts } from '../../../theme/tokens';
import { Txt, useTheme } from '../../../ui';

type Props = {
  next: NextAlarm;
  alarm: Alarm;
  now: number;
  onChanged: () => void;
};

/** The primary-container card at the top of the list: how long until the next ring. */
export function NextAlarmCard({ next, alarm, now, onChanged }: Props) {
  const { colors } = useTheme();
  const at = new Date(next.triggerAt);
  const detail = `${formatDay(next.triggerAt, now)}, ${formatTime(at.getHours(), at.getMinutes())}${alarm.label ? ` · ${alarm.label}` : ''}`;

  return (
    <Column
      verticalArrangement={{ spacedBy: 2 }}
      modifiers={[
        fillMaxWidth(),
        padding(16, 4, 16, 0),
        clip(Shapes.RoundedCorner(28)),
        background(colors.primaryContainer),
        padding(24, 20, 24, next.snoozed ? 8 : 20),
      ]}>
      <Txt variant="labelLarge" color={colors.onPrimaryContainer} style={{ fontSize: 14 }}>
        {next.snoozed ? 'Snoozed' : 'Next alarm'}
      </Txt>
      <Txt variant="headlineLarge" family={fonts.displayMedium} color={colors.onPrimaryContainer} style={{ fontSize: 36, lineHeight: 44 }}>
        {formatUntil(next.triggerAt, now)}
      </Txt>
      <Txt variant="bodyLarge" color={colors.onPrimaryContainer}>
        {detail}
      </Txt>
      {next.snoozed ? (
        <TextButton
          onClick={() => {
            ChronosAlarms.cancelSnooze(alarm.id);
            onChanged();
          }}
          colors={{ contentColor: colors.onPrimaryContainer }}
          modifiers={[padding(0, 4, 0, 0)]}>
          <Txt variant="labelLarge">Cancel snooze</Txt>
        </TextButton>
      ) : null}
    </Column>
  );
}
