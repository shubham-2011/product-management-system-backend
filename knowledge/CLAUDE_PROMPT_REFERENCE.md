# Claude & LLM Context Prompt Reference

Copy and paste this prompt when starting a new session with Claude or any AI coding assistant:

```text
You are assisting with development on the "Product Management System" (PMS) codebase.

Here is the domain and technical context:
1. Architecture:
   - Backend: Spring Boot 3.2.5 (Java 17), Spring Security 6, Spring Data JPA / Hibernate, JJWT 0.11.5, Neon Serverless PostgreSQL.
   - Frontend: Angular 18 (Standalone components, SSR support, RxJS, Custom INR Pipe, Dark Glassmorphism Vanilla CSS).
   - Deployment: Containerized with Docker Compose, Nginx reverse proxy, Render.com backend, Vercel frontend.

2. Core Business Logic:
   - Multi-tenant shopkeeper/admin system: Users own multiple Shops, which have Categories and Products.
   - Batch-Level Inventory (FIFO): When stock is purchased, individual `InventoryBatch` records are stored with unique `costPrice`, `expiryDate`, and `quantity`. When a sale occurs, `SalesService` deducts inventory using First-In-First-Out (FIFO) and computes real item profit = (sellingPrice - costPrice) * quantity.
   - Dynamic Clearance Pricing: If an `InventoryBatch` has a clearance discount applied, `SalesService` automatically applies it during checkout.
   - Smart Heuristics & Recommendations (`RecommendationController`):
     - Calculates 4-factor risk scores (Expiry Urgency 40%, Stock Pressure 25%, Low Demand 20%, Waste History 15%) to suggest clearance discounts (5% to 80%).
     - Calculates Sales Velocity (units/day) to predict days until stock depletion and issues restock alerts (HIGH, MEDIUM, LOW).
     - Calculates stock health score, total potential loss, and recoverable amount.
   - Reporting: Inventory Report (batch status, expiry audit, loss calculation) and Profit Report (Gross Profit, Net Profit = Gross Profit - Expired Stock Loss, Investment).

3. Security & Roles:
   - Roles: `ADMIN` and `SHOPKEEPER`.
   - Authentication: Stateless JWT via `HttpOnly` Secure Cookie (`jwt-token`) + Client Cookie (`user-info`).
   - Angular Guards: `authGuard` and `guestGuard`.

4. Guidelines for Modifications:
   - Always track and deduct stock at the `InventoryBatch` level, never directly on product counters.
   - Maintain the dark glassmorphism design system in frontend styles.
   - Ensure all API endpoints validate shop ownership against the authenticated user (`Principal.getName()`).
```
