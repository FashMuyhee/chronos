import { Column, Row, Spacer } from '@expo/ui/jetpack-compose';
import { fillMaxWidth, height } from '@expo/ui/jetpack-compose/modifiers';
import { useWindowDimensions } from 'react-native';

import ChronosAlarms, { LapList, MorphButton, StopwatchDial, type StopwatchState } from '../../../../modules/chronos-alarms';
import { SectionTitle, useTheme } from '../../../ui';

/**
 * Stopwatch tab: the native dial (which draws its own readout every frame), the controls, and
 * the laps. JS only re-renders when native reports a start, pause, lap or reset.
 */
export function StopwatchPage({ stopwatch }: { stopwatch: StopwatchState }) {
  const { colors } = useTheme();
  const { width } = useWindowDimensions();
  const running = stopwatch.status === 'running';
  const idle = stopwatch.status === 'idle';

  return (
    <Column horizontalAlignment="center" modifiers={[fillMaxWidth()]}>
      <Spacer modifiers={[height(8)]} />
      <StopwatchDial
        size={Math.min(width - 48, 320)}
        accentColor={colors.primary}
        contentColor={colors.onSurface}
        mutedColor={colors.onSurfaceVariant}
        trackColor={colors.outlineVariant}
        lapColor={colors.tertiary}
        faceColor={colors.surfaceContainerLow}
      />
      <Spacer modifiers={[height(28)]} />
      <Row verticalAlignment="center" horizontalArrangement={{ spacedBy: 12 }}>
        <MorphButton
          width={104}
          height={80}
          enabled={!idle}
          icon={running ? 'flag' : 'restart'}
          label={running ? 'Lap' : 'Reset'}
          fontSize={17}
          containerColor={colors.secondaryContainer}
          contentColor={colors.onSecondaryContainer}
          onPress={() => (running ? ChronosAlarms.lapStopwatch() : ChronosAlarms.resetStopwatch())}
        />
        <MorphButton
          width={160}
          height={88}
          // A squircle while running, a full pill while stopped.
          roundness={running ? 0.3 : 0.5}
          icon={running ? 'pause' : 'play'}
          label={running ? 'Stop' : idle ? 'Start' : 'Resume'}
          iconSize={30}
          fontSize={20}
          containerColor={running ? colors.primaryContainer : colors.primary}
          contentColor={running ? colors.onPrimaryContainer : colors.onPrimary}
          onPress={() => (running ? ChronosAlarms.pauseStopwatch() : ChronosAlarms.startStopwatch())}
        />
      </Row>
      {stopwatch.laps.length > 0 ? (
        <Column modifiers={[fillMaxWidth()]}>
          <SectionTitle>Laps</SectionTitle>
          <LapList
            contentColor={colors.onSurface}
            mutedColor={colors.onSurfaceVariant}
            containerColor={colors.surfaceContainer}
            fastestColor={colors.tertiary}
            fastestContainerColor={colors.tertiaryContainer}
            onFastestContainerColor={colors.onTertiaryContainer}
            slowestColor={colors.error}
            slowestContainerColor={colors.errorContainer}
            onSlowestContainerColor={colors.onErrorContainer}
          />
        </Column>
      ) : null}
    </Column>
  );
}
