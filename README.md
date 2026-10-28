# PlateForward

[![CI](https://github.com/pranusri1903/PlateForward/actions/workflows/ci.yml/badge.svg)](https://github.com/pranusri1903/PlateForward/actions/workflows/ci.yml)

Connects food donors with community organizations and manages pickups: Listed → Claimed → Confirmed → Picked up / Expired.

**Stack:** React (Phase 2) · Spring Boot 4 (Java 21) · PostgreSQL · Flyway

## Run everything with Docker (easiest)
Needs only [Docker Desktop](https://www.docker.com/products/docker-desktop/).
```bash
docker compose up --build        # first run takes a few minutes while images build
```
| What | Where |
|---|---|
| App | http://localhost:5173 (log in as `foodbank@demo.com` / `demo1234`) |
| Email inbox (Mailpit) | http://localhost:8025, every email the app sends lands here |
| API health | http://localhost:8080/actuator/health |

Stop with `Ctrl+C`. `docker compose down -v` also wipes the database. The stack starts PostgreSQL, the Spring Boot backend (demo profile with sample data),
the React site behind nginx (which forwards `/api` to the backend) and Mailpit.

## Run the backend
```bash
createuser plateforward && createdb -O plateforward plateforward   # one-time (password: plateforward)
cd backend && ./mvnw spring-boot:run                               # http://localhost:8080
```
Flyway creates the schema on startup. A first admin is seeded from `ADMIN_EMAIL` / `ADMIN_PASSWORD`
(defaults: `admin@plateforward.local` / `admin12345`; override both outside development, along with `JWT_SECRET`).

## Run the frontend
```bash
cd frontend && npm install && npm run dev   # http://localhost:5173 (proxies /api to :8080)
```
Set `VITE_API_URL` to the backend's address for production builds.

## Background jobs and email
A job runs every minute (`JOBS_INTERVAL`) and: expires listings past their expiry time, returns unconfirmed claims to the board
after the 2-hour hold, emails both parties an hour before a confirmed pickup, and warns donors 3 hours before an unclaimed listing expires.
It locks rows with `SKIP LOCKED`, so it is safe alongside user actions and multiple instances.

Emails are HTML, sent after the database transaction commits. Without a mail server they are written to the log instead.
To send real mail set `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, plus `MAIL_FROM` and `FRONTEND_URL` (used in links).

## Demo data
`SPRING_PROFILES_ACTIVE=demo ./mvnw spring-boot:run` seeds sample accounts (password `demo1234`: `bakery@demo.com`, `bistro@demo.com`, `market@demo.com`,
`maya@demo.com`, `foodbank@demo.com`, `shelter@demo.com` (unverified), `sam@demo.com`), a mix of listings in every status, reviews and an open dispute.
The admin account comes from `ADMIN_EMAIL` / `ADMIN_PASSWORD`.

## API
| Method | Path | Who |
|---|---|---|
| POST | `/api/auth/register`, `/api/auth/login` | public |
| GET | `/api/me` | any user |
| GET | `/api/listings?q=&category=&storage=&sort=expiring\|newest&page=&size=` | public |
| GET | `/api/listings/{id}` | public (address hidden until confirmed) |
| GET | `/api/listings/mine` | donors/givers: own listings · others: own claims |
| POST | `/api/listings` | donor, giver |
| POST | `/api/listings/{id}/{claim\|confirm\|decline\|release\|pickup}` | by role and status |
| DELETE | `/api/listings/{id}` | donor, while unclaimed |
| POST | `/api/listings/{id}/feedback` (review, no-show or issue) | the donor or claimer of that listing |
| GET | `/api/listings/history`, `/api/listings/history.csv` | any user (own finished listings) |
| GET/POST | `/api/admin/organizations`, `/api/admin/organizations/{id}/verify` | admin |
| GET/POST | `/api/admin/disputes`, `/api/admin/disputes/{id}/resolve` | admin |

Each listing response includes an `actions` array: what the current user may do next. The frontend renders buttons from it.
