import { RadioButton } from '@expo/ui/jetpack-compose';

import type { Challenge } from '../../../../modules/chronos-alarms';
import { useAlarms } from '../../alarms/hooks/useAlarms';
import { positionOf, RowItem, SectionTitle } from '../../../ui';

const options = [
  ['off', 'Off', 'Slide to stop'],
  ['type', 'Type the phrase', 'Shown on screen; type it exactly'],
  ['say', 'Say the phrase', 'Out loud, on-device speech'],
  ['secret', 'Secret phrase', 'Hidden; you have to remember it'],
] as const;

/** The default wake-up check; each alarm can override it in the editor. */
export function WakeCheckSection() {
  const { settings: s, saveSettings: save } = useAlarms();
  return (
    <>
      <SectionTitle>Wake-up check</SectionTitle>
      {options.map(([value, title, subtitle], i) => {
        const pick = () => save({ challenge: value as Challenge });
        return (
          <RowItem
            key={value}
            position={positionOf(i, options.length)}
            title={title}
            subtitle={subtitle}
            highlighted={s.challenge === value}
            onPress={pick}
            trailing={<RadioButton selected={s.challenge === value} onClick={pick} />}
          />
        );
      })}
    </>
  );
}
