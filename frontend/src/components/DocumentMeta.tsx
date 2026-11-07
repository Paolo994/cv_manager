import type { CriticIssue, GeneratedDocument } from '../api/types';
import { exportUrl } from '../api/jobs';
import { formatDate } from '../labels';

const ISSUE_LABELS: Record<CriticIssue['type'], string> = {
  UNSUPPORTED_CLAIM: 'Non supportato dal profilo',
  INACCURATE: 'Impreciso',
  STYLE: 'Stile',
};

/** Versione, origine e download PDF/DOCX di un documento. */
export function DocumentToolbar({ document }: { document: GeneratedDocument }) {
  return (
    <div className="doc-toolbar">
      <span className="muted small-text">
        Versione {document.version} · {document.origin === 'USER' ? 'modificata da te' : 'generata dagli agenti'} ·{' '}
        {formatDate(document.createdAt)} · {document.language === 'IT' ? 'italiano' : 'inglese'}
      </span>
      <div className="actions">
        <a className="button secondary small" href={exportUrl(document.id, 'pdf')}>
          Scarica PDF
        </a>
        <a className="button secondary small" href={exportUrl(document.id, 'docx')}>
          Scarica DOCX
        </a>
      </div>
    </div>
  );
}

/** Segnalazioni del Critic rimaste aperte dopo le revisioni: l'utente le deve vedere prima di inviare. */
export function CriticNotes({ issues }: { issues: CriticIssue[] }) {
  if (issues.length === 0) {
    return <p className="success-box">Il Critic ha verificato il documento: nessuna affermazione fuori dal tuo profilo.</p>;
  }
  const blocking = issues.some((i) => i.type !== 'STYLE');
  return (
    <div className={blocking ? 'warning-box' : 'info-box'}>
      <strong>{blocking ? 'Da controllare prima di inviare' : 'Suggerimenti di stile'}</strong>
      <ul className="bullets">
        {issues.map((issue, i) => (
          <li key={i}>
            <span className="badge badge-low">{ISSUE_LABELS[issue.type]}</span> «{issue.excerpt}» — {issue.problem}{' '}
            <span className="muted">{issue.suggestion}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
