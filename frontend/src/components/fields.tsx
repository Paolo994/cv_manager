import { useState } from 'react';

/**
 * Campi per liste di stringhe. Il testo resta locale mentre si scrive (così una virgola o un a capo
 * finale non spariscono); il genitore riceve la lista già pulita. Per ricaricare valori esterni,
 * il genitore cambia la `key` del componente.
 */

type ListFieldProps = {
  label: string;
  value: string[];
  onChange: (value: string[]) => void;
  hint?: string;
  rows?: number;
};

/** Un elemento per riga. */
export function LinesField({ label, value, onChange, hint, rows = 4 }: ListFieldProps) {
  const [text, setText] = useState(value.join('\n'));
  return (
    <label>
      {label}
      <textarea
        rows={rows}
        value={text}
        onChange={(e) => {
          setText(e.target.value);
          onChange(splitClean(e.target.value, /\n/));
        }}
      />
      <span className="hint">{hint ?? 'Uno per riga.'}</span>
    </label>
  );
}

/** Elementi separati da virgola. */
export function CommaField({ label, value, onChange, hint }: ListFieldProps) {
  const [text, setText] = useState(value.join(', '));
  return (
    <label>
      {label}
      <input
        value={text}
        onChange={(e) => {
          setText(e.target.value);
          onChange(splitClean(e.target.value, /,/));
        }}
      />
      <span className="hint">{hint ?? 'Separati da virgola.'}</span>
    </label>
  );
}

function splitClean(text: string, separator: RegExp): string[] {
  return text
    .split(separator)
    .map((s) => s.trim())
    .filter(Boolean);
}
