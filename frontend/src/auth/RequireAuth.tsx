import type { ReactNode } from 'react';
import { Navigate } from 'react-router';
import { useCurrentUser } from './useCurrentUser';

export function RequireAuth({ children }: { children: ReactNode }) {
  const { data: user, isPending, isError } = useCurrentUser();

  if (isPending) {
    return <p className="centered muted">Caricamento…</p>;
  }
  if (isError) {
    return <p className="centered error">Il server non risponde. Riprova tra poco.</p>;
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}
