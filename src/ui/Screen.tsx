import { Box, Column, Host, Icon, IconButton, Row, Spacer } from '@expo/ui/jetpack-compose';
import {
  align,
  animateContentSize,
  background,
  fillMaxSize,
  fillMaxWidth,
  height,
  imePadding,
  onVisibilityChanged,
  padding,
  scrollDismissesKeyboard,
  verticalScroll,
  weight,
} from '@expo/ui/jetpack-compose/modifiers';
import { useState, type ReactNode } from 'react';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useTheme } from './ThemeContext';
import { Txt } from './Txt';

type ScreenProps = {
  title: string;
  /**
   * Top-level screens get the design's 32sp title, which shrinks into a compact bar once
   * the content scrolls. Inner screens start compact.
   */
  large?: boolean;
  navigation?: { icon: number; label: string; onPress: () => void };
  actions?: ReactNode;
  /** Pinned over the content at the bottom end, e.g. the New alarm FAB. */
  floating?: ReactNode;
  /** Extra empty space after the content so the last item clears `floating`. */
  bottomSpace?: number;
  children: ReactNode;
};

/**
 * One full-screen Compose host with a sticky header.
 *
 * The header stays put on the screen background while the column under it scrolls. A 1dp
 * marker at the top of the content reports when it leaves the viewport; that single event
 * shrinks a large title to the compact bar, instead of streaming scroll offsets across the
 * bridge every frame.
 *
 * `imePadding` on the scroll area shrinks it above the keyboard, so Compose brings the
 * focused text field into view on its own.
 */
export function Screen(props: ScreenProps) {
  const { dark, overrides } = useTheme();
  return (
    <Host style={{ flex: 1 }} colorScheme={dark ? 'dark' : 'light'} colorOverrides={overrides} ignoreSafeAreaKeyboardInsets>
      <ScreenBody {...props} />
    </Host>
  );
}

/**
 * The screen without its own `<Host>`, for pages that share one, such as the home pager.
 * `scroll={false}` hands the area under the header to the children as a fixed column.
 */
export function ScreenBody({
  title,
  large,
  navigation,
  actions,
  floating,
  bottomSpace = 32,
  scroll = true,
  children,
}: ScreenProps & { scroll?: boolean }) {
  const { colors } = useTheme();
  const insets = useSafeAreaInsets();
  const [scrolled, setScrolled] = useState(false);
  const expanded = large && !scrolled;

  return (
      <Box modifiers={[fillMaxSize(), background(colors.surface)]}>
        <Column modifiers={[fillMaxSize()]}>
          <Row
            verticalAlignment="center"
            modifiers={[
              fillMaxWidth(),
              background(colors.surface),
              padding(4, insets.top + (expanded ? 20 : 4), 4, expanded ? 12 : 4),
              animateContentSize(0.9, 600),
            ]}>
            {navigation ? (
              <IconButton onClick={navigation.onPress}>
                <Icon source={navigation.icon} contentDescription={navigation.label} tint={colors.onSurface} />
              </IconButton>
            ) : null}
            <Txt
              variant={expanded ? 'headlineLarge' : 'titleLarge'}
              family={expanded ? 'ChronosText' : undefined}
              color={colors.onSurface}
              maxLines={1}
              overflow="ellipsis"
              style={expanded ? { fontSize: 32, lineHeight: 40 } : undefined}
              modifiers={[weight(1), padding(navigation ? 4 : 16, 0, 8, 0)]}>
              {title}
            </Txt>
            {actions ? <Row verticalAlignment="center">{actions}</Row> : null}
          </Row>

          <Column
            modifiers={[
              fillMaxWidth(),
              weight(1),
              imePadding(),
              ...(scroll ? [verticalScroll(), scrollDismissesKeyboard()] : []),
            ]}>
            <Spacer
              modifiers={[
                fillMaxWidth(),
                height(1),
                onVisibilityChanged((visible) => setScrolled(!visible), { minFractionVisible: 1 }),
              ]}
            />
            {children}
            <Spacer modifiers={[height(insets.bottom + bottomSpace)]} />
          </Column>
        </Column>

        {floating ? <Box modifiers={[align('bottomEnd'), padding(0, 0, 16, insets.bottom + 16)]}>{floating}</Box> : null}
      </Box>
  );
}
