/**
 * Rewrites incoming links before Expo Router sees them. The launcher shortcuts and the timer and
 * stopwatch notifications open chronos://timer and chronos://stopwatch, which are pages of the
 * home pager rather than routes of their own.
 */
export function redirectSystemPath({ path }: { path: string; initial: boolean }): string {
  try {
    const bare = path.replace(/^[a-z][a-z0-9+.-]*:\/\//i, '').replace(/^\/+/, '');
    // The stamp makes a repeat of the same link a new navigation, so Home reacts to it again.
    if (bare === 'timer' || bare === 'stopwatch') return `/?tab=${bare}&t=${Date.now()}`;
    return path;
  } catch {
    return path;
  }
}
