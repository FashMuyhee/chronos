import { SegmentedButton, SingleChoiceSegmentedButtonRow } from '@expo/ui/jetpack-compose';
import { fillMaxWidth, padding } from '@expo/ui/jetpack-compose/modifiers';

import ChronosAlarms from '../../../../modules/chronos-alarms';
import { useAlarms } from '../../alarms/hooks/useAlarms';
import { icons, RowItem, SectionTitle, Txt, useTheme } from '../../../ui';

/** Default ringtone (Android picker) and the volume ramp length. */
export function SoundSection() {
  const { settings: s, saveSettings: save } = useAlarms();
  const { colors } = useTheme();
  return (
    <>
      <SectionTitle>Sound</SectionTitle>
      <RowItem
        position="single"
        icon={icons.music}
        title="Default sound"
        value={s.defaultSoundTitle ?? ChronosAlarms.getDefaultSoundTitle()}
        onPress={async () => {
          const result = await ChronosAlarms.pickSound(s.defaultSoundUri);
          if (!result.cancelled) save({ defaultSoundUri: result.uri, defaultSoundTitle: result.title });
        }}
      />
      <Txt variant="bodyLarge" color={colors.onSurface} modifiers={[padding(28, 16, 28, 8)]}>
        Get louder over
      </Txt>
      <SingleChoiceSegmentedButtonRow modifiers={[fillMaxWidth(), padding(16, 0, 16, 0)]}>
        {[0, 15, 30, 60].map((value) => (
          <SegmentedButton key={value} selected={s.rampSeconds === value} onClick={() => save({ rampSeconds: value })}>
            <SegmentedButton.Label>
              <Txt variant="labelLarge">{value === 0 ? 'Off' : `${value} s`}</Txt>
            </SegmentedButton.Label>
          </SegmentedButton>
        ))}
      </SingleChoiceSegmentedButtonRow>
    </>
  );
}
