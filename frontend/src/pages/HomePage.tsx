import { useNavigate } from 'react-router';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { logout } from '../api/auth';
import { CURRENT_USER_KEY, useCurrentUser } from '../auth/useCurrentUser';

const UPCOMING = [
  { title: 'I miei CV', text: 'Carica un CV e lascia che il CV Parser estragga il tuo profilo.' },
  { title: 'Valutazione CV', text: 'Punteggio per criterio e suggerimenti puntuali.' },
  { title: 'Offerte di lavoro', text: 'Offerte coerenti con profilo e preferenze, con punteggio.' },
  { title: 'Lettere e candidature', text: 'Lettera e CV adattati per ogni offerta, e tracker delle candidature.' },
];

export function HomePage() {
  const { data: user } = useCurrentUser();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const logoutMutation = useMutation({
    mutationFn: logout,
    onSettled: () => {
      queryClient.setQueryData(CURRENT_USER_KEY, null);
      navigate('/login', { replace: true });
    },
  });

  return (
    <div className="layout">
      <header className="topbar">
        <strong>CV Manager</strong>
        <div className="topbar-user">
          <span className="muted">{user?.email}</span>
          <button className="secondary" onClick={() => logoutMutation.mutate()} disabled={logoutMutation.isPending}>
            Esci
          </button>
        </div>
      </header>
      <main className="content">
        <h1>Ciao {user?.fullName}</h1>
        <p className="muted">Queste sezioni arrivano nelle prossime fasi.</p>
        <section className="grid">
          {UPCOMING.map((item) => (
            <article key={item.title} className="card">
              <h2>{item.title}</h2>
              <p className="muted">{item.text}</p>
            </article>
          ))}
        </section>
      </main>
    </div>
  );
}
