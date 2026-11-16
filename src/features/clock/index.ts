/**
 * Timer and stopwatch. Native code (modules/chronos-alarms, clock/) owns the timestamps, the
 * foreground service and the notifications; these components only render that state.
 */
export { StopwatchPage } from './components/StopwatchPage';
export { TimerPage } from './components/TimerPage';
export { useClock } from './hooks/useClock';
