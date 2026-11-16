import { router } from 'expo-router';

import ChronosAlarms, { type Alarm } from '../../../../modules/chronos-alarms';
import { SectionTitle } from '../../../ui';
import { useAlarms } from '../hooks/useAlarms';
import { useNow } from '../hooks/useNow';
import { AlarmRow } from './AlarmRow';
import { EmptyState } from './EmptyState';
import { NextAlarmCard } from './NextAlarmCard';

const byTime = (a: Alarm, b: Alarm) => a.hour * 60 + a.minute - (b.hour * 60 + b.minute);
const byNext = (a: Alarm, b: Alarm) => (a.nextTriggerAt ?? Infinity) - (b.nextTriggerAt ?? Infinity) || byTime(a, b);

/** The Alarms tab: next-alarm card, then repeating and one-time alarms, or the empty state. */
export function AlarmsPage() {
  const data = useAlarms();
  const now = useNow();

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
    <>
      {data.next && nextAlarm ? <NextAlarmCard next={data.next} alarm={nextAlarm} now={now} onChanged={data.refresh} /> : null}
      {data.alarms.length === 0 ? <EmptyState now={now} onCreated={data.refresh} /> : null}
      {group('Repeating', repeating)}
      {group('One-time', oneTime)}
    </>
  );
}
