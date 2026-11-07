# CV Manager

Webapp multi-agente per gestire CV, cercare offerte e preparare candidature.
Spring Boot 4.1 (BFF + Spring Security) e SPA React, distribuiti in un unico JAR.

## Requisiti

- Java 21, Maven 3.9+
- Docker (PostgreSQL + pgvector in locale, Testcontainers nei test)
- Node 22 solo per lo sviluppo del frontend (la build Maven scarica Node da sola)

## Sviluppo

```bash
# 0. Una sola volta: crea .env (non versionato) e scegli una password per il DB locale
cp .env.example .env

# 1. Backend su :8080 (avvia da solo docker-compose.yml -> Postgres su localhost:5433)
mvn -pl backend -am spring-boot:run "-Dskip.npm" "-Dskip.installnodenpm"

# 2. Frontend su :5173 con hot reload (/api inoltrato a :8080)
cd frontend
npm install
npm run dev
```

Apri http://localhost:5173.

## Build e test

```bash
mvn verify                      # compila React, backend, esegue i test (serve Docker)
docker compose up -d            # Postgres per eseguire il JAR
DB_PASSWORD=... java -jar backend/target/cv-manager.jar   # app completa su http://localhost:8080
```

`-Dskip.npm -Dskip.installnodenpm` salta la build del frontend quando cambia solo il backend.

## Configurazione

| Variabile        | Default                                     | Note                              |
|------------------|---------------------------------------------|-----------------------------------|
| `DB_URL`         | `jdbc:postgresql://localhost:5433/cvmanager` |                                   |
| `DB_USER`        | `cvmanager`                                 |                                   |
| `DB_PASSWORD`    | nessuno                                     | obbligatoria, in `.env` in locale |
| `COOKIE_SECURE`  | `false`                                     | `true` in produzione (HTTPS)      |

## API di autenticazione

| Metodo | Path                 | Body                               | Risposta            |
|--------|----------------------|------------------------------------|---------------------|
| GET    | `/api/auth/csrf`     | -                                  | 204 + cookie `XSRF-TOKEN` |
| POST   | `/api/auth/register` | JSON `{email, password, fullName}` | 201 utente, 409 email usata |
| POST   | `/api/auth/login`    | form `email`, `password`           | 204 + cookie `SESSION`, 401 |
| POST   | `/api/auth/logout`   | -                                  | 204                 |
| GET    | `/api/auth/me`       | -                                  | 200 utente, 401     |

Ogni richiesta diversa da GET richiede l'header `X-XSRF-TOKEN` con il valore del cookie `XSRF-TOKEN`.
