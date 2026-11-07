import { useEffect, useRef } from 'react';
import type { RunView } from '../api/types';
import { useRun } from './useRun';

const AGENT_LABELS: Record<string, string> = {
  'cv-parser': 'CV Parser · legge il CV ed estrae il profilo',
  'cv-reviewer': 'CV Reviewer · valuta il CV',
  'job-analyzer': "Job Analyzer · estrae requisiti e parole chiave dall'annuncio",
  'cv-tailor': "CV Tailor · adatta il CV all'annuncio",
  'cover-letter-writer': 'Cover Letter Writer · scrive la lettera',
  critic: 'Critic · verifica che non ci siano fatti inventati',
};

const STATUS_LABELS = { RUNNING: 'in corso', SUCCEEDED: 'completato', FAILED: 'errore' } as const;

type Props = {
  runId: string;
  /** Chiamata una sola volta quando il run termina. */
  onFinished?: (run: RunView) => void;
};

/** Avanzamento in tempo reale di un run: un riga per agente, con stato, modello e token. */
export function RunProgress({ runId, onFinished }: Props) {
  const { run, connectionError } = useRun(runId);
  const notified = useRef<string | null>(null);

  useEffect(() => {
    if (run && run.status !== 'RUNNING' && notified.current !== run.id) {
      notified.current = run.id;
      onFinished?.(run);
    }
  }, [run, onFinished]);

  if (connectionError) {
    return <p className="error">Connessione persa con il server. Ricarica la pagina per vedere lo stato.</p>;
  }
  if (!run) {
    return <p className="muted">Avvio degli agenti…</p>;
  }

  return (
    <div className={`run run-${run.status.toLowerCase()}`} aria-live="polite">
      <ol className="run-steps">
        {run.steps.map((step, i) => (
          <li key={i} className={`run-step step-${step.status.toLowerCase()}`}>
            <span className="step-dot" aria-hidden="true" />
            <span className="step-name">
              {AGENT_LABELS[step.agent] ?? step.agent}
              {step.attempt > 1 && step.agent !== 'critic' && <span className="badge badge-medium">revisione {step.attempt - 1}</span>}
              {step.attempt > 1 && step.agent === 'critic' && <span className="badge badge-low">nuova verifica</span>}
            </span>
            <span className="step-meta">
              {STATUS_LABELS[step.status]}
              {step.status === 'SUCCEEDED' && ` · ${step.inputTokens + step.outputTokens} token`}
            </span>
          </li>
        ))}
        {run.steps.length === 0 && run.status === 'RUNNING' && <li className="muted">In coda…</li>}
      </ol>
      {run.status === 'FAILED' && <p className="error">{run.error}</p>}
    </div>
  );
}
