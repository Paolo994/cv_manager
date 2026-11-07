import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createJob, listJobs, startAnalyzeJob } from '../api/jobs';
import { ApiError } from '../api/client';
import { formatDate } from '../labels';

const MIN_LENGTH = 100;

export function JobsPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const jobs = useQuery({ queryKey: ['jobs'], queryFn: listJobs });
  const [description, setDescription] = useState('');
  const [url, setUrl] = useState('');

  const create = useMutation({
    mutationFn: async () => {
      const job = await createJob(description, url);
      const { runId } = await startAnalyzeJob(job.id);
      return { job, runId };
    },
    onSuccess: ({ job, runId }) => {
      void queryClient.invalidateQueries({ queryKey: ['jobs'] });
      navigate(`/jobs/${job.id}`, { state: { analyzeRunId: runId } });
    },
  });

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    create.mutate();
  }

  const tooShort = description.trim().length < MIN_LENGTH;

  return (
    <>
      <div className="page-head">
        <h1>Annunci</h1>
        <p className="muted">
          Incolla il testo di un annuncio, anche da LinkedIn: il Job Analyzer ne estrae i requisiti, poi puoi preparare lettera e CV
          adattati.
        </p>
      </div>

      <form className="panel" onSubmit={onSubmit}>
        <label>
          Testo dell'annuncio
          <textarea
            rows={8}
            maxLength={20000}
            required
            value={description}
            placeholder="Incolla qui titolo, descrizione, requisiti…"
            onChange={(e) => setDescription(e.target.value)}
          />
          <span className="hint">Almeno {MIN_LENGTH} caratteri. Il server non scarica pagine esterne: serve il testo.</span>
        </label>
        <label>
          Link all'annuncio (facoltativo)
          <input type="url" value={url} placeholder="https://www.linkedin.com/jobs/view/…" onChange={(e) => setUrl(e.target.value)} />
        </label>
        {create.error && <p className="error">{create.error instanceof ApiError ? create.error.message : 'Salvataggio non riuscito.'}</p>}
        <div className="actions">
          <button type="submit" disabled={tooShort || create.isPending}>
            {create.isPending ? 'Salvataggio…' : 'Aggiungi e analizza'}
          </button>
        </div>
      </form>

      <section>
        <h2>I tuoi annunci</h2>
        {jobs.data?.length === 0 && <p className="muted">Nessun annuncio ancora.</p>}
        <ul className="list">
          {jobs.data?.map((job) => (
            <li key={job.id}>
              <Link to={`/jobs/${job.id}`} className="list-item list-link">
                <div>
                  <strong>{job.analysis?.title || 'Annuncio in analisi'}</strong>
                  <div className="muted small-text">
                    {[job.analysis?.company, job.analysis?.location, formatDate(job.createdAt)].filter(Boolean).join(' · ')}
                  </div>
                </div>
                <span aria-hidden="true">→</span>
              </Link>
            </li>
          ))}
        </ul>
      </section>
    </>
  );
}
