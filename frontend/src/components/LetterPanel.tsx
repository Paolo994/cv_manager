import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { reviseLetter } from '../api/jobs';
import type { CoverLetter, GeneratedDocument } from '../api/types';
import { CriticNotes, DocumentToolbar } from './DocumentMeta';

type LetterDocument = Extract<GeneratedDocument, { type: 'COVER_LETTER' }>;

/** Anteprima della lettera, con modifica salvata come nuova versione. */
export function LetterPanel({ document }: { document: LetterDocument }) {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState(false);
  const letter = document.content;
  const [subject, setSubject] = useState(letter.subject);
  const [salutation, setSalutation] = useState(letter.salutation);
  const [body, setBody] = useState(letter.paragraphs.join('\n\n'));
  const [closing, setClosing] = useState(letter.closing);

  const save = useMutation({
    mutationFn: () => {
      const edited: CoverLetter = {
        subject: subject.trim(),
        salutation: salutation.trim(),
        paragraphs: body
          .split(/\n\s*\n/)
          .map((p) => p.trim())
          .filter(Boolean),
        closing: closing.trim(),
      };
      return reviseLetter(document.id, edited);
    },
    onSuccess: () => {
      setEditing(false);
      void queryClient.invalidateQueries({ queryKey: ['documents', document.jobId] });
    },
  });

  return (
    <article className="report">
      <DocumentToolbar document={document} />
      {document.origin === 'AI' && <CriticNotes issues={document.reviewNotes} />}

      {editing ? (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            save.mutate();
          }}
        >
          <label>
            Oggetto
            <input value={subject} maxLength={300} onChange={(e) => setSubject(e.target.value)} />
          </label>
          <label>
            Saluto
            <input value={salutation} maxLength={200} required onChange={(e) => setSalutation(e.target.value)} />
          </label>
          <label>
            Testo
            <textarea rows={14} value={body} required onChange={(e) => setBody(e.target.value)} />
            <span className="hint">Separa i paragrafi con una riga vuota.</span>
          </label>
          <label>
            Chiusura
            <input value={closing} maxLength={100} required onChange={(e) => setClosing(e.target.value)} />
            <span className="hint">La firma con il tuo nome viene aggiunta nel PDF e nel DOCX.</span>
          </label>
          {save.error && <p className="error">Salvataggio non riuscito: controlla che nessun campo sia vuoto.</p>}
          <div className="actions">
            <button type="submit" disabled={save.isPending}>
              Salva come nuova versione
            </button>
            <button type="button" className="secondary" onClick={() => setEditing(false)}>
              Annulla
            </button>
          </div>
        </form>
      ) : (
        <>
          <div className="letter">
            {letter.subject && <p className="letter-subject">{letter.subject}</p>}
            <p>{letter.salutation}</p>
            {letter.paragraphs.map((p, i) => (
              <p key={i}>{p}</p>
            ))}
            <p>{letter.closing}</p>
          </div>
          <div className="actions">
            <button className="secondary" onClick={() => setEditing(true)}>
              Modifica
            </button>
          </div>
        </>
      )}
    </article>
  );
}
