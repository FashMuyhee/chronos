import { Icon, Row } from '@expo/ui/jetpack-compose';
import { background, clip, padding, Shapes } from '@expo/ui/jetpack-compose/modifiers';

import { icons, Txt, useTheme } from '../../../ui';

/** The pill under the time picker that always says when the alarm will ring next. */
export function ScheduleSummary({ text, ok }: { text: string; ok: boolean }) {
  const { colors } = useTheme();
  const fg = ok ? colors.onSecondaryContainer : colors.onErrorContainer;
  return (
    <Row
      verticalAlignment="center"
      horizontalArrangement={{ spacedBy: 8 }}
      modifiers={[clip(Shapes.RoundedCorner(20)), background(ok ? colors.secondaryContainer : colors.errorContainer), padding(14, 8, 18, 8)]}>
      <Icon source={icons.schedule} size={18} tint={fg} />
      <Txt variant="labelLarge" color={fg}>
        {text}
      </Txt>
    </Row>
  );
}
