import { ExtendedFloatingActionButton, Icon, IconButton } from '@expo/ui/jetpack-compose';
import { Redirect, router } from 'expo-router';

import ChronosAlarms, { type Alarm } from '../../modules/chronos-alarms';
import { AlarmRow, EmptyState, NextAlarmCard, useAlarms, useNow } from '../features/alarms';
import { icons, Screen, SectionTitle, Txt, useTheme } from '../ui';

const byTime = (a: Alarm, b: Alarm) => a.hour * 60 + a.minute - (b.hour * 60 + b.minute);
const byNext = (a: Alarm, b: Alarm) => (a.nextTriggerAt ?? Infinity) - (b.nextTriggerAt ?? Infinity) || byTime(a, b);

/** Home: next-alarm card, then repeating and one-time alarms, or the first-run empty state. */
export default function AlarmList() {
  const data = useAlarms();
  const { colors } = useTheme();
  const now = useNow();

  if (!data.settings.setupDone) return <Redirect href="/setup" />;

  const repeating = data.alarms.filter((a) => a.kind === 'repeat').sort(byTime);
  const oneTime = data.alarms.filter((a) => a.kind !== 'repeat').sort(byNext);
  const nextAlarm = data.next ? data.alarms.find((a) => a.id === data.next?.alarmId) : undefined;
  const open = (alarm: Alarm) => router.push({ pathname: '/alarm/[id]', params: { id: alarm.id } });
  const toggle = (alarm: Alarm, enabled: boolean) => {
    ChronosAlarms.setAlarmEnabled(alarm.id, enabled);
    data.refresh();
  };
  const remove = (alarm: Alarm) => {
    ChronosAlarms.deleteAlarm(alarm.id);
    data.refresh();
  };

  const group = (title: string, alarms: Alarm[]) =>
    alarms.length > 0 ? (
      <>
        <SectionTitle>{title}</SectionTitle>
        {alarms.map((alarm, i) => (
          <AlarmRow
            key={alarm.id}
            alarm={alarm}
            challenge={alarm.challenge ?? data.settings.challenge}
            index={i}
            count={alarms.length}
            onPress={open}
            onToggle={toggle}
            onDelete={remove}
          />
        ))}
      </>
    ) : null;

  return (
    <Screen
      title="Alarms"
      large
      bottomSpace={112}
      actions={
        <IconButton onClick={() => router.push('/settings')}>
          <Icon source={icons.settings} contentDescription="Settings" tint={colors.onSurfaceVariant} />
        </IconButton>
      }
      floating={
        <ExtendedFloatingActionButton
          onClick={() => router.push({ pathname: '/alarm/[id]', params: { id: 'new' } })}
          containerColor={colors.primaryContainer}>
          <ExtendedFloatingActionButton.Icon>
            <Icon source={icons.add} tint={colors.onPrimaryContainer} />
          </ExtendedFloatingActionButton.Icon>
          <ExtendedFloatingActionButton.Text>
            <Txt variant="labelLarge" color={colors.onPrimaryContainer} style={{ fontSize: 16 }}>
              New alarm
            </Txt>
          </ExtendedFloatingActionButton.Text>
        </ExtendedFloatingActionButton>
      }>
      {data.next && nextAlarm ? <NextAlarmCard next={data.next} alarm={nextAlarm} now={now} onChanged={data.refresh} /> : null}
      {data.alarms.length === 0 ? <EmptyState now={now} onCreated={data.refresh} /> : null}
      {group('Repeating', repeating)}
      {group('One-time', oneTime)}
    </Screen>
  );
}
