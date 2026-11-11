import {
  Box,
  Button,
  Column,
  DatePickerDialog,
  DateTimePicker,
  FilledTonalButton,
  OutlinedButton,
  OutlinedTextField,
  RadioButton,
  SegmentedButton,
  SingleChoiceSegmentedButtonRow,
  Switch,
  TextButton,
  useNativeState,
} from '@expo/ui/jetpack-compose';
import { fillMaxWidth, padding } from '@expo/ui/jetpack-compose/modifiers';
import { router, useLocalSearchParams } from 'expo-router';
import { useMemo, useState } from 'react';

import ChronosAlarms, { Days, type Alarm, type AlarmKind, type Challenge } from '../../../modules/chronos-alarms';
import { DayPicker, previewNextRing, ScheduleSummary, useAlarms } from '../../features/alarms';
import { challengeLabel, describeSchedule, formatIsoDate, formatTime, is24Hour } from '../../lib/format';
import { ConfirmDialog, fieldShape, icons, positionOf, RowItem, Screen, SectionTitle, Txt, useTheme } from '../../ui';
import { fonts } from '../../theme/tokens';

const close = () => (router.canGoBack() ? router.back() : router.replace('/'));

/** Edit route: `/alarm/new` creates, `/alarm/<id>` edits. Unknown ids fall back to a new alarm. */
export default function EditAlarm() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const data = useAlarms();
  const alarm = id === 'new' ? null : (data.alarms.find((a) => a.id === id) ?? null);
  // Keyed so a different alarm never inherits the previous one's draft.
  return <AlarmEditor key={alarm?.id ?? 'new'} alarm={alarm} />;
}

