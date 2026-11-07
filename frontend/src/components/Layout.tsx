import { NavLink, Outlet, useNavigate } from 'react-router';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { logout } from '../api/auth';
import { CURRENT_USER_KEY, useCurrentUser } from '../auth/useCurrentUser';

const NAV = [
  { to: '/', label: 'Home', end: true },
  { to: '/cv', label: 'CV', end: false },
  { to: '/profile', label: 'Profilo', end: false },
  { to: '/preferences', label: 'Preferenze', end: false },
];

export function Layout() {
  const { data: user } = useCurrentUser();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const logoutMutation = useMutation({
    mutationFn: logout,
    onSettled: () => {
      queryClient.clear();
      queryClient.setQueryData(CURRENT_USER_KEY, null);
      navigate('/login', { replace: true });
    },
  });

  return (
    <div className="layout">
      <header className="topbar">
        <div className="topbar-left">
          <strong className="brand">CV Manager</strong>
          <nav className="nav">
            {NAV.map((item) => (
              <NavLink key={item.to} to={item.to} end={item.end} className={({ isActive }) => (isActive ? 'active' : '')}>
                {item.label}
              </NavLink>
            ))}
          </nav>
        </div>
        <div className="topbar-user">
          <span className="muted">{user?.email}</span>
          <button className="secondary small" onClick={() => logoutMutation.mutate()} disabled={logoutMutation.isPending}>
            Esci
          </button>
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
