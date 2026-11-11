import { SegmentedButton, SingleChoiceSegmentedButtonRow } from '@expo/ui/jetpack-compose';
import { fillMaxWidth, padding } from '@expo/ui/jetpack-compose/modifiers';

import { useAlarms } from '../../alarms/hooks/useAlarms';
import { icons, RowItem, SectionTitle, Stepper, Txt } from '../../../ui';

/** Default snooze length and how many snoozes an alarm allows. */
export function SnoozeSection() {
  const { settings: s, saveSettings: save } = useAlarms();
  return (
    <>
      <SectionTitle>Snooze</SectionTitle>
      <SingleChoiceSegmentedButtonRow modifiers={[fillMaxWidth(), padding(16, 0, 16, 8)]}>
        {[5, 9, 10, 15].map((value) => (
          <SegmentedButton key={value} selected={s.snoozeMinutes === value} onClick={() => save({ snoozeMinutes: value })}>
            <SegmentedButton.Label>
              <Txt variant="labelLarge">{`${value} min`}</Txt>
            </SegmentedButton.Label>
          </SegmentedButton>
        ))}
      </SingleChoiceSegmentedButtonRow>
      <RowItem
        position="single"
        icon={icons.snooze}
        title="Snoozes allowed"
        subtitle={s.maxSnoozes === 0 ? 'Snooze is turned off' : null}
        trailing={<Stepper label="snoozes" value={s.maxSnoozes} min={0} max={10} onChange={(v) => save({ maxSnoozes: v })} />}
      />
    </>
  );
}