function AlarmEditor({ alarm }: { alarm: Alarm | null }) {
  const data = useAlarms();
  const { colors } = useTheme();
  const settings = data.settings;

  const initial = useMemo(() => ({ hour: alarm?.hour ?? 7, minute: alarm?.minute ?? 0 }), [alarm]);
  const [hour, setHour] = useState(initial.hour);
  const [minute, setMinute] = useState(initial.minute);
  const [kind, setKind] = useState<AlarmKind>(alarm?.kind ?? 'once');
  const [days, setDays] = useState(alarm?.days || Days.weekdays);
  const [date, setDate] = useState<string | null>(alarm?.date ?? null);
  const [showDatePicker, setShowDatePicker] = useState(false);
  const [confirm, setConfirm] = useState<'delete' | 'turnOff' | null>(null);
  const [label, setLabel] = useState(alarm?.label ?? '');
  const [phrase, setPhrase] = useState(alarm?.phrase ?? '');
  const [soundUri, setSoundUri] = useState(alarm?.soundUri ?? null);
  const [soundTitle, setSoundTitle] = useState(alarm?.soundTitle ?? null);
  const [vibrate, setVibrate] = useState(alarm?.vibrate ?? true);
  const [snoozeMinutes, setSnoozeMinutes] = useState<number | null>(alarm?.snoozeMinutes ?? null);
  const [challenge, setChallenge] = useState<Challenge | null>(alarm?.challenge ?? null);

  const labelState = useNativeState(label);
  const phraseState = useNativeState(phrase);

  const initialPickerDate = useMemo(() => {
    const d = new Date();
    d.setHours(initial.hour, initial.minute, 0, 0);
    return d.toISOString();
  }, [initial]);

  const preview = previewNextRing({ hour, minute, kind, days, date });
  const canSave = preview.at !== null;
  const effectiveChallenge = challenge ?? settings.challenge;
  const defaultSoundTitle = settings.defaultSoundTitle ?? ChronosAlarms.getDefaultSoundTitle();

  const save = (): Alarm => {
    const saved = ChronosAlarms.saveAlarm({
      id: alarm?.id,
      hour,
      minute,
      kind,
      days,
      date: kind === 'date' ? date : null,
      label: label.trim(),
      phrase: phrase.trim() || null,
      soundUri,
      soundTitle,
      vibrate,
      snoozeMinutes,
      challenge,
      enabled: true,
    });
    data.refresh();
    return saved;
  };

  const pickSound = async () => {
    const result = await ChronosAlarms.pickSound(soundUri);
    if (result.cancelled) return;
    setSoundUri(result.uri);
    setSoundTitle(result.title);
  };

  return (
    <Screen
      title={alarm ? 'Edit alarm' : 'New alarm'}
      navigation={{ icon: icons.close, label: 'Discard and close', onPress: close }}
      actions={
        <Button
          enabled={canSave}
          onClick={() => {
            save();
            close();
          }}
          modifiers={[padding(0, 0, 8, 0)]}>
          <Txt variant="labelLarge">Save</Txt>
        </Button>
      }>
      <Box modifiers={[fillMaxWidth(), padding(16, 8, 16, 0)]} contentAlignment="center">
        <DateTimePicker
          displayedComponents="hourAndMinute"
          variant="input"
          showVariantToggle
          is24Hour={is24Hour}
          initialDate={initialPickerDate}
          onDateSelected={(d) => {
            if (!d) return;
            setHour(d.getHours());
            setMinute(d.getMinutes());
          }}
        />
      </Box>

      <Box modifiers={[fillMaxWidth(), padding(16, 4, 16, 16)]} contentAlignment="center">
        <ScheduleSummary text={preview.summary} ok={canSave} />
      </Box>

      <SingleChoiceSegmentedButtonRow modifiers={[fillMaxWidth(), padding(16, 0, 16, 0)]}>
        {(
          [
            ['once', 'Once'],
            ['repeat', 'Repeat'],
            ['date', 'Date'],
          ] as const
        ).map(([value, text]) => (
          <SegmentedButton key={value} selected={kind === value} onClick={() => setKind(value)}>
            <SegmentedButton.Label>
              <Txt variant="labelLarge">{text}</Txt>
            </SegmentedButton.Label>
          </SegmentedButton>
        ))}
      </SingleChoiceSegmentedButtonRow>

      {kind === 'repeat' ? <DayPicker days={days} onChange={setDays} /> : null}

      {kind === 'date' ? (
        <Column modifiers={[fillMaxWidth(), padding(0, 12, 0, 0)]}>
          <RowItem
            position="single"
            icon={icons.calendar}
            title="Date"
            value={date ? formatIsoDate(date) : 'Pick a date'}
            onPress={() => setShowDatePicker(true)}
          />
        </Column>
      ) : null}

      {kind === 'once' ? (
        <Column modifiers={[fillMaxWidth(), padding(28, 12, 28, 0)]} verticalArrangement={{ spacedBy: 2 }}>
          <Txt variant="titleSmall" color={colors.onSurface}>
            {`Rings once, at the next ${formatTime(hour, minute)}`}
          </Txt>
          <Txt variant="bodyMedium" color={colors.onSurfaceVariant}>
            If that time has passed today, it rings tomorrow, then switches itself off.
          </Txt>
        </Column>
      ) : null}

      <SectionTitle>Details</SectionTitle>
      <Column modifiers={[fillMaxWidth(), padding(16, 0, 16, 8)]}>
        <OutlinedTextField
          value={labelState}
          onValueChange={setLabel}
          singleLine
          keyboardOptions={{ capitalization: 'sentences', imeAction: 'done' }}
          textStyle={{ fontFamily: fonts.text, fontSize: 16 }}
          shape={fieldShape()}
          modifiers={[fillMaxWidth()]}>
          <OutlinedTextField.Label>
            <Txt variant="bodyLarge">Label</Txt>
          </OutlinedTextField.Label>
        </OutlinedTextField>
      </Column>
      <RowItem position="first" icon={icons.music} title="Sound" value={soundTitle ?? defaultSoundTitle} onPress={pickSound} />
      <RowItem
        position="last"
        icon={icons.vibration}
        title="Vibrate"
        onPress={() => setVibrate(!vibrate)}
        trailing={<Switch value={vibrate} onCheckedChange={setVibrate} />}
      />

      <SectionTitle>Snooze</SectionTitle>
      <SingleChoiceSegmentedButtonRow modifiers={[fillMaxWidth(), padding(16, 0, 16, 0)]}>
        {([null, 5, 10, 15] as const).map((value) => (
          <SegmentedButton key={String(value)} selected={snoozeMinutes === value} onClick={() => setSnoozeMinutes(value)}>
            <SegmentedButton.Label>
              <Txt variant="labelLarge">{value === null ? `Default (${settings.snoozeMinutes})` : `${value} min`}</Txt>
            </SegmentedButton.Label>
          </SegmentedButton>
        ))}
      </SingleChoiceSegmentedButtonRow>

      <SectionTitle>Wake-up check</SectionTitle>
      {(
        [
          [null, `Default (${challengeLabel(settings.challenge)})`, 'Follows Settings'],
          ['off', 'Off', 'Slide to stop'],
          ['type', 'Type the phrase', 'Shown on screen; type it exactly'],
          ['say', 'Say the phrase', 'Out loud, on-device speech'],
          ['secret', 'Secret phrase', 'Hidden; you have to remember it'],
        ] as const
      ).map(([value, title, subtitle], i, all) => (
        <RowItem
          key={String(value)}
          position={positionOf(i, all.length)}
          title={title}
          subtitle={subtitle}
          highlighted={challenge === value}
          onPress={() => setChallenge(value)}
          trailing={<RadioButton selected={challenge === value} onClick={() => setChallenge(value)} />}
        />
      ))}

      {effectiveChallenge !== 'off' ? (
        <Column modifiers={[fillMaxWidth(), padding(16, 12, 16, 0)]}>
          <OutlinedTextField
            value={phraseState}
            onValueChange={setPhrase}
            keyboardOptions={{ capitalization: 'sentences', imeAction: 'done' }}
            textStyle={{ fontFamily: fonts.text, fontSize: 16 }}
          shape={fieldShape()}
            modifiers={[fillMaxWidth()]}>
            <OutlinedTextField.Label>
              <Txt variant="bodyLarge">Phrase for this alarm (optional)</Txt>
            </OutlinedTextField.Label>
            <OutlinedTextField.SupportingText>
              <Txt variant="bodySmall">Leave empty to use one of your saved phrases, a different one each day.</Txt>
            </OutlinedTextField.SupportingText>
          </OutlinedTextField>
        </Column>
      ) : null}

      <Column modifiers={[fillMaxWidth(), padding(16, 24, 16, 0)]} verticalArrangement={{ spacedBy: 8 }}>
        <OutlinedButton
          enabled={canSave}
          modifiers={[fillMaxWidth()]}
          onClick={async () => {
            // The ringing screen needs notifications on Android 13+.
            if (!data.permissions.notifications) await ChronosAlarms.requestNotifications();
            const saved = save();
            ChronosAlarms.testAlarm(saved.id, 5);
          }}>
          <Txt variant="labelLarge">Save and test: ring in 5 seconds</Txt>
        </OutlinedButton>
        {alarm?.kind === 'repeat' && alarm.enabled ? (
          <FilledTonalButton modifiers={[fillMaxWidth()]} onClick={() => setConfirm('turnOff')}>
            <Txt variant="labelLarge">Turn off this alarm</Txt>
          </FilledTonalButton>
        ) : null}
        {alarm ? (
          <TextButton modifiers={[fillMaxWidth()]} colors={{ contentColor: colors.error }} onClick={() => setConfirm('delete')}>
            <Txt variant="labelLarge">Delete alarm</Txt>
          </TextButton>
        ) : null}
      </Column>

      {alarm && confirm === 'turnOff' ? (
        <ConfirmDialog
          icon={icons.alarm}
          title="Turn off this alarm?"
          message={`${describeSchedule(alarm)} at ${formatTime(alarm.hour, alarm.minute)} won't ring until you switch it back on. Its days and settings are kept.`}
          confirmLabel="Turn off"
          onConfirm={() => {
            ChronosAlarms.setAlarmEnabled(alarm.id, false);
            data.refresh();
            close();
          }}
          onDismiss={() => setConfirm(null)}
        />
      ) : null}
      {alarm && confirm === 'delete' ? (
        <ConfirmDialog
          icon={icons.delete}
          title="Delete this alarm?"
          message={`${formatTime(alarm.hour, alarm.minute)}${alarm.label ? ` · ${alarm.label}` : ''} will be removed and won't ring again. You can't undo this.`}
          confirmLabel="Delete"
          destructive
          onConfirm={() => {
            ChronosAlarms.deleteAlarm(alarm.id);
            data.refresh();
            close();
          }}
          onDismiss={() => setConfirm(null)}
        />
      ) : null}

      {showDatePicker ? (
        <DatePickerDialog
          initialDate={date ? `${date}T00:00:00.000Z` : new Date().toISOString()}
          selectableDates={{ start: startOfToday() }}
          confirmButtonLabel="OK"
          dismissButtonLabel="Cancel"
          onDateSelected={(d) => {
            // The date picker reports UTC midnight of the chosen day.
            setDate(`${d.getUTCFullYear()}-${String(d.getUTCMonth() + 1).padStart(2, '0')}-${String(d.getUTCDate()).padStart(2, '0')}`);
            setShowDatePicker(false);
          }}
          onDismissRequest={() => setShowDatePicker(false)}
        />
      ) : null}
    </Screen>
  );
}

function startOfToday() {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  return d;
}
