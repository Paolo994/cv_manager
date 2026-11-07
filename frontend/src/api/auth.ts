import { ApiError, apiRequest } from './client';

export type CurrentUser = {
  id: string;
  email: string;
  fullName: string;
  role: 'USER' | 'ADMIN';
};

export type RegisterRequest = {
  email: string;
  password: string;
  fullName: string;
};

/** Utente corrente, oppure null se non c'è una sessione valida. */
export async function fetchCurrentUser(): Promise<CurrentUser | null> {
  try {
    return await apiRequest<CurrentUser>('/api/auth/me');
  } catch (e) {
    if (e instanceof ApiError && e.status === 401) {
      return null;
    }
    throw e;
  }
}

export function login(email: string, password: string): Promise<void> {
  return apiRequest<void>('/api/auth/login', { method: 'POST', form: { email, password } });
}

export function logout(): Promise<void> {
  return apiRequest<void>('/api/auth/logout', { method: 'POST' });
}

export function register(request: RegisterRequest): Promise<CurrentUser> {
  return apiRequest<CurrentUser>('/api/auth/register', { method: 'POST', json: request });
}
