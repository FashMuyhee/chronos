import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { AppState } from 'react-native';

import ChronosAlarms, {
  type Alarm,
  type AlarmSettings,
  type NextAlarm,
  type PermissionStatus,
} from '../../../../modules/chronos-alarms';

export type ChronosData = {
  alarms: Alarm[];
  settings: AlarmSettings;
  next: NextAlarm | null;
  snoozed: { alarmId: string; triggerAt: number }[];
  permissions: PermissionStatus;
  refresh: () => void;
  saveSettings: (patch: Partial<AlarmSettings>) => void;
};

function read() {
  return {
    alarms: ChronosAlarms.getAlarms(),
    settings: ChronosAlarms.getSettings(),
    next: ChronosAlarms.getNextAlarm(),
    snoozed: ChronosAlarms.getSnoozed(),
    permissions: ChronosAlarms.getPermissionStatus(),
  };
}

/**
 * Alarms live in native storage, because the boot receiver and the ringing service need them
 * without JS. This hook mirrors them and refreshes when native code reports a change
 * (an alarm rang, was snoozed or stopped) and when the app comes back to the foreground.
 */
export function useChronosState(): ChronosData {
  const [state, setState] = useState(read);

  const refresh = useCallback(() => setState(read()), []);

  useEffect(() => {
    const changed = ChronosAlarms.addListener('onAlarmsChanged', refresh);
    const appState = AppState.addEventListener('change', (status) => {
      if (status === 'active') refresh();
    });
    return () => {
      changed.remove();
      appState.remove();
    };
  }, [refresh]);

  const saveSettings = useCallback((patch: Partial<AlarmSettings>) => {
    ChronosAlarms.saveSettings(patch);
    setState(read());
  }, []);

  return { ...state, refresh, saveSettings };
}

export const ChronosContext = createContext<ChronosData | null>(null);

/** The app's one copy of the native alarm state, provided by the root layout. */
export function useAlarms(): ChronosData {
  const data = useContext(ChronosContext);
  if (!data) throw new Error('useAlarms must be used inside ChronosContext');
  return data;
}
