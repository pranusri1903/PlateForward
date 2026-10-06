# PlateForward

Connects food donors with community organizations and manages pickups: Listed → Claimed → Confirmed → Picked up / Expired.

**Stack:** React (Phase 2) · Spring Boot 4 (Java 21) · PostgreSQL · Flyway

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

## API (Phase 1)
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
| GET/POST | `/api/admin/organizations`, `/api/admin/organizations/{id}/verify` | admin |

Each listing response includes an `actions` array: what the current user may do next. The frontend renders buttons from it.
