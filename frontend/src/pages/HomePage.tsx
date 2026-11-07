import { Link } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { fetchProfile, listCvs } from '../api/cv';
import { useCurrentUser } from '../auth/useCurrentUser';

export function HomePage() {
  const { data: user } = useCurrentUser();
  const cvs = useQuery({ queryKey: ['cvs'], queryFn: listCvs });
  const profile = useQuery({ queryKey: ['profile'], queryFn: fetchProfile });

  const cvCount = cvs.data?.length ?? 0;
  const profileText = !profile.data
    ? 'Ancora nessun profilo: carica un CV.'
    : profile.data.status === 'CONFIRMED'
      ? 'Profilo confermato: gli agenti useranno questo.'
      : 'Bozza da rivedere e confermare.';

  return (
    <>
      <h1>Ciao {user?.fullName}</h1>
      <section className="grid">
        <Link to="/cv" className="card card-link">
          <h2>I miei CV</h2>
          <p className="muted">
            {cvCount === 0 ? 'Carica il tuo CV in PDF o DOCX.' : `${cvCount} CV caricati. Valutali o estrai il profilo.`}
          </p>
        </Link>
        <Link to="/profile" className="card card-link">
          <h2>
            Profilo{' '}
            {profile.data && (
              <span className={`badge ${profile.data.status === 'CONFIRMED' ? 'badge-good' : 'badge-warning'}`}>
                {profile.data.status === 'CONFIRMED' ? 'Confermato' : 'Bozza'}
              </span>
            )}
          </h2>
          <p className="muted">{profileText}</p>
        </Link>
        <Link to="/preferences" className="card card-link">
          <h2>Preferenze</h2>
          <p className="muted">Ruoli, città, modalità di lavoro e RAL per la ricerca delle offerte.</p>
        </Link>
        <article className="card card-disabled">
          <h2>Offerte e candidature</h2>
          <p className="muted">In arrivo nelle prossime fasi.</p>
        </article>
      </section>
    </>
  );
}
