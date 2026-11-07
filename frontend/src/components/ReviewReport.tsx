import type { ReviewView } from '../api/types';
import { CRITERION_LABELS, PRIORITY_LABELS, scoreTone } from '../labels';

export function ReviewReport({ review: view }: { review: ReviewView }) {
  const { review } = view;
  return (
    <article className="report">
      <header className="report-header">
        <div className={`score-big tone-${scoreTone(review.overallScore)}`}>
          <span className="score-value">{review.overallScore}</span>
          <span className="score-scale">/100</span>
        </div>
        <p>{review.summary}</p>
      </header>

      <section>
        <h2>Criteri</h2>
        <ul className="criteria">
          {review.criteria.map((c) => (
            <li key={c.criterion}>
              <div className="criterion-head">
                <strong>{CRITERION_LABELS[c.criterion] ?? c.criterion}</strong>
                <span>{c.score}</span>
              </div>
              <div
                className="bar"
                role="meter"
                aria-valuemin={0}
                aria-valuemax={100}
                aria-valuenow={c.score}
                aria-label={CRITERION_LABELS[c.criterion]}
              >
                <div className={`bar-fill tone-${scoreTone(c.score)}`} style={{ width: `${c.score}%` }} />
              </div>
              <p className="muted">{c.comment}</p>
            </li>
          ))}
        </ul>
      </section>

      {review.strengths.length > 0 && (
        <section>
          <h2>Punti di forza</h2>
          <ul className="bullets">
            {review.strengths.map((s, i) => (
              <li key={i}>{s}</li>
            ))}
          </ul>
        </section>
      )}

      <section>
        <h2>Cosa migliorare</h2>
        <ol className="suggestions">
          {review.suggestions.map((s, i) => (
            <li key={i} className="suggestion">
              <div className="suggestion-head">
                <span className={`badge badge-${s.priority.toLowerCase()}`}>{PRIORITY_LABELS[s.priority]}</span>
                <span className="muted">{s.section}</span>
              </div>
              <p>
                <strong>{s.issue}</strong>
              </p>
              <p>{s.recommendation}</p>
              {s.example && <blockquote>{s.example}</blockquote>}
            </li>
          ))}
        </ol>
      </section>

      {view.jobDescription && (
        <details>
          <summary>Annuncio usato per la valutazione</summary>
          <p className="pre-wrap muted">{view.jobDescription}</p>
        </details>
      )}
    </article>
  );
}
