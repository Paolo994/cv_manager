import { useCallback, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { deleteJob, getJob, listDocuments, startAnalyzeJob, startPrepareApplication } from '../api/jobs';
import { fetchProfile } from '../api/cv';
import type { DocumentType, GeneratedDocument, JobAnalysis, Language, RunView, Tone } from '../api/types';
import { RunProgress } from '../runs/RunProgress';
import { LetterPanel } from '../components/LetterPanel';
import { TailoredCvPanel } from '../components/TailoredCvPanel';

const WORK_MODE: Record<JobAnalysis['workMode'], string> = {
  REMOTE: 'Da remoto',
  HYBRID: 'Ibrido',
  ON_SITE: 'In sede',
  UNSPECIFIED: '',
};

const TONES: Record<Tone, string> = { PROFESSIONAL: 'Professionale', FORMAL: 'Formale', DIRECT: 'Diretto' };

/** Ultima versione di ciascun tipo di documento (la lista arriva già ordinata per versione decrescente). */
function latestByType(documents: GeneratedDocument[] | undefined) {
  const latest: Partial<Record<DocumentType, GeneratedDocument>> = {};
  for (const d of documents ?? []) latest[d.type] ??= d;
  return latest;
}

export function JobDetailPage() {
  const { id: jobId = '' } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const job = useQuery({ queryKey: ['job', jobId], queryFn: () => getJob(jobId) });
  const documents = useQuery({ queryKey: ['documents', jobId], queryFn: () => listDocuments(jobId) });
  const profile = useQuery({ queryKey: ['profile'], queryFn: fetchProfile });

  const [analyzeRunId, setAnalyzeRunId] = useState<string | null>(
    (location.state as { analyzeRunId?: string } | null)?.analyzeRunId ?? null,
  );
  const [prepareRunId, setPrepareRunId] = useState<string | null>(null);
  const [preparing, setPreparing] = useState(false);
  const [types, setTypes] = useState<DocumentType[]>(['COVER_LETTER', 'TAILORED_CV']);
  const [language, setLanguage] = useState<Language | ''>('');
  const [tone, setTone] = useState<Tone>('PROFESSIONAL');
  const [notes, setNotes] = useState('');
  const [tab, setTab] = useState<DocumentType>('COVER_LETTER');

  const analyze = useMutation({ mutationFn: () => startAnalyzeJob(jobId), onSuccess: ({ runId }) => setAnalyzeRunId(runId) });
  const prepare = useMutation({
    mutationFn: () => startPrepareApplication({ jobId, documents: types, language: language || null, tone, notes }),
    onSuccess: ({ runId }) => {
      setPrepareRunId(runId);
      setPreparing(true);
    },
  });
  const remove = useMutation({
    mutationFn: () => deleteJob(jobId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['jobs'] });
      navigate('/jobs', { replace: true });
    },
  });

  const onAnalyzed = useCallback(() => void queryClient.invalidateQueries({ queryKey: ['job', jobId] }), [queryClient, jobId]);
  const onPrepared = useCallback(
    (run: RunView) => {
      setPreparing(false);
      void queryClient.invalidateQueries({ queryKey: ['job', jobId] });
      void queryClient.invalidateQueries({ queryKey: ['documents', jobId] });
      if (run.status === 'SUCCEEDED' && !types.includes(tab)) setTab(types[0]);
    },
    [queryClient, jobId, types, tab],
  );

  if (job.isError) {
    return (
      <p className="error">
        Annuncio non trovato. <Link to="/jobs">Torna agli annunci</Link>
      </p>
    );
  }
  if (!job.data) return <p className="muted">Caricamento…</p>;

  const analysis = job.data.analysis;
  const profileConfirmed = profile.data?.status === 'CONFIRMED';
  const skills = new Set((profile.data?.profile.technicalSkills ?? []).map((s) => s.toLowerCase()));
  const covered = (requirement: string) => [...skills].some((s) => requirement.toLowerCase().includes(s));
  const latest = latestByType(documents.data);
  const shownTab = latest[tab] ? tab : (Object.keys(latest)[0] as DocumentType | undefined);

  return (
    <>
      <p className="muted">
        <Link to="/jobs">← Annunci</Link>
      </p>
      <div className="page-head">
        <h1>{analysis?.title || 'Annuncio'}</h1>
        {analysis && (
          <p className="muted">
            {[analysis.company, analysis.location, WORK_MODE[analysis.workMode], analysis.salary].filter(Boolean).join(' · ')}
            {job.data.url && (
              <>
                {' · '}
                <a href={job.data.url} target="_blank" rel="noopener noreferrer">
                  annuncio originale
                </a>
              </>
            )}
          </p>
        )}
      </div>

      {analyzeRunId && !analysis && (
        <div className="run-panel">
          <RunProgress runId={analyzeRunId} onFinished={onAnalyzed} />
        </div>
      )}

      {analysis ? (
        <section className="panel">
          <p>{analysis.summary}</p>
          <div className="requirements">
            <div>
              <h2>Requisiti</h2>
              <ul className="req-list">
                {analysis.mustHave.map((r) => (
                  <li key={r} className={covered(r) ? 'covered' : ''}>
                    {r}
                  </li>
                ))}
              </ul>
            </div>
            {analysis.niceToHave.length > 0 && (
              <div>
                <h2>Graditi</h2>
                <ul className="req-list">
                  {analysis.niceToHave.map((r) => (
                    <li key={r} className={covered(r) ? 'covered' : ''}>
                      {r}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
          <p className="hint">In verde i requisiti che citano una competenza del tuo profilo (controllo indicativo).</p>
          {analysis.keywords.length > 0 && (
            <div className="chips">
              {analysis.keywords.map((k) => (
                <span key={k} className="chip">
                  {k}
                </span>
              ))}
            </div>
          )}
          <details>
            <summary>Testo dell'annuncio</summary>
            <p className="pre-wrap muted">{job.data.description}</p>
          </details>
        </section>
      ) : (
        !analyzeRunId && (
          <section className="panel">
            <p>Questo annuncio non è ancora stato analizzato.</p>
            <button className="secondary" onClick={() => analyze.mutate()} disabled={analyze.isPending}>
              Analizza annuncio
            </button>
          </section>
        )
      )}

      <section className="panel">
        <h2>Prepara la candidatura</h2>
        {!profileConfirmed ? (
          <p className="warning-box">
            Prima <Link to="/profile">conferma il tuo profilo</Link>: CV Tailor, Cover Letter Writer e Critic usano solo dati confermati.
          </p>
        ) : (
          <form
            onSubmit={(e) => {
              e.preventDefault();
              prepare.mutate();
            }}
          >
            <div className="form-grid">
              <fieldset className="choice-group">
                <legend>Documenti</legend>
                {(
                  [
                    ['COVER_LETTER', 'Lettera di presentazione'],
                    ['TAILORED_CV', 'CV adattato'],
                  ] as const
                ).map(([value, label]) => (
                  <label key={value} className="checkbox">
                    <input
                      type="checkbox"
                      checked={types.includes(value)}
                      onChange={(e) => setTypes(e.target.checked ? [...types, value] : types.filter((t) => t !== value))}
                    />
                    {label}
                  </label>
                ))}
              </fieldset>
              <div className="form-grid">
                <label>
                  Lingua
                  <select value={language} onChange={(e) => setLanguage(e.target.value as Language | '')}>
                    <option value="">Come l'annuncio</option>
                    <option value="IT">Italiano</option>
                    <option value="EN">Inglese</option>
                  </select>
                </label>
                <label>
                  Tono della lettera
                  <select value={tone} onChange={(e) => setTone(e.target.value as Tone)}>
                    {Object.entries(TONES).map(([value, label]) => (
                      <option key={value} value={value}>
                        {label}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
              <label className="span-2">
                Cosa vuoi sottolineare? (facoltativo)
                <textarea
                  rows={2}
                  maxLength={1000}
                  value={notes}
                  placeholder="Es. il progetto di migrazione del database, la disponibilità a trasferirmi…"
                  onChange={(e) => setNotes(e.target.value)}
                />
              </label>
            </div>
            <div className="actions">
              <button type="submit" disabled={types.length === 0 || prepare.isPending || preparing}>
                {latest.COVER_LETTER || latest.TAILORED_CV ? 'Rigenera documenti' : 'Prepara documenti'}
              </button>
              <span className="hint">Circa 1-2 minuti: gli agenti lavorano in parallelo e il Critic verifica tutto.</span>
            </div>
            {prepare.error && <p className="error">{prepare.error.message}</p>}
          </form>
        )}
        {prepareRunId && (
          <div className="run-panel">
            <RunProgress runId={prepareRunId} onFinished={onPrepared} />
          </div>
        )}
      </section>

      {shownTab && (
        <section>
          <div className="tabs" role="tablist">
            {latest.COVER_LETTER && (
              <button role="tab" aria-selected={shownTab === 'COVER_LETTER'} className={shownTab === 'COVER_LETTER' ? 'tab active' : 'tab'} onClick={() => setTab('COVER_LETTER')}>
                Lettera
              </button>
            )}
            {latest.TAILORED_CV && (
              <button role="tab" aria-selected={shownTab === 'TAILORED_CV'} className={shownTab === 'TAILORED_CV' ? 'tab active' : 'tab'} onClick={() => setTab('TAILORED_CV')}>
                CV adattato
              </button>
            )}
          </div>
          {shownTab === 'COVER_LETTER' && latest.COVER_LETTER?.type === 'COVER_LETTER' && (
            <LetterPanel key={latest.COVER_LETTER.id} document={latest.COVER_LETTER} />
          )}
          {shownTab === 'TAILORED_CV' && latest.TAILORED_CV?.type === 'TAILORED_CV' && <TailoredCvPanel document={latest.TAILORED_CV} />}
        </section>
      )}

      <div className="actions danger-zone">
        <button
          className="secondary small danger"
          onClick={() => window.confirm('Eliminare questo annuncio e i documenti preparati?') && remove.mutate()}
          disabled={remove.isPending}
        >
          Elimina annuncio
        </button>
      </div>
    </>
  );
}
