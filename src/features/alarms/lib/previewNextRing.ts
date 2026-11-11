import type { AlarmKind } from '../../../../modules/chronos-alarms';
import { formatDay, formatIsoDate, formatUntil } from '../../../lib/format';

/** Mirrors AlarmScheduler.nextTrigger so the summary updates as you edit. */
export function previewNextRing({
  hour,
  minute,
  kind,
  days,
  date,
}: {
  hour: number;
  minute: number;
  kind: AlarmKind;
  days: number;
  date: string | null;
}): { at: number | null; summary: string } {
  const now = Date.now();
  const candidate = new Date();
  candidate.setHours(hour, minute, 0, 0);

  if (kind === 'once') {
    if (candidate.getTime() <= now) candidate.setDate(candidate.getDate() + 1);
    const at = candidate.getTime();
    return { at, summary: `${formatDay(at, now)}, ${formatUntil(at, now)}` };
  }

  if (kind === 'repeat') {
    if (!days) return { at: null, summary: 'Pick at least one day' };
    for (let offset = 0; offset <= 7; offset++) {
      const d = new Date(candidate);
      d.setDate(candidate.getDate() + offset);
      const bit = 1 << ((d.getDay() + 6) % 7);
      if (d.getTime() > now && days & bit) {
        return { at: d.getTime(), summary: `Next: ${formatDay(d.getTime(), now)}, ${formatUntil(d.getTime(), now)}` };
      }
    }
    return { at: null, summary: 'Pick at least one day' };
  }

  if (!date) return { at: null, summary: 'Pick a date' };
  const [y, m, d] = date.split('-').map(Number);
  const at = new Date(y, m - 1, d, hour, minute, 0, 0).getTime();
  if (at <= now) return { at: null, summary: 'That time has already passed' };
  return { at, summary: `${formatIsoDate(date)}, ${formatUntil(at, now)}` };
}
