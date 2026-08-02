@../../GLOBAL_DESCRIPTION_LM_PROJECT.md

You are an expert in Java, Spring Boot 4, and building secure, maintainable REST APIs.
Versions (verified 2026-08-01): Java 25 (LTS), Spring Boot 4.1.0, Spring Framework 7, PostgreSQL 18. Boot 4 renamed starters — this project uses spring-boot-starter-webmvc (not -web) and spring-boot-starter-liquibase. Spring Boot has no LTS; plan a minor upgrade roughly yearly.

## Architecture

- Layered structure: controller -> service -> repository. Controllers stay thin -- no
  business logic.
- Entities and DTOs are separate. Use MapStruct for mapping between them. Never expose
  entities directly through the API.
- Package by feature (e.g. `order`, `product`, `auth`), not by layer.

## Spring Boot conventions

- Constructor injection only. No field injection (`@Autowired` on fields).
- Use `record` for DTOs where immutability fits.
- Validate incoming DTOs with Jakarta Bean Validation (`@Valid`, `@NotNull`, etc.) at
  the controller boundary.
- Centralize exception handling with `@RestControllerAdvice`. No try/catch for
  control flow in controllers.
- Document endpoints with SpringDoc OpenAPI annotations where behavior isn't obvious
  from the method signature.

## Security

- Spring Security + JWT for stateless auth, Google OAuth2 for social login (see
  imported project description -- these are decided).
- Never log tokens, passwords, or full JWTs.
- Keep password/secret handling consistent with standard Spring Security practices
  (BCrypt, no custom crypto for auth unless explicitly discussed).

## Database

- Liquibase changesets live in `db/changelog/`. One changeset per logical change, never
  edit an already-applied changeset -- add a new one.
- Prefer explicit column definitions over relying on Hibernate auto-DDL in anything
  that touches production.

## Testing

- JUnit 5 + Mockito for unit tests.
- Testcontainers (real PostgreSQL via `@ServiceConnection`) for integration tests. Do
  not substitute H2.
- WireMock for stubbing external HTTP dependencies (e.g. payment gateway) in tests.
