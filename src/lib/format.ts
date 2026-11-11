import { getCalendars } from 'expo-localization';

import { Days, type Alarm } from '../../modules/chronos-alarms';

const uses24h = getCalendars()[0]?.uses24hourClock ?? true;

export const is24Hour = uses24h;

/** "6:30" or "6:30 AM", following the phone's clock setting. */
export function formatTime(hour: number, minute: number): string {
  const mm = String(minute).padStart(2, '0');
  if (uses24h) return `${hour}:${mm}`;
  const h12 = hour % 12 === 0 ? 12 : hour % 12;
  return `${h12}:${mm} ${hour < 12 ? 'AM' : 'PM'}`;
}

/** "in 7 h 12 min", "in 3 min", "in 2 days". */
export function formatUntil(at: number, now = Date.now()): string {
  const minutes = Math.max(0, Math.round((at - now) / 60000));
  if (minutes < 1) return 'in less than a minute';
  if (minutes < 60) return `in ${minutes} min`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) {
    const rest = minutes % 60;
    return rest ? `in ${hours} h ${rest} min` : `in ${hours} h`;
  }
  const days = Math.round(hours / 24);
  return `in ${days} ${days === 1 ? 'day' : 'days'}`;
}

const dayNames = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

/** "Thu 12 Nov", or "Today" / "Tomorrow". */
export function formatDay(at: number, now = Date.now()): string {
  const d = new Date(at);
  const today = new Date(now);
  const tomorrow = new Date(now + 86400000);
  if (sameDay(d, today)) return 'Today';
  if (sameDay(d, tomorrow)) return 'Tomorrow';
  const weekday = dayNames[(d.getDay() + 6) % 7];
  return `${weekday} ${d.getDate()} ${monthNames[d.getMonth()]}`;
}

export function formatIsoDate(iso: string): string {
  const [y, m, d] = iso.split('-').map(Number);
  const date = new Date(y, m - 1, d);
  const weekday = dayNames[(date.getDay() + 6) % 7];
  return `${weekday} ${d} ${monthNames[m - 1]} ${y}`;
}

export function toIsoDate(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

/** "Weekdays", "Mon, Wed, Thu", "Every day". */
export function formatDays(mask: number): string {
  if (mask === Days.everyDay) return 'Every day';
  if (mask === Days.weekdays) return 'Weekdays';
  if (mask === Days.weekends) return 'Weekends';
  const picked = dayNames.filter((_, i) => mask & (1 << i));
  return picked.length ? picked.join(', ') : 'No days picked';
}

/** The schedule line under an alarm's time. */
export function describeSchedule(alarm: Alarm): string {
  switch (alarm.kind) {
    case 'repeat':
      return formatDays(alarm.days);
    case 'date':
      return alarm.date ? formatIsoDate(alarm.date) : 'No date picked';
    default:
      return alarm.nextTriggerAt ? `${formatDay(alarm.nextTriggerAt)}, once` : 'Once';
  }
}

export function challengeLabel(challenge: Alarm['challenge']): string | null {
  switch (challenge) {
    case 'type':
      return 'Type phrase';
    case 'say':
      return 'Say phrase';
    case 'secret':
      return 'Secret phrase';
    case 'off':
      return 'Slide to stop';
    default:
      return null;
  }
}

function sameDay(a: Date, b: Date) {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}
