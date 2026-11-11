import { Column, Icon, IconButton, Row, Switch } from '@expo/ui/jetpack-compose';
import { background, clickable, clip, fillMaxWidth, padding, weight } from '@expo/ui/jetpack-compose/modifiers';
import { useState } from 'react';

import type { Alarm, Challenge } from '../../../../modules/chronos-alarms';
import { challengeLabel, describeSchedule, formatTime } from '../../../lib/format';
import { fonts } from '../../../theme/tokens';
import { ConfirmDialog, groupShape, icons, positionOf, Txt, useTheme } from '../../../ui';

type Props = {
  alarm: Alarm;
  /** The check that will actually run: the alarm's own, or the default from Settings. */
  challenge: Challenge;
  index: number;
  count: number;
  onPress: (alarm: Alarm) => void;
  onToggle: (alarm: Alarm, enabled: boolean) => void;
  onDelete: (alarm: Alarm) => void;
};

const challengeIcon: Partial<Record<Challenge, number>> = {
  type: icons.keyboard,
  say: icons.mic,
  secret: icons.lock,
};

/** One alarm in a grouped list, laid out like the design: wide time, label, schedule line. */
export function AlarmRow({ alarm, challenge, index, count, onPress, onToggle, onDelete }: Props) {
  const { colors } = useTheme();
  const [confirming, setConfirming] = useState(false);
  const on = alarm.enabled;
  const muted = colors.onSurfaceVariant;
  const check = challenge !== 'off' ? challengeLabel(challenge) : null;
  const checkIcon = challengeIcon[challenge];

  return (
    <Row
      verticalAlignment="center"
      horizontalArrangement={{ spacedBy: 12 }}
      modifiers={[
        fillMaxWidth(),
        padding(16, 1, 16, 1),
        clip(groupShape(positionOf(index, count))),
        background(on ? colors.surfaceContainer : colors.surfaceContainerLow),
        clickable(() => onPress(alarm)),
        padding(20, 14, 16, 14),
      ]}>
      <Column modifiers={[weight(1)]} verticalArrangement={{ spacedBy: 4 }}>
        <Row verticalAlignment="bottom" horizontalArrangement={{ spacedBy: 10 }}>
          <Txt
            variant="displaySmall"
            family={fonts.display}
            color={on ? colors.onSurface : muted}
            style={{ fontSize: 44, lineHeight: 48 }}>
            {formatTime(alarm.hour, alarm.minute)}
          </Txt>
          {alarm.label ? (
            <Txt variant="bodyLarge" color={muted} maxLines={1} overflow="ellipsis" modifiers={[weight(1), padding(0, 0, 0, 6)]}>
              {alarm.label}
            </Txt>
          ) : null}
        </Row>
        <Row verticalAlignment="center" horizontalArrangement={{ spacedBy: 6 }}>
          <Txt variant="bodyMedium" color={muted} maxLines={1}>
            {on ? describeSchedule(alarm) : `${describeSchedule(alarm)} · Off`}
          </Txt>
          {on && check ? (
            <>
              <Txt variant="bodyMedium" color={muted}>
                ·
              </Txt>
              {checkIcon !== undefined ? <Icon source={checkIcon} size={18} tint={muted} /> : null}
              <Txt variant="bodyMedium" color={muted} maxLines={1}>
                {check}
              </Txt>
            </>
          ) : null}
        </Row>
      </Column>
      <IconButton onClick={() => setConfirming(true)}>
        <Icon source={icons.delete} size={22} tint={muted} contentDescription={`Delete ${formatTime(alarm.hour, alarm.minute)} alarm`} />
      </IconButton>
      <Switch value={on} onCheckedChange={(v) => onToggle(alarm, v)}>
        {on ? (
          <Switch.ThumbContent>
            <Icon source={icons.check} size={16} />
          </Switch.ThumbContent>
        ) : null}
      </Switch>
      {confirming ? (
        <ConfirmDialog
          icon={icons.delete}
          title="Delete this alarm?"
          message={`${formatTime(alarm.hour, alarm.minute)}${alarm.label ? ` · ${alarm.label}` : ''} will be removed and won't ring again. You can't undo this.`}
          confirmLabel="Delete"
          destructive
          onConfirm={() => onDelete(alarm)}
          onDismiss={() => setConfirming(false)}
        />
      ) : null}
    </Row>
  );
}
