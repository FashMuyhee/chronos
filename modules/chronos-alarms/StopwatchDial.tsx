import { requireNativeView } from 'expo';

export type StopwatchDialProps = {
  /** Width and height in dp. */
  size?: number;
  /** Hand, passed ticks and centiseconds. */
  accentColor?: string;
  /** The main readout. */
  contentColor?: string;
  /** Five-second ticks and the lap readout. */
  mutedColor?: string;
  /** The other ticks. */
  trackColor?: string;
  /** The dot marking the last lap. */
  lapColor?: string;
  /** Fill behind the ticks. */
  faceColor?: string;
};

const NativeDial = requireNativeView<StopwatchDialProps>('ChronosAlarms', 'StopwatchDialView');

/**
 * The stopwatch face: tick marks, a sweeping hand and the readout with centiseconds. A Compose
 * view that reads the native stopwatch itself, so it must sit inside an expo-ui `<Host>`.
 */
export function StopwatchDial(props: StopwatchDialProps) {
  return <NativeDial {...props} />;
}
