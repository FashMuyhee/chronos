import { Host, Icon, IconButton } from '@expo/ui/jetpack-compose';
import { Redirect, router, useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { GlassTabScaffold, type IconName } from '../../modules/chronos-alarms';
import { AlarmsPage, useAlarms } from '../features/alarms';
import { StopwatchPage, TimerPage, useClock } from '../features/clock';
import { icons, ScreenBody, useTheme } from '../ui';

const TABS: { key: string; label: string; icon: IconName }[] = [
  { key: 'alarms', label: 'Alarms', icon: 'alarm' },
  { key: 'timer', label: 'Timer', icon: 'hourglass' },
  { key: 'stopwatch', label: 'Stopwatch', icon: 'stopwatch' },
];

const pageOf = (tab: string | undefined) => Math.max(0, TABS.findIndex((t) => t.key === tab));

/** Room under each page's last item so it clears the floating glass bar. */
const BAR_SPACE = 112;

/**
 * Home: Alarms, Timer and Stopwatch as swipeable pages in one Compose host, under Chronos's
 * frosted-glass tab bar. `?tab=timer` (from a shortcut or notification deep link) picks a page.
 */
export default function Home() {
  const data = useAlarms();
  const clock = useClock();
  const { colors, dark, overrides } = useTheme();
  const insets = useSafeAreaInsets();
  const { tab, t } = useLocalSearchParams<{ tab?: string; t?: string }>();
  const [page, setPage] = useState(() => pageOf(tab));

  // A deep link while the app is open (each carries a fresh `t`): show its page. This syncs to
  // an external event, the link, which is what an effect is for; the native pager glides there.
  useEffect(() => {
    if (!tab) return;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setPage(pageOf(tab));
  }, [tab, t]);

  if (!data.settings.setupDone) return <Redirect href="/setup" />;

  return (
    <Host style={{ flex: 1 }} colorScheme={dark ? 'dark' : 'light'} colorOverrides={overrides} ignoreSafeAreaKeyboardInsets>
      <GlassTabScaffold
        selected={page}
        labels={TABS.map((t) => t.label)}
        icons={TABS.map((t) => t.icon)}
        actionPage={0}
        actionIcon="add"
        actionLabel="New alarm"
        bottomInset={insets.bottom}
        dark={dark}
        accentColor={colors.primary}
        onAccentColor={colors.onPrimary}
        contentColor={colors.onSurfaceVariant}
        glassColor={colors.surfaceContainerHigh}
        onTabSelect={setPage}
        onPageChange={setPage}
        onAction={() => router.push({ pathname: '/alarm/[id]', params: { id: 'new' } })}>
        <ScreenBody
          title="Alarms"
          large
          bottomSpace={BAR_SPACE}
          actions={
            <IconButton onClick={() => router.push('/settings')}>
              <Icon source={icons.settings} contentDescription="Settings" tint={colors.onSurfaceVariant} />
            </IconButton>
          }>
          <AlarmsPage />
        </ScreenBody>
        <ScreenBody title="Timer" large bottomSpace={BAR_SPACE}>
          <TimerPage timer={clock.timer} />
        </ScreenBody>
        <ScreenBody title="Stopwatch" large bottomSpace={BAR_SPACE}>
          <StopwatchPage stopwatch={clock.stopwatch} />
        </ScreenBody>
      </GlassTabScaffold>
    </Host>
  );
}
