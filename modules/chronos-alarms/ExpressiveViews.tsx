import { requireNativeView } from 'expo';
import type { ReactNode } from 'react';

/**
 * Chronos's own expressive Compose views. Like expo-ui components they join the Compose tree,
 * so they must be inside an expo-ui `<Host>`. Colors are theme roles as hex strings.
 */

export type IconName =
  | 'add'
  | 'alarm'
  | 'backspace'
  | 'close'
  | 'flag'
  | 'hourglass'
  | 'pause'
  | 'play'
  | 'restart'
  | 'stop'
  | 'stopwatch';

export type GlassTabScaffoldProps = {
  selected: number;
  labels: string[];
  icons: IconName[];
  /** The page whose tab bar shows the round action button; omit for none. */
  actionPage?: number;
  actionIcon?: IconName;
  actionLabel?: string;
  /** Height of the gesture navigation area, in dp. */
  bottomInset?: number;
  dark?: boolean;
  accentColor?: string;
  onAccentColor?: string;
  contentColor?: string;
  glassColor?: string;
  /** A tab was tapped. */
  onTabSelect?: (index: number) => void;
  /** A swipe moved to another page. */
  onPageChange?: (index: number) => void;
  onAction?: () => void;
  /** One child per page, each normally a full-screen expo-ui view. */
  children: ReactNode;
};

type IndexEvent = { nativeEvent: { index: number } };

type NativeScaffoldProps = Omit<GlassTabScaffoldProps, 'onTabSelect' | 'onPageChange' | 'onAction'> & {
  onTabSelect?: (event: IndexEvent) => void;
  onPageChange?: (event: IndexEvent) => void;
  onAction?: () => void;
};

const NativeScaffold = requireNativeView<NativeScaffoldProps>('ChronosAlarms', 'GlassTabScaffoldView');

/**
 * Swipeable full-screen pages under a floating frosted-glass tab bar. `selected` drives the
 * native pager; a swipe reports back through `onPageChange`.
 */
export function GlassTabScaffold({ onTabSelect, onPageChange, ...rest }: GlassTabScaffoldProps) {
  return (
    <NativeScaffold
      {...rest}
      onTabSelect={onTabSelect ? (e) => onTabSelect(e.nativeEvent.index) : undefined}
      onPageChange={onPageChange ? (e) => onPageChange(e.nativeEvent.index) : undefined}
    />
  );
}

export type TimerFaceProps = {
  size?: number;
  accentColor?: string;
  containerColor?: string;
  onContainerColor?: string;
  trackColor?: string;
  alertColor?: string;
  alertContainerColor?: string;
  onAlertContainerColor?: string;
};

const NativeTimerFace = requireNativeView<TimerFaceProps>('ChronosAlarms', 'TimerFaceView');

/** The running timer: a turning cookie that morphs on pause and when time is up. Tap toggles. */
export function TimerFace(props: TimerFaceProps) {
  return <NativeTimerFace {...props} />;
}

export type LapListProps = {
  contentColor?: string;
  mutedColor?: string;
  containerColor?: string;
  fastestColor?: string;
  fastestContainerColor?: string;
  onFastestContainerColor?: string;
  slowestColor?: string;
  slowestContainerColor?: string;
  onSlowestContainerColor?: string;
};

const NativeLapList = requireNativeView<LapListProps>('ChronosAlarms', 'LapListView');

/** The stopwatch laps, newest first; new laps grow in at the top. Reads the native stopwatch. */
export function LapList(props: LapListProps) {
  return <NativeLapList {...props} />;
}

export type MorphButtonProps = {
  width?: number;
  height?: number;
  label?: string;
  icon?: IconName;
  accessibilityLabel?: string;
  /** Corner radius at rest as a fraction of the shorter side; 0.5 is fully round. */
  roundness?: number;
  /** Use the display cut (keypad digits). */
  display?: boolean;
  fontSize?: number;
  iconSize?: number;
  enabled?: boolean;
  containerColor?: string;
  contentColor?: string;
  borderColor?: string;
  onPress?: () => void;
};

const NativeMorphButton = requireNativeView<MorphButtonProps>('ChronosAlarms', 'MorphButtonView');

/** A button whose corners squeeze and spring back when pressed. */
export function MorphButton(props: MorphButtonProps) {
  return <NativeMorphButton {...props} />;
}
