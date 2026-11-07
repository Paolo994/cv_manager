import type { GeneratedDocument } from '../api/types';
import { CriticNotes, DocumentToolbar } from './DocumentMeta';

type CvDocument = Extract<GeneratedDocument, { type: 'TAILORED_CV' }>;

/** Anteprima del CV adattato, con le spiegazioni del CV Tailor su cosa è cambiato. */
export function TailoredCvPanel({ document }: { document: CvDocument }) {
  const cv = document.content;
  return (
    <article className="report">
      <DocumentToolbar document={document} />
      <CriticNotes issues={document.reviewNotes} />

      {cv.changes.length > 0 && (
        <div className="info-box">
          <strong>Cosa è stato adattato</strong>
          <ul className="bullets">
            {cv.changes.map((c, i) => (
              <li key={i}>{c}</li>
            ))}
          </ul>
        </div>
      )}

      <div className="cv-preview">
        <h2 className="cv-name">{cv.fullName}</h2>
        <p className="cv-headline">{cv.headline}</p>
        {cv.summary && <p>{cv.summary}</p>}

        {cv.experiences.length > 0 && <h3>Esperienza</h3>}
        {cv.experiences.map((e, i) => (
          <div key={i} className="cv-entry">
            <div className="cv-entry-head">
              <strong>{[e.role, e.company].filter(Boolean).join(' · ')}</strong>
              <span className="muted">{e.period}</span>
            </div>
            <ul className="bullets">
              {e.bullets.map((b, j) => (
                <li key={j}>{b}</li>
              ))}
            </ul>
          </div>
        ))}

        {cv.education.length > 0 && <h3>Formazione</h3>}
        {cv.education.map((e, i) => (
          <div key={i} className="cv-entry">
            <div className="cv-entry-head">
              <strong>{[e.degree, e.institution].filter(Boolean).join(' · ')}</strong>
              <span className="muted">{e.period}</span>
            </div>
            {e.details && <p className="muted">{e.details}</p>}
          </div>
        ))}

        {cv.skills.length > 0 && <h3>Competenze</h3>}
        {cv.skills.map((g, i) => (
          <p key={i}>
            <strong>{g.category}:</strong> {g.items.join(', ')}
          </p>
        ))}

        {cv.languages.length > 0 && (
          <>
            <h3>Lingue</h3>
            <p>{cv.languages.join(' · ')}</p>
          </>
        )}
        {cv.certifications.length > 0 && (
          <>
            <h3>Certificazioni</h3>
            <ul className="bullets">
              {cv.certifications.map((c, i) => (
                <li key={i}>{c}</li>
              ))}
            </ul>
          </>
        )}
      </div>
    </article>
  );
}
