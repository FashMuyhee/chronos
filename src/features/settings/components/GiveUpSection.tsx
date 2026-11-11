import { useAlarms } from '../../alarms/hooks/useAlarms';
import { icons, RowItem, SectionTitle, Stepper } from '../../../ui';

/**
 * "I can't do this right now": how often the alarm rings again and how many times
 * before it stops for the day.
 */
export function GiveUpSection() {
  const { settings: s, saveSettings: save } = useAlarms();
  return (
    <>
      <SectionTitle>If I can't do it</SectionTitle>
      <RowItem
        position="first"
        icon={icons.timer}
        title="Ring again every"
        subtitle={`${s.giveUpIntervalMinutes} min`}
        trailing={<Stepper label="minutes" value={s.giveUpIntervalMinutes} min={1} max={10} onChange={(v) => save({ giveUpIntervalMinutes: v })} />}
      />
      <RowItem
        position="last"
        icon={icons.repeat}
        title="Stop after"
        subtitle={`${s.giveUpRings} more rings`}
        trailing={<Stepper label="rings" value={s.giveUpRings} min={1} max={20} onChange={(v) => save({ giveUpRings: v })} />}
      />
    </>
  );
}
