# AGENTS Guide for JewelryOnlineStore

## Snapshot
- Stack: Spring Boot MVC + Thymeleaf + Spring Security + Spring Data JPA (`pom.xml`). Java 17.
- Architecture is classic controller -> service -> repository -> entity; DTOs are used for web-facing payloads (`src/main/java/com/jewelryonlinestore/dto`).
- App is split by user journey tags in comments: customer `C01..C08`, admin `A02..A09` (see controllers).
- Existing AI-instruction files search currently finds only `AGENTS.md` (no `.github/copilot-instructions.md`, `AGENT.md`, `CLAUDE.md`, `.cursorrules`, `.windsurfrules`, `.clinerules`, or `README.md`).

## Core Structure You Should Follow
- Entry/config: `src/main/java/com/jewelryonlinestore/config` (`SecurityConfig`, `WebMvcConfig`, `OAuth2Config`).
- Customer UI controllers: `src/main/java/com/jewelryonlinestore/controller/customer`.
- Admin UI controllers: `src/main/java/com/jewelryonlinestore/controller/admin`.
- Persistence: entities in `src/main/java/com/jewelryonlinestore/entity`, repositories in `src/main/java/com/jewelryonlinestore/repository`.
- Services are mostly interfaces in `src/main/java/com/jewelryonlinestore/service`; concrete implementations now live in `service/impl`.

## Request/Data Flow Patterns (project-specific)
- Server-rendered pages return Thymeleaf view names; AJAX endpoints return `ApiResponse<T>` wrappers (`dto/response/ApiResponse.java`).
- Cart/checkout flow threads through `CartController -> CartService -> CartRepository/CartItemRepository` and then `OrderController -> OrderService`.
- Product listing uses filter DTO + repository queries (`ProductFilterRequest`, `ProductRepository.filterProducts`, `fullTextSearch`).
- Wishlist is implemented as a concrete service class (`service/WishlistService.java`), not an interface unlike most services.
- Security split: URL authorization in `SecurityConfig`, plus method-level guards (`@PreAuthorize`) on admin controllers.

## Integrations and External Dependencies
- MySQL is required; no SQL migrations found. Local schema evolution currently relies on `spring.jpa.hibernate.ddl-auto=update` (`application.properties`).
- OAuth2 login + form login are both wired in `SecurityConfig`; callback/profile-completion flow is in `AuthController`.
- Email SMTP settings exist in `application.properties`, but current sending flow is async service logging in `service/impl/EmailServiceImpl.java`; templates remain under `src/main/resources/templates/email`.
- File uploads are stored on local filesystem under `app.upload.dir` via `FileStorageService`; exposed by `WebMvcConfig` as `/uploads/**`.
- Payment gateway callbacks are expected at `/payment/vnpay/callback` and `/payment/momo/callback` (`PaymentController`).

## Current Build/Test Reality (important before editing)
- Run commands from project root with wrapper:
  - `./mvnw.cmd test`
  - `./mvnw.cmd spring-boot:run`
- Current compile status (verified on 2026-03-20): `./mvnw.cmd -DskipTests compile` passes.
- Current test/runtime caveat:
  - `spring.jpa.hibernate.ddl-auto` is now `update` in `application.properties` (not `validate`).
  - `./mvnw.cmd test` currently passes in this workspace setup, but still depends on a reachable MySQL instance from `spring.datasource.*`.
- Lombok-generated accessors/builders are referenced heavily; if IDE shows missing getters/builders, ensure annotation processing is enabled.

## Conventions and Gotchas to Preserve
- Domain enums are often nested inside entities (`Order`, `User`, `Review`) and reused in business logic.
- Repository query params frequently use lowercase status strings (`"delivered"`, `"approved"`) even where entity enums are uppercase; do not "fix" blindly without full query audit.
- Keep current naming as-is (`AddressResponse`, `WishlistController`); do not reintroduce older typo variants from legacy docs/snippets.
- `target/` contains generated artifacts; treat it as build output, not source of truth.

## Practical Agent Workflow
- Before non-trivial edits, inspect both controller view names and actual template paths to avoid breaking routing.
- Prefer adding/adjusting DTOs and repository methods alongside controller/service changes (this codebase is DTO-centric at boundaries).
- When implementing missing pieces, prioritize unblocking compile in this order: empty security/service contracts -> package/type mismatches -> controller/template path alignment.
- After each change set, rerun `./mvnw.cmd test` to catch cross-layer compile regressions quickly.

