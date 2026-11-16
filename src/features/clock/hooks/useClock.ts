import { useEffect, useState } from 'react';
import { AppState } from 'react-native';

import ChronosAlarms, { type ClockState } from '../../../../modules/chronos-alarms';

/**
 * Mirrors the native timer and stopwatch. Native code owns the timestamps and reports every
 * change (from these screens, the notification buttons or the timer's alarm) as an event.
 */
export function useClock(): ClockState {
  const [state, setState] = useState(() => ChronosAlarms.getClock());

  useEffect(() => {
    const changed = ChronosAlarms.addListener('onClockChanged', setState);
    const appState = AppState.addEventListener('change', (status) => {
      if (status === 'active') setState(ChronosAlarms.getClock());
    });
    return () => {
      changed.remove();
      appState.remove();
    };
  }, []);

  return state;
}
