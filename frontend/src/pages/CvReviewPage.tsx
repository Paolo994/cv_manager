import { useCallback, useState } from 'react';
import { Link, useParams } from 'react-router';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { listCvs, listReviews, startReviewCv } from '../api/cv';
import type { RunView } from '../api/types';
import { RunProgress } from '../runs/RunProgress';
import { ReviewReport } from '../components/ReviewReport';
import { formatDate, scoreTone } from '../labels';

export function CvReviewPage() {
  const { id: cvId = '' } = useParams();
  const queryClient = useQueryClient();
  const cvs = useQuery({ queryKey: ['cvs'], queryFn: listCvs });
  const reviews = useQuery({ queryKey: ['reviews', cvId], queryFn: () => listReviews(cvId) });
  const [jobDescription, setJobDescription] = useState('');
  const [runId, setRunId] = useState<string | null>(null);
  const [runActive, setRunActive] = useState(false);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const cv = cvs.data?.find((c) => c.id === cvId);

  const start = useMutation({
    mutationFn: () => startReviewCv(cvId, jobDescription),
    onSuccess: ({ runId }) => {
      setRunId(runId);
      setRunActive(true);
    },
  });

  const onFinished = useCallback(
    (run: RunView) => {
      setRunActive(false);
      if (run.status === 'SUCCEEDED') {
        setSelectedId((run.output?.reviewId as string) ?? null);
        void queryClient.invalidateQueries({ queryKey: ['reviews', cvId] });
      }
    },
    [queryClient, cvId],
  );

  if (reviews.isError) {
    return (
      <p className="error">
        CV non trovato. <Link to="/cv">Torna ai CV</Link>
      </p>
    );
  }

  const selected = reviews.data?.find((r) => r.id === selectedId) ?? reviews.data?.[0];
  const running = start.isPending || runActive;

  return (
    <>
      <p className="muted">
        <Link to="/cv">← I miei CV</Link>
      </p>
      <h1>Valutazione CV{cv && <span className="muted"> · {cv.filename}</span>}</h1>

      <section className="panel">
        <label>
          Annuncio di lavoro (facoltativo)
          <textarea
            rows={5}
            maxLength={20000}
            placeholder="Incolla il testo di un annuncio per valutare anche l'aderenza del CV a quel ruolo."
            value={jobDescription}
            onChange={(e) => setJobDescription(e.target.value)}
          />
        </label>
        <div className="actions">
          <button onClick={() => start.mutate()} disabled={running}>
            {jobDescription.trim() ? 'Valuta rispetto all’annuncio' : 'Valuta il CV'}
          </button>
        </div>
        {start.error && <p className="error">{start.error.message}</p>}
        {runId && (
          <div className="run-panel">
            <RunProgress runId={runId} onFinished={onFinished} />
          </div>
        )}
      </section>

      {reviews.data && reviews.data.length > 0 && (
        <div className="review-layout">
          <aside className="review-history">
            <h2>Storico</h2>
            <ul className="list compact">
              {reviews.data.map((r) => (
                <li key={r.id}>
                  <button
                    className={`history-item ${r.id === selected?.id ? 'selected' : ''}`}
                    onClick={() => setSelectedId(r.id)}
                  >
                    <span className={`score-pill tone-${scoreTone(r.review.overallScore)}`}>{r.review.overallScore}</span>
                    <span>
                      {formatDate(r.createdAt)}
                      <span className="muted small-text block">{r.jobDescription ? 'con annuncio' : 'valutazione generale'}</span>
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          </aside>
          {selected && <ReviewReport review={selected} />}
        </div>
      )}
      {reviews.data?.length === 0 && !runId && <p className="muted">Nessuna valutazione ancora per questo CV.</p>}
    </>
  );
}
