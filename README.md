
# DietMath

Free and open-source calorie counter with full self-hosting support.

DietMath is a self-hosted calorie and nutrition tracking application designed for small setups (personal use, family, or up to ~10 users).  
The system follows a clean client–server architecture: the backend exposes an API, and all clients interact exclusively through it.

---

## Goals

- Fully self-hostable
- Simple to deploy and maintain
- Single backend serving multiple clients (CLI, web, mobile)
- Built-in common food database + user-defined foods
- No external dependencies

---

## Already Have

### Ready Features

- Spring Boot backend scaffold (Java, Maven)
- PostgreSQL + Flyway migrations
- Docker Compose for app + db
- Home screen with links (README, DB status, login, register)
- Registration + login flow
- User page with profile form
- Weight history entries
- BMI calculator
- Daily calorie calculator (dynamic/fixed)

### Run Everything (app + db)

```bash
docker compose up --build
```

App: `http://localhost:8080`

### Run Only Database (for Eclipse/local Spring Boot)

```bash
docker compose up -d db
```

### Stop Containers

```bash
docker compose down
```

---

## Architecture Overview

### Backend
- **Technology**: Spring Boot + Java
- **Database**: PostgreSQL
- **Authentication**: username + password
- **API**: REST (JSON)
- **Migration**: Flyway

### Clients
- Phase 1: CLI client (for fast iteration and testing)
- Phase 2: Web client
- Phase 3: Mobile app 

All clients communicate **only via API calls**.

---

## Data Model (High Level)

### Users
- Login credentials
- Personal settings (targets, preferences)

### Foods
- Common (built-in) foods
- User-defined foods
- Stored in a single table, differentiated by source:
	- `BUILTIN`
	- `USER`

### Diary Entries
- User
- Date
- Food
- Quantity (grams)

---

## Common Food Database

- Stored as **CSV files** in the repository
- Loaded into the database via an **importer**
- Each built-in food has a stable `external_id`
- Import is **idempotent** (safe to re-run, supports updates)

This allows:
- Version-controlled food data
- Easy community contributions
- Automatic initialization on first startup

---

## Roadmap

### Phase 0 – Project Foundation
- Initialize Spring Boot project
- Set up PostgreSQL configuration
- Add database migration tool
- Basic project structure (domain, service, repository, api)

---

### Phase 1 – Core Backend (CLI-first)
**Goal**: Fully usable system without UI

- User registration & login
- Food entity + repository
- CSV food importer
- Diary entry CRUD
- Daily calorie calculation
- Simple CLI client:
	- Login
	- Search foods
	- Add diary entries
	- Show daily summary

Outcome:  
A working calorie tracker usable entirely from the terminal.

---

### Phase 2 – API Stabilization
- Clean REST API design
- DTOs and validation
- Error handling & consistent responses
- Pagination and search for foods
- Basic authorization (user can only access own data)

Outcome:  
Stable API ready for multiple clients.

---

### Phase 3 – Web Client (PWA)
- Web UI consuming the existing API
- Login & session handling
- Food search and quick add
- Daily / weekly overview
- Offline-first basics (optional)

Outcome:  
Usable in browser and installable as an app.

---

### Phase 4 – Mobile App
- Reuse existing API
- Native features (optional):
	- Offline sync
	- Barcode scanning
	- Notifications

Outcome:  
Full cross-platform experience.

---

### Phase 5 – Nice-to-haves
- Macro goals (protein/carbs/fat)
- Recipes (composed foods)
- Import/export (CSV/JSON)
- Admin tools (manage users, rebuild seed data)
- Backup/restore helpers

---

## Non-Goals (for now)
- Cloud hosting
- Social features
- Public food editing
- Ads or tracking

---

## License
- GPL-3.0 license

---

## TODO

- Add auth (login/token/session)
- Build core REST API (users, foods, diary entries)
- CSV food importer
- CLI client for Phase 1
