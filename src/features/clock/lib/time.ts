/**
 * Number-pad entry for the timer. Running times are formatted natively (TimerFace, LapList,
 * StopwatchDial), so this only turns typed digits into a length.
 */

/** Up to six digits, read right to left as HH MM SS. */
export const MAX_DIGITS = 6;

/** "130" → "000130" → 0 h 1 min 30 s. Minutes and seconds may exceed 59, like the system clock. */
export function digitsToMs(digits: string): number {
  const d = digits.padStart(MAX_DIGITS, '0');
  const h = Number(d.slice(0, 2));
  const m = Number(d.slice(2, 4));
  const s = Number(d.slice(4, 6));
  return ((h * 60 + m) * 60 + s) * 1000;
}

/** The digits a preset of [minutes] types into the pad: 5 → "500" (5 min 00 s). */
export function presetDigits(minutes: number): string {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return `${h > 0 ? String(h) : ''}${h > 0 ? String(m).padStart(2, '0') : String(m)}00`;
}
