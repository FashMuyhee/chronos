import { router } from 'expo-router';

import {
  AppearanceSection,
  GiveUpSection,
  PhrasesSection,
  ReliabilitySection,
  SnoozeSection,
  SoundSection,
  WakeCheckSection,
} from '../features/settings';
import { icons, Screen } from '../ui';

/** Settings route: only orders the sections; each section owns its own reads and writes. */
export default function Settings() {
  return (
    <Screen title="Settings" navigation={{ icon: icons.arrowBack, label: 'Back', onPress: () => router.back() }}>
      <AppearanceSection />
      <SnoozeSection />
      <WakeCheckSection />
      <PhrasesSection />
      <GiveUpSection />
      <SoundSection />
      <ReliabilitySection />
    </Screen>
  );
}
