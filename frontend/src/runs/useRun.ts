import { useEffect, useState } from 'react';
import type { RunView } from '../api/types';

/**
 * Segue un run degli agenti via Server-Sent Events.
 * Ogni evento "update" contiene lo stato completo del run, quindi basta sostituirlo.
 * La connessione si chiude quando il run termina (altrimenti EventSource si riconnetterebbe).
 */
export function useRun(runId: string | null): { run: RunView | null; connectionError: boolean } {
  const [run, setRun] = useState<RunView | null>(null);
  const [connectionError, setConnectionError] = useState(false);

  useEffect(() => {
    setRun(null);
    setConnectionError(false);
    if (!runId) return;

    const source = new EventSource(`/api/runs/${runId}/events`);
    source.addEventListener('update', (event) => {
      const view = JSON.parse((event as MessageEvent<string>).data) as RunView;
      setRun(view);
      setConnectionError(false);
      if (view.status !== 'RUNNING') source.close();
    });
    source.onerror = () => {
      // EventSource riprova da solo; segnaliamo solo se la connessione è chiusa definitivamente
      if (source.readyState === EventSource.CLOSED) setConnectionError(true);
    };
    return () => source.close();
  }, [runId]);

  return { run, connectionError };
}
