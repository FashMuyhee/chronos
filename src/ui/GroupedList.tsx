import { Column, Icon, Row } from '@expo/ui/jetpack-compose';
import {
  background,
  clickable,
  clip,
  defaultMinSize,
  fillMaxWidth,
  padding,
  Shapes,
  weight,
} from '@expo/ui/jetpack-compose/modifiers';
import type { ReactNode } from 'react';

import { useTheme } from './ThemeContext';
import { Txt } from './Txt';

export type GroupPosition = 'single' | 'first' | 'middle' | 'last';

export function positionOf(index: number, count: number): GroupPosition {
  if (count === 1) return 'single';
  if (index === 0) return 'first';
  if (index === count - 1) return 'last';
  return 'middle';
}

/** Material 3 Expressive grouped list: large outer corners, small inner ones. */
export function groupShape(position: GroupPosition) {
  const big = 20;
  const small = 4;
  switch (position) {
    case 'single':
      return Shapes.RoundedCorner(big);
    case 'first':
      return Shapes.RoundedCorner({ topStart: big, topEnd: big, bottomStart: small, bottomEnd: small });
    case 'last':
      return Shapes.RoundedCorner({ topStart: small, topEnd: small, bottomStart: big, bottomEnd: big });
    default:
      return Shapes.RoundedCorner(small);
  }
}

type RowItemProps = {
  position: GroupPosition;
  icon?: number;
  title: string;
  subtitle?: string | null;
  value?: string | null;
  trailing?: ReactNode;
  highlighted?: boolean;
  onPress?: () => void;
};

/** A row in a grouped list: optional icon, title + subtitle, and a value or control at the end. */
export function RowItem({ position, icon, title, subtitle, value, trailing, highlighted, onPress }: RowItemProps) {
  const { colors } = useTheme();
  const modifiers = [
    fillMaxWidth(),
    padding(16, 1, 16, 1),
    clip(groupShape(position)),
    background(highlighted ? colors.surfaceContainerHigh : colors.surfaceContainer),
    ...(onPress ? [clickable(onPress)] : []),
    defaultMinSize({ minHeight: 56 }),
    padding(16, 10, 16, 10),
  ];
  return (
    <Row modifiers={modifiers} verticalAlignment="center" horizontalArrangement={{ spacedBy: 16 }}>
      {icon !== undefined && <Icon source={icon} size={24} tint={colors.onSurfaceVariant} />}
      <Column modifiers={[weight(1)]}>
        <Txt color={colors.onSurface} variant="bodyLarge">
          {title}
        </Txt>
        {subtitle ? (
          <Txt color={colors.onSurfaceVariant} variant="bodyMedium">
            {subtitle}
          </Txt>
        ) : null}
      </Column>
      {value ? (
        <Txt color={colors.onSurfaceVariant} variant="bodyMedium" maxLines={1}>
          {value}
        </Txt>
      ) : null}
      {trailing}
    </Row>
  );
}
