import { AlertDialog, Icon, TextButton } from '@expo/ui/jetpack-compose';

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
        <TextButton onClick={onDismiss}>
          <Txt variant="labelLarge">Cancel</Txt>
        </TextButton>
      </AlertDialog.DismissButton>
      <AlertDialog.ConfirmButton>
        <TextButton
          onClick={() => {
            onConfirm();
            onDismiss();
          }}
          colors={destructive ? { contentColor: colors.error } : undefined}>
          <Txt variant="labelLarge">{confirmLabel}</Txt>
        </TextButton>
      </AlertDialog.ConfirmButton>
    </AlertDialog>
  );
}
