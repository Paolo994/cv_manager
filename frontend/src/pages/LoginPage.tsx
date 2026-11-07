import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { login } from '../api/auth';
import { ApiError } from '../api/client';
import { CURRENT_USER_KEY } from '../auth/useCurrentUser';

export function LoginPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const mutation = useMutation({
    mutationFn: () => login(email, password),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: CURRENT_USER_KEY });
      navigate('/', { replace: true });
    },
  });

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    mutation.mutate();
  }

  const error =
    mutation.error instanceof ApiError && mutation.error.status === 401
      ? 'Email o password non corretti.'
      : mutation.error
        ? 'Accesso non riuscito. Riprova.'
        : null;

  return (
    <main className="auth-card">
      <h1>Accedi</h1>
      <form onSubmit={onSubmit}>
        <label>
          Email
          <input type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        </label>
        <label>
          Password
          <input
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>
        {error && <p className="error">{error}</p>}
        <button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? 'Accesso…' : 'Accedi'}
        </button>
      </form>
      <p className="muted">
        Non hai un account? <Link to="/register">Registrati</Link>
      </p>
    </main>
  );
}
