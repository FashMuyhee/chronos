import { Column, Icon, IconButton, OutlinedTextField, useNativeState } from '@expo/ui/jetpack-compose';
import { fillMaxWidth, padding } from '@expo/ui/jetpack-compose/modifiers';
import { useState } from 'react';

import { useAlarms } from '../../alarms/hooks/useAlarms';
import { fieldShape, icons, positionOf, RowItem, SectionTitle, Txt, useTheme } from '../../../ui';
import { fonts } from '../../../theme/tokens';

/** Saved phrases (one is picked per day) with inline add and delete. */
export function PhrasesSection() {
  const { settings: s, saveSettings: save } = useAlarms();
  const { colors } = useTheme();
  const [draft, setDraft] = useState('');
  const draftState = useNativeState('');

  const add = () => {
    const value = draft.trim();
    if (!value) return;
    save({ phrases: [...s.phrases, value] });
    setDraft('');
    draftState.set('');
  };

  return (
    <>
      <SectionTitle>Phrases</SectionTitle>
      <Txt variant="bodyMedium" color={colors.onSurfaceVariant} modifiers={[padding(28, 0, 28, 8)]}>
        One is picked each day. Alarms with their own phrase use that instead.
      </Txt>
      {s.phrases.map((phrase, i) => (
        <RowItem
          key={`${i}-${phrase}`}
          position={positionOf(i, s.phrases.length)}
          title={phrase}
          trailing={
            s.phrases.length > 1 ? (
              <IconButton onClick={() => save({ phrases: s.phrases.filter((_, j) => j !== i) })}>
                <Icon source={icons.delete} contentDescription={`Delete phrase: ${phrase}`} tint={colors.onSurfaceVariant} />
              </IconButton>
            ) : null
          }
        />
      ))}
      <Column modifiers={[fillMaxWidth(), padding(16, 12, 16, 0)]}>
        <OutlinedTextField
          value={draftState}
          onValueChange={setDraft}
          singleLine
          keyboardOptions={{ capitalization: 'sentences', imeAction: 'done' }}
          keyboardActions={{ onDone: add }}
          textStyle={{ fontFamily: fonts.text, fontSize: 16 }}
          shape={fieldShape()}
          modifiers={[fillMaxWidth()]}>
          <OutlinedTextField.Label>
            <Txt variant="bodyLarge">Add a phrase</Txt>
          </OutlinedTextField.Label>
          <OutlinedTextField.TrailingIcon>
            <IconButton enabled={draft.trim().length > 0} onClick={add}>
              <Icon source={icons.add} contentDescription="Add phrase" />
            </IconButton>
          </OutlinedTextField.TrailingIcon>
        </OutlinedTextField>
      </Column>
    </>
  );
}
