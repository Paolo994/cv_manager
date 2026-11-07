import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { fetchPreferences, savePreferences } from '../api/cv';
import type { ApplicationLanguage, JobPreferences, Seniority, WorkMode } from '../api/types';
import { LinesField } from '../components/fields';
import { SENIORITY_LABELS, WORK_MODE_LABELS } from '../labels';

const LANGUAGE_LABELS: Record<ApplicationLanguage, string> = { IT: 'Italiano', EN: 'Inglese' };

function toggle<T>(list: T[], item: T, on: boolean): T[] {
  return on ? [...list.filter((x) => x !== item), item] : list.filter((x) => x !== item);
}

export function PreferencesPage() {
  const queryClient = useQueryClient();
  const query = useQuery({ queryKey: ['preferences'], queryFn: fetchPreferences });
  const [prefs, setPrefs] = useState<JobPreferences | null>(null);
  const [loadedAt, setLoadedAt] = useState(0);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    if (query.data) {
      setPrefs(query.data);
      setLoadedAt(query.dataUpdatedAt);
    }
  }, [query.data, query.dataUpdatedAt]);

  const save = useMutation({
    mutationFn: savePreferences,
    onSuccess: (data) => {
      queryClient.setQueryData(['preferences'], data);
      setSaved(true);
    },
  });

  if (query.isPending || !prefs) return <p className="muted">Caricamento…</p>;

  const update = (patch: Partial<JobPreferences>) => {
    setSaved(false);
    setPrefs({ ...prefs, ...patch });
  };

  return (
    <>
      <div className="page-head">
        <h1>Preferenze di ricerca</h1>
        <p className="muted">Servono agli agenti per cercare e filtrare offerte coerenti con quello che vuoi.</p>
      </div>
      <form
        key={loadedAt}
        className="panel"
        onSubmit={(e) => {
          e.preventDefault();
          save.mutate(prefs);
        }}
      >
        <div className="form-grid">
          <LinesField label="Ruoli cercati" value={prefs.targetRoles} hint="Uno per riga, es. Backend Developer" rows={3} onChange={(targetRoles) => update({ targetRoles })} />
          <LinesField label="Città" value={prefs.locations} hint="Una per riga" rows={3} onChange={(locations) => update({ locations })} />

          <fieldset className="choice-group">
            <legend>Modalità di lavoro</legend>
            {(Object.keys(WORK_MODE_LABELS) as WorkMode[]).map((mode) => (
              <label key={mode} className="checkbox">
                <input type="checkbox" checked={prefs.workModes.includes(mode)} onChange={(e) => update({ workModes: toggle(prefs.workModes, mode, e.target.checked) })} />
                {WORK_MODE_LABELS[mode]}
              </label>
            ))}
          </fieldset>

          <fieldset className="choice-group">
            <legend>Lingue delle candidature</legend>
            {(Object.keys(LANGUAGE_LABELS) as ApplicationLanguage[]).map((lang) => (
              <label key={lang} className="checkbox">
                <input
                  type="checkbox"
                  checked={prefs.applicationLanguages.includes(lang)}
                  onChange={(e) => update({ applicationLanguages: toggle(prefs.applicationLanguages, lang, e.target.checked) })}
                />
                {LANGUAGE_LABELS[lang]}
              </label>
            ))}
          </fieldset>

          <label>
            RAL minima (€ lordi annui)
            <input
              type="number"
              min={0}
              step={1000}
              value={prefs.minAnnualSalaryEur ?? ''}
              placeholder="Nessun vincolo"
              onChange={(e) => update({ minAnnualSalaryEur: e.target.value === '' ? null : Number(e.target.value) })}
            />
          </label>
          <label>
            Seniority cercata
            <select value={prefs.seniority ?? ''} onChange={(e) => update({ seniority: (e.target.value || null) as Seniority | null })}>
              <option value="">Come nel profilo</option>
              {Object.entries(SENIORITY_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>

          <LinesField label="Settori da escludere" value={prefs.excludedIndustries} hint="Uno per riga" rows={3} onChange={(excludedIndustries) => update({ excludedIndustries })} />
          <label className="checkbox">
            <input type="checkbox" checked={prefs.willingToRelocate} onChange={(e) => update({ willingToRelocate: e.target.checked })} />
            Disponibile a trasferirmi
          </label>
        </div>

        <div className="actions">
          <button type="submit" disabled={save.isPending}>
            Salva preferenze
          </button>
          {saved && <span className="success">Preferenze salvate.</span>}
          {save.error && <span className="error">Salvataggio non riuscito: controlla i valori.</span>}
        </div>
      </form>
    </>
  );
}
