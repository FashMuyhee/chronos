import { NativeModule, requireNativeModule } from 'expo';

export type AlarmKind = 'once' | 'repeat' | 'date';
export type Challenge = 'off' | 'type' | 'say' | 'secret';

/** Bit flags for repeating alarms. Monday is bit 0, Sunday is bit 6. */
export const Days = {
  mon: 1 << 0,
  tue: 1 << 1,
  wed: 1 << 2,
  thu: 1 << 3,
  fri: 1 << 4,
  sat: 1 << 5,
  sun: 1 << 6,
  weekdays: 0b0011111,
  weekends: 0b1100000,
  everyDay: 0b1111111,
} as const;

export type Alarm = {
  id: string;
  hour: number;
  minute: number;
  label: string;
  kind: AlarmKind;
  /** Bitmask, see {@link Days}. Used when `kind` is `repeat`. */
  days: number;
  /** `yyyy-MM-dd`. Used when `kind` is `date`. */
  date: string | null;
  enabled: boolean;
  /** Null follows the default sound from settings, then the phone's alarm sound. */
  soundUri: string | null;
  soundTitle: string | null;
  vibrate: boolean;
  /** Null follows settings. */
  snoozeMinutes: number | null;
  /** Null follows settings. */
  challenge: Challenge | null;
  /** Null picks one of the saved phrases each day. */
  phrase: string | null;
  /** Epoch milliseconds of the next ring, or null when off or in the past. */
  nextTriggerAt: number | null;
};

export type AlarmInput = Partial<Omit<Alarm, 'nextTriggerAt'>> & Pick<Alarm, 'hour' | 'minute'>;

export type Palette = 'chronos' | 'device';
export type ThemeMode = 'system' | 'light' | 'dark';

export type AlarmSettings = {
  snoozeMinutes: number;
  maxSnoozes: number;
  challenge: Challenge;
  phrases: string[];
  giveUpIntervalMinutes: number;
  giveUpRings: number;
  giveUpRingSeconds: number;
  rampSeconds: number;
  palette: Palette;
  theme: ThemeMode;
  defaultSoundUri: string | null;
  defaultSoundTitle: string | null;
  setupDone: boolean;
};

export type NextAlarm = {
  alarmId: string;
  label: string;
  triggerAt: number;
  snoozed: boolean;
};

export type PermissionStatus = {
  notifications: boolean;
  fullScreen: boolean;
  exactAlarms: boolean;
  batteryUnrestricted: boolean;
  microphone: boolean;
};

export type PickedSound =
  | { cancelled: true }
  | { cancelled: false; uri: string | null; title: string | null };

type ChronosAlarmsEvents = {
  onAlarmsChanged: () => void;
};

declare class ChronosAlarmsModule extends NativeModule<ChronosAlarmsEvents> {
  getAlarms(): Alarm[];
  saveAlarm(alarm: AlarmInput): Alarm;
  setAlarmEnabled(id: string, enabled: boolean): Alarm | null;
  deleteAlarm(id: string): boolean;
  getNextAlarm(): NextAlarm | null;
  getSnoozed(): { alarmId: string; triggerAt: number }[];
  cancelSnooze(id: string): void;
  getSettings(): AlarmSettings;
  saveSettings(settings: Partial<AlarmSettings>): AlarmSettings;
  testAlarm(id: string, delaySeconds: number): boolean;
  getPermissionStatus(): PermissionStatus;
  requestNotifications(): Promise<boolean>;
  requestMicrophone(): Promise<boolean>;
  openFullScreenSettings(): void;
  openExactAlarmSettings(): void;
  openNotificationSettings(): void;
  requestBatteryUnrestricted(): void;
  getDefaultSoundTitle(): string;
  pickSound(currentUri: string | null): Promise<PickedSound>;
}

export default requireNativeModule<ChronosAlarmsModule>('ChronosAlarms');
