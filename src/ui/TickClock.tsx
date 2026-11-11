import { Box, Icon } from '@expo/ui/jetpack-compose';
import { rotate, size as sizeMod } from '@expo/ui/jetpack-compose/modifiers';
import { useEffect, useState } from 'react';

const parts = {
  tick: require('../../assets/clock/tick.xml'),
  hour: require('../../assets/clock/hour.xml'),
  minute: require('../../assets/clock/minute.xml'),
};

type TickClockProps = {
  size: number;
  date: Date;
  lit: string;
  dim: string;
  hand: string;
  minute: string;
};

/**
 * Logo 27, "Countdown ticks", live: the ring lights up tick by tick to the current hour and
 * the hands spring round to the current time, like the clock icons on Android and iOS.
 *
 * Each piece is a full-size vector drawn at 12 o'clock and rotated about the centre in
 * Compose, so the whole mark is native layers with no canvas or SVG library.
 *
 * Rotation uses the `rotate` modifier: in expo-ui 58, `graphicsLayer({ rotationZ })` does not
 * rotate an `Icon`. The hands' sweep is a short JS ease-out (about 50 frames, once on mount).
 */
export function TickClock({ size, date, lit, dim, hand, minute }: TickClockProps) {
  const h = date.getHours() % 12;
  const m = date.getMinutes();
  const pos = h + m / 60;
  const target = Math.floor(pos) + 1;

  const [litCount, setLitCount] = useState(0);
  const [progress, setProgress] = useState(0);

  // Sweep the hands from 12 o'clock to now once, easing out.
  useEffect(() => {
    let frame = 0;
    const startedAt = Date.now() + 150;
    const tick = () => {
      const t = Math.min(1, Math.max(0, (Date.now() - startedAt) / 900));
      setProgress(1 - Math.pow(1 - t, 3));
      if (t < 1) frame = requestAnimationFrame(tick);
    };
    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, []);

  // Light the ring one tick at a time on first show, then follow the clock.
  useEffect(() => {
    if (litCount === target) return;
    const step = setTimeout(() => setLitCount((c) => (c < target ? c + 1 : target)), litCount === 0 ? 220 : 70);
    return () => clearTimeout(step);
  }, [litCount, target]);

  return (
    <Box contentAlignment="center" modifiers={[sizeMod(size, size)]}>
      {Array.from({ length: 12 }, (_, k) => (
        <Icon
          key={k}
          source={parts.tick}
          size={size}
          tint={k < litCount ? lit : dim}
          modifiers={[rotate(k * 30)]}
        />
      ))}
      <Icon
        source={parts.minute}
        size={size}
        tint={minute}
        modifiers={[rotate((h * 360 + m * 6) * progress)]}
      />
      <Icon
        source={parts.hour}
        size={size}
        tint={hand}
        modifiers={[rotate(pos * 30 * progress)]}
      />
    </Box>
  );
}
