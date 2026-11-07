import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { login, register } from '../api/auth';
import { ApiError } from '../api/client';
import { CURRENT_USER_KEY } from '../auth/useCurrentUser';

const MIN_PASSWORD = 10;

export function RegisterPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const mutation = useMutation({
    mutationFn: async () => {
      await register({ fullName, email, password });
      await login(email, password);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: CURRENT_USER_KEY });
      navigate('/', { replace: true });
    },
  });

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    mutation.mutate();
  }

  let error: string | null = null;
  if (mutation.error instanceof ApiError && mutation.error.status === 409) {
    error = 'Esiste già un account con questa email.';
  } else if (mutation.error instanceof ApiError && mutation.error.status === 400) {
    error = 'Controlla i dati inseriti.';
  } else if (mutation.error) {
    error = 'Registrazione non riuscita. Riprova.';
  }

  return (
    <main className="auth-card">
      <h1>Crea un account</h1>
      <form onSubmit={onSubmit}>
        <label>
          Nome e cognome
          <input autoComplete="name" required maxLength={120} value={fullName} onChange={(e) => setFullName(e.target.value)} />
        </label>
        <label>
          Email
          <input type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        </label>
        <label>
          Password
          <input
            type="password"
            autoComplete="new-password"
            required
            minLength={MIN_PASSWORD}
            maxLength={72}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <span className="hint">Almeno {MIN_PASSWORD} caratteri.</span>
        </label>
        {error && <p className="error">{error}</p>}
        <button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? 'Creazione…' : 'Registrati'}
        </button>
      </form>
      <p className="muted">
        Hai già un account? <Link to="/login">Accedi</Link>
      </p>
    </main>
  );
}
