import { AlertDialog, Button, Icon, OutlinedButton } from '@expo/ui/jetpack-compose';

import { useTheme } from './ThemeContext';
import { Txt } from './Txt';

type ConfirmDialogProps = {
  /** Material symbol shown above the title. */
  icon: number;
  title: string;
  message: string;
  confirmLabel: string;
  /** Destructive actions (delete) get the error color on the confirm button. */
  destructive?: boolean;
  onConfirm: () => void;
  onDismiss: () => void;
};

/**
 * Material 3 confirmation dialog for actions that change or remove an alarm. Render it only
 * while it should be open; tapping outside, back, or Cancel all call `onDismiss`.
 *
 * Cancel is outlined and the action is filled, so the two read as clearly different choices;
 * destructive actions fill with the error color.
 */
export function ConfirmDialog({ icon, title, message, confirmLabel, destructive, onConfirm, onDismiss }: ConfirmDialogProps) {
  const { colors } = useTheme();
  return (
    <AlertDialog onDismissRequest={onDismiss} colors={{ containerColor: colors.surfaceContainerHigh }}>
      <AlertDialog.Icon>
        <Icon source={icon} tint={destructive ? colors.error : colors.secondary} />
      </AlertDialog.Icon>
      <AlertDialog.Title>
        <Txt variant="headlineSmall" color={colors.onSurface} style={{ textAlign: 'center' }}>
          {title}
        </Txt>
      </AlertDialog.Title>
      <AlertDialog.Text>
        <Txt variant="bodyMedium" color={colors.onSurfaceVariant}>
          {message}
        </Txt>
      </AlertDialog.Text>
      <AlertDialog.DismissButton>
        <OutlinedButton onClick={onDismiss}>
          <Txt variant="labelLarge">Cancel</Txt>
        </OutlinedButton>
      </AlertDialog.DismissButton>
      <AlertDialog.ConfirmButton>
        <Button
          onClick={() => {
            onConfirm();
            onDismiss();
          }}
          colors={destructive ? { containerColor: colors.error, contentColor: colors.onError } : undefined}>
          <Txt variant="labelLarge">{confirmLabel}</Txt>
        </Button>
      </AlertDialog.ConfirmButton>
    </AlertDialog>
  );
}
