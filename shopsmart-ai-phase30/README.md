# ShopSmart AI
**Phase 1 of 4 (30%) - Catalog and accounts**

Spring Boot 3.5 · Spring AI · Java 21 · PostgreSQL 16 + pgvector · React 19 · Vite · Tailwind 4

## Run it
**Needs:** Docker Desktop (running). Nothing else.
```bash
docker compose up --build
```
First build takes a few minutes (Maven and npm download dependencies). Then:

| What | Where |
|---|---|
| Shop (UI) | http://localhost:3000 |
| API docs | http://localhost:8080/swagger-ui.html |

Stop with `Ctrl+C`. **Starting from a clean database:** `docker compose down -v` (do this when switching to another phase zip, it is the safest route; moving up a phase on the same database also works).

Without an LLM key everything runs: intent parsing uses rules and policy answers are extractive quotes with sources. To use a model, copy `.env.example` to `.env` and set `OPENAI_API_KEY` (any OpenAI-compatible endpoint via `OPENAI_BASE_URL`).

## Demo accounts
| Email | Password | Role |
|---|---|---|
| admin@shopsmart.test | Admin@123 | Staff (admin screens) |
| basic@shopsmart.test | Demo@1234 | BASIC member |
| prime@shopsmart.test | Demo@1234 | PRIME member |
| premium@shopsmart.test | Demo@1234 | PREMIUM member |

## What works in this phase
- **Catalog:** 108 seeded products in 8 categories; full-text + typo-tolerant search, filters (brand, price, rating, specs), sorting, paging
- **Product page:** gallery, specs, live availability across 3 warehouses; **compare** up to 4 products side by side
- **Accounts:** register, login (JWT with refresh), profile, shopping preferences, saved addresses; tiers BASIC / PRIME / PREMIUM
- **Staff area:** Inventory screen (stock per warehouse, low-stock filter, restock with movement ledger)

## Demo script (about 5 minutes)
1. Open the shop, search "laptop", filter by brand and price, sort by rating.
2. Add two products to compare; show the spec table.
3. Log in as `prime@shopsmart.test` and open Profile: preferences and addresses.
4. Log in as `admin@shopsmart.test`, open Staff > Inventory, filter low stock, restock one item.

## Coming next
Phase 2 adds wishlist, cart, checkout with tier pricing, orders and mock shipment tracking.

## Run without Docker
```bash
docker compose up db                        # Postgres + pgvector only
cd backend && mvn -pl app -am spring-boot:run
cd frontend && npm install && npm run dev   # http://localhost:5173
cd frontend && npm run dev:mock             # UI only, mock API, no backend needed
```
