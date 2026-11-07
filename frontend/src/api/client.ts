export class ApiError extends Error {
  readonly status: number;
  readonly detail?: unknown;

  constructor(status: number, message: string, detail?: unknown) {
    super(message);
    this.status = status;
    this.detail = detail;
  }
}

function readCookie(name: string): string | undefined {
  const entry = document.cookie.split('; ').find((c) => c.startsWith(`${name}=`));
  return entry ? decodeURIComponent(entry.substring(name.length + 1)) : undefined;
}

/** Il backend imposta il cookie XSRF-TOKEN; ogni richiesta che modifica dati lo rimanda come header. */
async function csrfToken(): Promise<string> {
  let token = readCookie('XSRF-TOKEN');
  if (!token) {
    await fetch('/api/auth/csrf', { credentials: 'same-origin' });
    token = readCookie('XSRF-TOKEN');
  }
  if (!token) {
    throw new ApiError(0, 'Token CSRF non disponibile');
  }
  return token;
}

type RequestOptions = {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  json?: unknown;
  form?: Record<string, string>;
};

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const method = options.method ?? 'GET';
  const headers: Record<string, string> = { Accept: 'application/json' };
  let body: BodyInit | undefined;

  if (options.json !== undefined) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(options.json);
  } else if (options.form) {
    body = new URLSearchParams(options.form);
  }
  if (method !== 'GET') {
    headers['X-XSRF-TOKEN'] = await csrfToken();
  }

  const response = await fetch(path, { method, headers, body, credentials: 'same-origin' });

  if (!response.ok) {
    let detail: unknown;
    let message = response.statusText || `Errore ${response.status}`;
    try {
      detail = await response.json();
      const problem = detail as { detail?: string; title?: string };
      message = problem.detail ?? problem.title ?? message;
    } catch {
      // risposta senza body JSON
    }
    throw new ApiError(response.status, message, detail);
  }

  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}
