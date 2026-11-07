import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { fetchProfile, saveProfile } from '../api/cv';
import { ApiError } from '../api/client';
import type { CandidateProfile, Education, Experience, Seniority } from '../api/types';
import { CommaField, LinesField } from '../components/fields';
import { ContactPanel } from '../components/ContactPanel';
import { formatDate, SENIORITY_LABELS } from '../labels';

const EMPTY_EXPERIENCE: Experience = {
  role: '', company: '', location: '', startDate: '', endDate: '', current: false,
  description: '', achievements: [], technologies: [],
};
const EMPTY_EDUCATION: Education = { degree: '', field: '', institution: '', startDate: '', endDate: '', grade: '' };

export function ProfilePage() {
  const queryClient = useQueryClient();
  const query = useQuery({ queryKey: ['profile'], queryFn: fetchProfile });
  const [draft, setDraft] = useState<CandidateProfile | null>(null);
  // cambia quando arrivano dati dal server: rimonta i campi lista con i nuovi valori
  const [formKey, setFormKey] = useState('');
  const [saved, setSaved] = useState<string | null>(null);

  useEffect(() => {
    if (query.data) {
      setDraft(query.data.profile);
      setFormKey(query.data.updatedAt);
    }
  }, [query.data]);

  const save = useMutation({
    mutationFn: ({ profile, confirm }: { profile: CandidateProfile; confirm: boolean }) => saveProfile(profile, confirm),
    onSuccess: (view, { confirm }) => {
      queryClient.setQueryData(['profile'], view);
      setSaved(confirm ? 'Profilo confermato.' : 'Bozza salvata.');
    },
  });

  if (query.isPending) return <p className="muted">Caricamento…</p>;
  if (query.isError) return <p className="error">Impossibile caricare il profilo.</p>;
  if (!query.data || !draft) {
    return (
      <>
        <h1>Profilo</h1>
        <div className="panel">
          <p>Non hai ancora un profilo.</p>
          <p className="muted">
            <Link to="/cv">Carica un CV</Link>: il CV Parser estrarrà esperienze, competenze e formazione.
          </p>
        </div>
      </>
    );
  }

  const view = query.data;
  const update = (patch: Partial<CandidateProfile>) => {
    setSaved(null);
    setDraft({ ...draft, ...patch });
  };
  const updateExperience = (index: number, patch: Partial<Experience>) =>
    update({ experiences: draft.experiences.map((e, i) => (i === index ? { ...e, ...patch } : e)) });
  const updateEducation = (index: number, patch: Partial<Education>) =>
    update({ education: draft.education.map((e, i) => (i === index ? { ...e, ...patch } : e)) });

  const error =
    save.error instanceof ApiError && save.error.status === 400
      ? 'Alcuni campi non sono validi o sono troppo lunghi.'
      : save.error && 'Salvataggio non riuscito.';

  return (
    <>
      <div className="page-head">
        <h1>
          Profilo{' '}
          <span className={`badge ${view.status === 'CONFIRMED' ? 'badge-good' : 'badge-warning'}`}>
            {view.status === 'CONFIRMED' ? 'Confermato' : 'Bozza'}
          </span>
        </h1>
        <p className="muted">
          {view.status === 'CONFIRMED'
            ? `Confermato il ${formatDate(view.confirmedAt!)}. Gli agenti useranno questi dati.`
            : 'Estratto dal CV dal CV Parser: controlla i dati, correggi se serve e conferma.'}
        </p>
      </div>

      <form
        key={formKey}
        className="profile-form"
        onSubmit={(e) => {
          e.preventDefault();
          save.mutate({ profile: draft, confirm: true });
        }}
      >
        <section className="panel">
          <h2>Dati principali</h2>
          <div className="form-grid">
            <label>
              Nome e cognome
              <input value={draft.fullName} maxLength={120} onChange={(e) => update({ fullName: e.target.value })} />
            </label>
            <label>
              Località
              <input value={draft.location} maxLength={120} onChange={(e) => update({ location: e.target.value })} />
            </label>
            <label className="span-2">
              Titolo professionale
              <input value={draft.headline} maxLength={200} onChange={(e) => update({ headline: e.target.value })} />
            </label>
            <label>
              Seniority
              <select value={draft.seniority} onChange={(e) => update({ seniority: e.target.value as Seniority })}>
                {Object.entries(SENIORITY_LABELS).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Anni di esperienza
              <input
                type="number"
                min={0}
                max={60}
                value={draft.yearsOfExperience}
                onChange={(e) => update({ yearsOfExperience: Number(e.target.value) || 0 })}
              />
              <span className="hint">Calcolati dalle date delle esperienze.</span>
            </label>
            <label className="span-2">
              Sintesi
              <textarea rows={4} maxLength={2000} value={draft.summary} onChange={(e) => update({ summary: e.target.value })} />
            </label>
          </div>
        </section>

        <section className="panel">
          <div className="section-head">
            <h2>Esperienze</h2>
            <button type="button" className="secondary small" onClick={() => update({ experiences: [{ ...EMPTY_EXPERIENCE }, ...draft.experiences] })}>
              Aggiungi
            </button>
          </div>
          {draft.experiences.map((exp, i) => (
            <fieldset key={`${formKey}-exp-${i}-${draft.experiences.length}`} className="item">
              <div className="form-grid">
                <label>
                  Ruolo
                  <input value={exp.role} maxLength={150} onChange={(e) => updateExperience(i, { role: e.target.value })} />
                </label>
                <label>
                  Azienda
                  <input value={exp.company} maxLength={150} onChange={(e) => updateExperience(i, { company: e.target.value })} />
                </label>
                <label>
                  Inizio
                  <input value={exp.startDate} placeholder="AAAA-MM" maxLength={10} onChange={(e) => updateExperience(i, { startDate: e.target.value })} />
                </label>
                <label>
                  Fine
                  <input
                    value={exp.endDate}
                    placeholder="AAAA-MM"
                    maxLength={10}
                    disabled={exp.current}
                    onChange={(e) => updateExperience(i, { endDate: e.target.value })}
                  />
                </label>
                <label className="checkbox span-2">
                  <input type="checkbox" checked={exp.current} onChange={(e) => updateExperience(i, { current: e.target.checked, endDate: '' })} />
                  Ruolo attuale
                </label>
                <label className="span-2">
                  Descrizione
                  <textarea rows={3} maxLength={4000} value={exp.description} onChange={(e) => updateExperience(i, { description: e.target.value })} />
                </label>
                <div className="span-2">
                  <LinesField label="Risultati" value={exp.achievements} rows={3} onChange={(achievements) => updateExperience(i, { achievements })} />
                </div>
                <div className="span-2">
                  <CommaField label="Tecnologie" value={exp.technologies} onChange={(technologies) => updateExperience(i, { technologies })} />
                </div>
              </div>
              <button
                type="button"
                className="secondary small danger"
                onClick={() => update({ experiences: draft.experiences.filter((_, j) => j !== i) })}
              >
                Rimuovi esperienza
              </button>
            </fieldset>
          ))}
        </section>

        <section className="panel">
          <div className="section-head">
            <h2>Formazione</h2>
            <button type="button" className="secondary small" onClick={() => update({ education: [...draft.education, { ...EMPTY_EDUCATION }] })}>
              Aggiungi
            </button>
          </div>
          {draft.education.map((edu, i) => (
            <fieldset key={`${formKey}-edu-${i}-${draft.education.length}`} className="item">
              <div className="form-grid">
                <label>
                  Titolo
                  <input value={edu.degree} maxLength={200} onChange={(e) => updateEducation(i, { degree: e.target.value })} />
                </label>
                <label>
                  Indirizzo di studio
                  <input value={edu.field} maxLength={200} onChange={(e) => updateEducation(i, { field: e.target.value })} />
                </label>
                <label>
                  Istituto
                  <input value={edu.institution} maxLength={200} onChange={(e) => updateEducation(i, { institution: e.target.value })} />
                </label>
                <label>
                  Anno di fine
                  <input value={edu.endDate} placeholder="AAAA" maxLength={10} onChange={(e) => updateEducation(i, { endDate: e.target.value })} />
                </label>
                <label>
                  Voto
                  <input value={edu.grade} maxLength={50} onChange={(e) => updateEducation(i, { grade: e.target.value })} />
                </label>
              </div>
              <button type="button" className="secondary small danger" onClick={() => update({ education: draft.education.filter((_, j) => j !== i) })}>
                Rimuovi
              </button>
            </fieldset>
          ))}
        </section>

        <section className="panel">
          <h2>Competenze e lingue</h2>
          <CommaField label="Competenze tecniche" value={draft.technicalSkills} onChange={(technicalSkills) => update({ technicalSkills })} />
          <CommaField label="Soft skill" value={draft.softSkills} onChange={(softSkills) => update({ softSkills })} />
          <LinesField
            label="Lingue"
            value={draft.languages.map((l) => (l.level ? `${l.language}: ${l.level}` : l.language))}
            hint="Una per riga, es. Inglese: C1"
            rows={3}
            onChange={(lines) =>
              update({
                languages: lines.map((line) => {
                  const [language, ...level] = line.split(':');
                  return { language: language.trim(), level: level.join(':').trim() };
                }),
              })
            }
          />
          <LinesField label="Certificazioni" value={draft.certifications} rows={3} onChange={(certifications) => update({ certifications })} />
        </section>

        <div className="sticky-actions">
          {error && <p className="error">{error}</p>}
          {saved && <p className="success">{saved}</p>}
          <button type="button" className="secondary" disabled={save.isPending} onClick={() => save.mutate({ profile: draft, confirm: false })}>
            Salva bozza
          </button>
          <button type="submit" disabled={save.isPending}>
            {view.status === 'CONFIRMED' ? 'Salva e conferma' : 'Conferma profilo'}
          </button>
        </div>
      </form>

      <div className="spaced-top">
        <ContactPanel />
      </div>
    </>
  );
}
