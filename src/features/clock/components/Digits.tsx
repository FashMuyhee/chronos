import { Box, Row } from '@expo/ui/jetpack-compose';
import { width } from '@expo/ui/jetpack-compose/modifiers';

import { fonts } from '../../../theme/tokens';
import { Txt } from '../../../ui';

/** Advance width of a tabular figure in the Chronos cuts (1288–1290 of 2000 units). */
const TABULAR_EM = 0.645;

export type DigitRun = { text: string; color: string; size?: number };

type DigitsProps = {
  /** One or more runs, e.g. the time and then smaller centiseconds in another color. */
  runs: DigitRun[];
  size: number;
  family?: string;
};

/**
 * Tabular digits. expo-ui can't turn on the font's `tnum` feature, and the Chronos digits are
 * proportional, so each digit sits in a cell exactly one tabular advance wide. The readout then
 * keeps its width as the numbers change instead of jittering.
 */
export function Digits({ runs, size, family = fonts.display }: DigitsProps) {
  return (
    <Row verticalAlignment="bottom">
      {runs.flatMap((run, r) => {
        const fontSize = run.size ?? size;
        return [...run.text].map((ch, i) => {
          const glyph = (
            <Txt family={family} color={run.color} style={{ fontSize, lineHeight: fontSize * 1.15 }}>
              {ch}
            </Txt>
          );
          return /\d/.test(ch) ? (
            <Box key={`${r}-${i}`} contentAlignment="bottomCenter" modifiers={[width(fontSize * TABULAR_EM)]}>
              {glyph}
            </Box>
          ) : (
            <Box key={`${r}-${i}`}>{glyph}</Box>
          );
        });
      })}
    </Row>
  );
}
