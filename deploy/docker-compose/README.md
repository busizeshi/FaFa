# Local Docker Compose

1. Copy `.env.example` to `.env`. The supplied values are local-only development defaults.
2. Start infrastructure: `docker compose -f compose.dev.yaml --profile core up -d`.
3. In the Nacos console at `http://localhost:18080`, publish each file under `nacos/configs/` with the exact filename as its Data ID and `FAFA_GROUP` as its group.
4. Start the applications: `docker compose -f compose.dev.yaml --profile core --profile apps up --build`.

Local ports: Gateway `8080`, Nacos API `8848`, Nacos console `18080`, MySQL `3306`, Redis `6379`. MySQL and Redis are bound to loopback for local development. The Java services and Python service are internal to the Compose network; access them through Gateway.

The local Nacos instance disables authentication only to avoid adding local setup friction. Never reuse this setting for a shared or production Nacos instance. Production credentials must be injected by the deployment environment, and management services must remain private.

The SQL init script creates separate `fafa_admin` and `fafa_biz` schemas and grants the local development account access to both. This is one local MySQL instance and one development account; production should use separate credentials and least-privilege grants per service.

Profiles keep costs and local resource use controlled. Start only `core` while working on infrastructure or Java configuration; add `apps` for end-to-end service development. Queue and vector database containers are intentionally added when their first application integration is implemented.
