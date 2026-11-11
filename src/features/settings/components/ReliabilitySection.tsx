import { Icon } from '@expo/ui/jetpack-compose';
import { router } from 'expo-router';

import { useAlarms } from '../../alarms/hooks/useAlarms';
import { icons, RowItem, SectionTitle, useTheme } from '../../../ui';

/** Summarises the permissions alarms depend on and links to the setup checklist. */
export function ReliabilitySection() {
  const { permissions: p } = useAlarms();
  const { colors } = useTheme();
  const allGood = p.notifications && p.fullScreen && p.exactAlarms && p.batteryUnrestricted;
  return (
    <>
      <SectionTitle>Reliability</SectionTitle>
      <RowItem
        position="single"
        icon={icons.lock}
        title={allGood ? 'Alarms will ring reliably' : 'Some permissions are missing'}
        subtitle={allGood ? 'Notifications, full screen, exact timing and battery are all set' : 'Tap to fix them'}
        onPress={() => router.push('/setup')}
        trailing={<Icon source={icons.chevronRight} tint={colors.onSurfaceVariant} />}
      />
    </>
  );
}
