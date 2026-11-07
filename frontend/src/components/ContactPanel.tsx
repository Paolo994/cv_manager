import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getContact, saveContact } from '../api/jobs';
import type { ContactInfo } from '../api/types';

const FIELDS: { key: keyof ContactInfo; label: string; type?: string; max: number }[] = [
  { key: 'email', label: 'Email', type: 'email', max: 254 },
  { key: 'phone', label: 'Telefono', type: 'tel', max: 40 },
  { key: 'city', label: 'Città', max: 120 },
  { key: 'linkedin', label: 'LinkedIn', max: 300 },
  { key: 'website', label: 'Sito o portfolio', max: 300 },
];

/** Contatti per l'intestazione di CV e lettere esportati. Restano sul server: non vengono inviati all'AI. */
export function ContactPanel() {
  const queryClient = useQueryClient();
  const query = useQuery({ queryKey: ['contact'], queryFn: getContact });
  const [contact, setContact] = useState<ContactInfo | null>(null);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    if (query.data) setContact(query.data);
  }, [query.data]);

  const save = useMutation({
    mutationFn: saveContact,
    onSuccess: (data) => {
      queryClient.setQueryData(['contact'], data);
      setSaved(true);
    },
  });

  if (!contact) return null;

  return (
    <form
      className="panel"
      onSubmit={(e) => {
        e.preventDefault();
        save.mutate(contact);
      }}
    >
      <h2>Contatti</h2>
      <p className="muted">
        Compaiono nell'intestazione di CV e lettere che scarichi. Non vengono mai inviati agli agenti AI.
      </p>
      <div className="form-grid">
        {FIELDS.map((f) => (
          <label key={f.key}>
            {f.label}
            <input
              type={f.type ?? 'text'}
              maxLength={f.max}
              value={contact[f.key] ?? ''}
              onChange={(e) => {
                setSaved(false);
                setContact({ ...contact, [f.key]: e.target.value });
              }}
            />
          </label>
        ))}
      </div>
      <div className="actions">
        <button type="submit" className="secondary" disabled={save.isPending}>
          Salva contatti
        </button>
        {saved && <span className="success">Contatti salvati.</span>}
        {save.error && <span className="error">Controlla l'email.</span>}
      </div>
    </form>
  );
}
