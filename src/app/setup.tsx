import { Box, Button, Column, FilledTonalButton, Icon, Row, TextButton } from '@expo/ui/jetpack-compose';
import {
  background,
  clip,
  fillMaxWidth,
  height,
  padding,
  Shapes,
  size,
  weight,
} from '@expo/ui/jetpack-compose/modifiers';
import { router } from 'expo-router';

import ChronosAlarms from '../../modules/chronos-alarms';
import { useAlarms, useNow } from '../features/alarms';
import { icons, positionOf, RowItem, Screen, TickClock, Txt, useTheme, withAlpha } from '../ui';

/**
 * Permission checklist. Shown as "Welcome" on first run (the list redirects here until
 * setup is done) and as "Reliability" when opened from Settings.
 */
export default function Setup() {
  const data = useAlarms();
  const { colors } = useTheme();
  const now = useNow();
  const p = data.permissions;
  const firstRun = !data.settings.setupDone;

  const items: {
    key: string;
    icon: number;
    title: string;
    hint: string;
    ok: boolean;
    optional?: boolean;
    fix: () => void | Promise<unknown>;
  }[] = [
    {
      key: 'notifications',
      icon: icons.notifications,
      title: 'Notifications',
      hint: 'So the alarm can appear',
      ok: p.notifications,
      fix: async () => {
        const granted = await ChronosAlarms.requestNotifications();
        if (!granted) ChronosAlarms.openNotificationSettings();
        data.refresh();
      },
    },
    {
      key: 'fullScreen',
      icon: icons.alarm,
      title: 'Full-screen alarm',
      hint: 'Shows over the lock screen',
      ok: p.fullScreen,
      fix: () => ChronosAlarms.openFullScreenSettings(),
    },
    {
      key: 'exact',
      icon: icons.schedule,
      title: 'Exact timing',
      hint: 'Rings on the minute, not roughly',
      ok: p.exactAlarms,
      fix: () => ChronosAlarms.openExactAlarmSettings(),
    },
    {
      key: 'battery',
      icon: icons.battery,
      title: 'Battery: Unrestricted',
      hint: 'Stops the phone putting Chronos to sleep',
      ok: p.batteryUnrestricted,
      fix: () => ChronosAlarms.requestBatteryUnrestricted(),
    },
    {
      key: 'mic',
      icon: icons.mic,
      title: 'Microphone (optional)',
      hint: 'Only to say your phrase out loud',
      ok: p.microphone,
      optional: true,
      fix: async () => {
        await ChronosAlarms.requestMicrophone();
        data.refresh();
      },
    },
  ];

  const ready = items.every((i) => i.ok || i.optional);
  const finish = () => {
    data.saveSettings({ setupDone: true });
    if (router.canGoBack()) router.back();
    else router.replace('/');
  };

  return (
    <Screen
      title={firstRun ? 'Welcome' : 'Reliability'}
      large={firstRun}
      navigation={firstRun ? undefined : { icon: icons.arrowBack, label: 'Back', onPress: () => router.back() }}>
      {firstRun ? (
        <Row verticalAlignment="center" horizontalArrangement={{ spacedBy: 16 }} modifiers={[fillMaxWidth(), padding(20, 0, 24, 20)]}>
          <Box contentAlignment="center" modifiers={[size(72, 72), clip(Shapes.Material.Cookie9Sided), background(colors.primaryContainer)]}>
            <TickClock
              size={58}
              date={new Date(now)}
              lit={colors.primary}
              dim={withAlpha(colors.onPrimaryContainer, 0.28)}
              hand={colors.onPrimaryContainer}
              minute={colors.primary}
            />
          </Box>
          <Txt variant="bodyLarge" color={colors.onSurfaceVariant} modifiers={[weight(1)]}>
            A few permissions so Chronos rings every time: locked, on silent, or just restarted.
          </Txt>
        </Row>
      ) : null}

      {items.map((item, i) => (
        <RowItem
          key={item.key}
          position={positionOf(i, items.length)}
          icon={item.icon}
          title={item.title}
          subtitle={item.ok ? 'Allowed' : item.hint}
          highlighted={!item.ok && !item.optional}
          trailing={
            item.ok ? (
              <Box contentAlignment="center" modifiers={[size(36, 36), clip(Shapes.Circle), background(colors.primary)]}>
                <Icon source={icons.check} size={20} tint={colors.onPrimary} contentDescription="Allowed" />
              </Box>
            ) : item.optional ? (
              <TextButton onClick={() => void item.fix()}>
                <Txt variant="labelLarge">Allow</Txt>
              </TextButton>
            ) : (
              <FilledTonalButton onClick={() => void item.fix()}>
                <Txt variant="labelLarge">Allow</Txt>
              </FilledTonalButton>
            )
          }
        />
      ))}

      <Row verticalAlignment="top" horizontalArrangement={{ spacedBy: 12 }} modifiers={[fillMaxWidth(), padding(28, 16, 28, 0)]}>
        <Icon source={icons.info} size={20} tint={colors.onSurfaceVariant} />
        <Txt variant="bodyMedium" color={colors.onSurfaceVariant} modifiers={[weight(1)]}>
          Chronos can't read alarms from your phone's Clock app. Turn those off once your Chronos alarms are set, so you don't get two.
        </Txt>
      </Row>

      <Column modifiers={[fillMaxWidth(), padding(16, 32, 16, 0)]}>
        <Button modifiers={[fillMaxWidth(), height(56)]} onClick={finish}>
          <Txt variant="labelLarge" style={{ fontSize: 16 }}>
            {firstRun ? (ready ? 'Continue' : 'Continue anyway') : 'Done'}
          </Txt>
        </Button>
      </Column>
    </Screen>
  );
}
