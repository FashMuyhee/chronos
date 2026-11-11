import { isDynamicColorAvailable, RadioButton, SegmentedButton, SingleChoiceSegmentedButtonRow } from '@expo/ui/jetpack-compose';
import { fillMaxWidth, padding } from '@expo/ui/jetpack-compose/modifiers';

import type { Palette, ThemeMode } from '../../../../modules/chronos-alarms';
import { useAlarms } from '../../alarms/hooks/useAlarms';
import { icons, RowItem, SectionTitle, Txt } from '../../../ui';

/** Burgundy or wallpaper (Material You) colors, and System / Light / Dark. */
export function AppearanceSection() {
  const { settings: s, saveSettings: save } = useAlarms();
  const options = [
    ['chronos', 'Chronos burgundy', "The app's own colors"],
    ['device', 'Match my wallpaper', isDynamicColorAvailable ? 'Material You dynamic color' : 'Needs Android 12 or newer'],
  ] as const;
  return (
    <>
      <SectionTitle>Appearance</SectionTitle>
      {options.map(([value, title, subtitle], i) => {
        const enabled = value === 'chronos' || isDynamicColorAvailable;
        const pick = () => save({ palette: value as Palette });
        return (
          <RowItem
            key={value}
            position={i === 0 ? 'first' : 'last'}
            icon={icons.palette}
            title={title}
            subtitle={subtitle}
            highlighted={s.palette === value}
            onPress={enabled ? pick : undefined}
            trailing={<RadioButton selected={s.palette === value} enabled={enabled} onClick={pick} />}
          />
        );
      })}
      <SingleChoiceSegmentedButtonRow modifiers={[fillMaxWidth(), padding(16, 12, 16, 0)]}>
        {(
          [
            ['system', 'System'],
            ['light', 'Light'],
            ['dark', 'Dark'],
          ] as const
        ).map(([value, text]) => (
          <SegmentedButton key={value} selected={s.theme === value} onClick={() => save({ theme: value as ThemeMode })}>
            <SegmentedButton.Label>
              <Txt variant="labelLarge">{text}</Txt>
            </SegmentedButton.Label>
          </SegmentedButton>
        ))}
      </SingleChoiceSegmentedButtonRow>
    </>
  );
}
