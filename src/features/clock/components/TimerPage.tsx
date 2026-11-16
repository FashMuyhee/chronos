import { AnimatedVisibility, Column, EnterTransition, ExitTransition, Row, Spacer } from '@expo/ui/jetpack-compose';
import { fillMaxWidth, height, padding } from '@expo/ui/jetpack-compose/modifiers';
import { useState } from 'react';
import { useWindowDimensions } from 'react-native';

import ChronosAlarms, { MorphButton, TimerFace, type TimerState } from '../../../../modules/chronos-alarms';
import { fonts } from '../../../theme/tokens';
import { Txt, useTheme } from '../../../ui';
import { digitsToMs, MAX_DIGITS, presetDigits } from '../lib/time';
import { Digits } from './Digits';

const PRESETS = [1, 2, 5, 10];
const KEYS = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '00', '0', 'back'] as const;

/** Timer tab: number-pad entry while idle, the morphing timer face once started. */
export function TimerPage({ timer }: { timer: TimerState }) {
  return timer.status === 'idle' ? <TimerEntry /> : <TimerRunning timer={timer} />;
}

function TimerEntry() {
  const { colors } = useTheme();
  const { width } = useWindowDimensions();
  const [digits, setDigits] = useState('');
  const ms = digitsToMs(digits);
  const keyWidth = Math.floor((width - 48 - 2 * 12) / 3);

  const type = (more: string) => setDigits((d) => (d + more).replace(/^0+/, '').slice(0, MAX_DIGITS));
  const padded = digits.padStart(MAX_DIGITS, '0');
  // Typed digits are bright; the zeros still waiting to be pushed left are muted.
  const typedFrom = MAX_DIGITS - digits.length;
  const group = (from: number, unit: string) => (
    <Row verticalAlignment="bottom">
      <Digits
        size={64}
        runs={[...padded.slice(from, from + 2)].map((d, i) => ({
          text: d,
          color: from + i >= typedFrom ? colors.primary : colors.outlineVariant,
        }))}
      />
      <Txt family={fonts.text} color={colors.onSurfaceVariant} style={{ fontSize: 22 }} modifiers={[padding(2, 0, 10, 12)]}>
        {unit}
      </Txt>
    </Row>
  );

  return (
    <Column horizontalAlignment="center" modifiers={[fillMaxWidth()]}>
      <Row verticalAlignment="bottom">
        {group(0, 'h')}
        {group(2, 'm')}
        {group(4, 's')}
      </Row>
      <Spacer modifiers={[height(14)]} />
      <Row horizontalArrangement={{ spacedBy: 8 }}>
        {PRESETS.map((minutes) => (
          <MorphButton
            key={minutes}
            width={78}
            height={40}
            label={`${minutes} min`}
            fontSize={15}
            containerColor={colors.secondaryContainer}
            contentColor={colors.onSecondaryContainer}
            onPress={() => setDigits(presetDigits(minutes))}
          />
        ))}
      </Row>
      <Spacer modifiers={[height(20)]} />
      <Column verticalArrangement={{ spacedBy: 12 }}>
        {[0, 1, 2, 3].map((row) => (
          <Row key={row} horizontalArrangement={{ spacedBy: 12 }}>
            {KEYS.slice(row * 3, row * 3 + 3).map((key) =>
              key === 'back' ? (
                <MorphButton
                  key={key}
                  width={keyWidth}
                  height={68}
                  icon="backspace"
                  accessibilityLabel="Delete"
                  iconSize={28}
                  containerColor={colors.tertiaryContainer}
                  contentColor={colors.onTertiaryContainer}
                  onPress={() => setDigits((d) => d.slice(0, -1))}
                />
              ) : (
                <MorphButton
                  key={key}
                  width={keyWidth}
                  height={68}
                  label={key}
                  display
                  fontSize={30}
                  containerColor={colors.surfaceContainerHigh}
                  contentColor={colors.onSurface}
                  onPress={() => type(key)}
                />
              ),
            )}
          </Row>
        ))}
      </Column>
      <Spacer modifiers={[height(20)]} />
      <AnimatedVisibility
        visible={ms > 0}
        enterTransition={EnterTransition.scaleIn({ initialScale: 0.4 }).plus(EnterTransition.fadeIn())}
        exitTransition={ExitTransition.scaleOut({ targetScale: 0.4 }).plus(ExitTransition.fadeOut())}>
        <MorphButton
          width={keyWidth * 2 + 12}
          height={80}
          icon="play"
          label="Start"
          iconSize={30}
          fontSize={22}
          containerColor={colors.primary}
          contentColor={colors.onPrimary}
          onPress={() => {
            ChronosAlarms.startTimer(ms);
            setDigits('');
          }}
        />
      </AnimatedVisibility>
    </Column>
  );
}

function TimerRunning({ timer }: { timer: TimerState }) {
  const { colors } = useTheme();
  const { width } = useWindowDimensions();
  const running = timer.status === 'running';
  const ringing = timer.status === 'ringing';
  const face = Math.min(width - 56, 320);

  return (
    <Column horizontalAlignment="center" modifiers={[fillMaxWidth()]}>
      <Spacer modifiers={[height(8)]} />
      <TimerFace
        size={face}
        accentColor={colors.primary}
        containerColor={colors.primaryContainer}
        onContainerColor={colors.onPrimaryContainer}
        trackColor={colors.surfaceContainerHighest}
        alertColor={colors.tertiary}
        alertContainerColor={colors.tertiaryContainer}
        onAlertContainerColor={colors.onTertiaryContainer}
      />
      <Spacer modifiers={[height(36)]} />
      {ringing ? (
        <Row verticalAlignment="center" horizontalArrangement={{ spacedBy: 12 }}>
          <MorphButton
            width={112}
            height={80}
            label="+1 min"
            fontSize={18}
            containerColor={colors.surfaceContainerHigh}
            contentColor={colors.onSurface}
            onPress={() => ChronosAlarms.addTimerMinute()}
          />
          <MorphButton
            width={176}
            height={80}
            icon="stop"
            label="Stop"
            iconSize={28}
            fontSize={22}
            containerColor={colors.tertiary}
            contentColor={colors.onTertiary}
            onPress={() => ChronosAlarms.resetTimer()}
          />
        </Row>
      ) : (
        <Row verticalAlignment="center" horizontalArrangement={{ spacedBy: 12 }}>
          <MorphButton
            width={72}
            height={72}
            icon="close"
            accessibilityLabel="Cancel timer"
            containerColor={colors.surfaceContainerHigh}
            contentColor={colors.onSurfaceVariant}
            onPress={() => ChronosAlarms.resetTimer()}
          />
          <MorphButton
            width={144}
            height={88}
            // A squircle while running, a full pill while paused.
            roundness={running ? 0.3 : 0.5}
            icon={running ? 'pause' : 'play'}
            accessibilityLabel={running ? 'Pause' : 'Resume'}
            iconSize={36}
            containerColor={running ? colors.primaryContainer : colors.primary}
            contentColor={running ? colors.onPrimaryContainer : colors.onPrimary}
            onPress={() => (running ? ChronosAlarms.pauseTimer() : ChronosAlarms.resumeTimer())}
          />
          <MorphButton
            width={96}
            height={72}
            label="+1 min"
            fontSize={17}
            containerColor={colors.secondaryContainer}
            contentColor={colors.onSecondaryContainer}
            onPress={() => ChronosAlarms.addTimerMinute()}
          />
        </Row>
      )}
    </Column>
  );
}
