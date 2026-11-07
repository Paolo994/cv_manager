import { useCallback, useState, type DragEvent } from 'react';
import { Link } from 'react-router';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { cvFileUrl, deleteCv, fetchProfile, listCvs, startImportCv, uploadCv } from '../api/cv';
import { ApiError } from '../api/client';
import type { RunView } from '../api/types';
import { RunProgress } from '../runs/RunProgress';
import { formatDate, formatSize } from '../labels';

const ACCEPT = '.pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document';
const MAX_BYTES = 5 * 1024 * 1024;

export function CvPage() {
  const queryClient = useQueryClient();
  const cvs = useQuery({ queryKey: ['cvs'], queryFn: listCvs });
  const profile = useQuery({ queryKey: ['profile'], queryFn: fetchProfile });
  const [importRunId, setImportRunId] = useState<string | null>(null);
  const [importDone, setImportDone] = useState<RunView | null>(null);
  const [dragging, setDragging] = useState(false);
  const [localError, setLocalError] = useState<string | null>(null);

  /** Il nuovo profilo sostituisce quello attuale: chiede conferma se ne esiste già uno confermato. */
  const confirmReplace = () =>
    profile.data?.status !== 'CONFIRMED' ||
    window.confirm('Il profilo confermato verrà sostituito da una nuova bozza estratta da questo CV. Continuare?');

  const importMutation = useMutation({
    mutationFn: startImportCv,
    onSuccess: ({ runId }) => {
      setImportDone(null);
      setImportRunId(runId);
    },
  });

  const uploadMutation = useMutation({
    mutationFn: uploadCv,
    onSuccess: async (cv) => {
      await queryClient.invalidateQueries({ queryKey: ['cvs'] });
      if (confirmReplace()) importMutation.mutate(cv.id);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteCv,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['cvs'] }),
  });

  const onImportFinished = useCallback(
    (run: RunView) => {
      setImportDone(run);
      void queryClient.invalidateQueries({ queryKey: ['profile'] });
    },
    [queryClient],
  );

  function handleFile(file: File | undefined) {
    setLocalError(null);
    if (!file) return;
    if (file.size > MAX_BYTES) {
      setLocalError('Il file supera i 5 MB.');
      return;
    }
    uploadMutation.mutate(file);
  }

  function onDrop(e: DragEvent) {
    e.preventDefault();
    setDragging(false);
    handleFile(e.dataTransfer.files[0]);
  }

  const uploadError =
    localError ?? (uploadMutation.error instanceof ApiError ? uploadMutation.error.message : uploadMutation.error && 'Caricamento non riuscito.');

  return (
    <>
      <h1>I miei CV</h1>

      <section className="panel">
        <label
          className={`dropzone ${dragging ? 'dragging' : ''}`}
          onDragOver={(e) => {
            e.preventDefault();
            setDragging(true);
          }}
          onDragLeave={() => setDragging(false)}
          onDrop={onDrop}
        >
          <input
            type="file"
            accept={ACCEPT}
            className="visually-hidden"
            disabled={uploadMutation.isPending}
            onChange={(e) => {
              handleFile(e.target.files?.[0]);
              e.target.value = '';
            }}
          />
          <strong>{uploadMutation.isPending ? 'Caricamento…' : 'Trascina qui il CV o clicca per sceglierlo'}</strong>
          <span className="muted">PDF o DOCX, massimo 5 MB. Il CV Parser estrarrà il tuo profilo.</span>
        </label>
        {uploadError && <p className="error">{uploadError}</p>}
        {importMutation.error && <p className="error">{importMutation.error.message}</p>}

        {importRunId && (
          <div className="run-panel">
            <RunProgress runId={importRunId} onFinished={onImportFinished} />
            {importDone?.status === 'SUCCEEDED' && (
              <p>
                Profilo estratto. <Link to="/profile">Rivedilo e confermalo</Link>.
              </p>
            )}
          </div>
        )}
      </section>

      <section>
        <h2>CV caricati</h2>
        {cvs.isPending && <p className="muted">Caricamento…</p>}
        {cvs.data?.length === 0 && <p className="muted">Nessun CV caricato.</p>}
        <ul className="list">
          {cvs.data?.map((cv) => (
            <li key={cv.id} className="list-item">
              <div>
                <strong>{cv.filename}</strong>
                <div className="muted small-text">
                  {formatDate(cv.createdAt)} · {formatSize(cv.sizeBytes)}
                  {profile.data?.sourceCvId === cv.id && ' · fonte del profilo attuale'}
                </div>
              </div>
              <div className="actions">
                <Link className="button secondary small" to={`/cv/${cv.id}/review`}>
                  Valuta
                </Link>
                <button
                  className="secondary small"
                  disabled={importMutation.isPending}
                  onClick={() => confirmReplace() && importMutation.mutate(cv.id)}
                >
                  Estrai profilo
                </button>
                <a className="button secondary small" href={cvFileUrl(cv.id)}>
                  Scarica
                </a>
                <button
                  className="secondary small danger"
                  disabled={deleteMutation.isPending}
                  onClick={() => window.confirm(`Eliminare ${cv.filename} e le sue valutazioni?`) && deleteMutation.mutate(cv.id)}
                >
                  Elimina
                </button>
              </div>
            </li>
          ))}
        </ul>
      </section>
    </>
  );
}
