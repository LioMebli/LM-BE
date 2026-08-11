@../../GLOBAL_DESCRIPTION_LM_PROJECT.md

You are an expert in Java, Spring Boot 4, and building secure, maintainable REST APIs.
Versions (verified 2026-08-01): Java 25 (LTS), Spring Boot 4.1.0, Spring Framework 7, PostgreSQL 18. Boot 4 renamed starters — this project uses spring-boot-starter-webmvc (not -web) and spring-boot-starter-liquibase. Spring Boot has no LTS; plan a minor upgrade roughly yearly.

## Architecture

- **Package by feature, layers inside the feature** — `catalog`, `product`,
  `selection`, `auth`, plus `common` for what is genuinely shared. Never a top-level
  `controller/`, `service/` or `dto/`. The full tree is `docs/ARCHITECTURE.md` §7.
- Within a feature the flow is controller -> service -> repository. Controllers stay
  thin -- no business logic.
- Entities and DTOs are separate. Use MapStruct for mapping between them. Never expose
  entities directly through the API.
- A feature may depend on another feature's **service**, never on its repository.

## Spring Boot conventions

- Constructor injection only. No field injection (`@Autowired` on fields).
- Use `record` for DTOs where immutability fits.
- Validate incoming DTOs with Jakarta Bean Validation (`@Valid`, `@NotNull`, etc.) at
  the controller boundary.
- Centralize exception handling with `@RestControllerAdvice`. No try/catch for
  control flow in controllers.
- **Error handling is specified in full in `docs/ERROR_HANDLING.md`.** Read it before
  adding an exception class or touching the handler. In short: the domain never
  imports `org.springframework.web`, the hierarchy is `BaseException` -> category ->
  concrete and never four levels deep, constructors take typed parameters and never
  `Object...`, every exception carries a stable `ErrorCode`, and every error response
  is RFC 9457 `ProblemDetail` including the ones Spring raises itself.
- Document endpoints with SpringDoc OpenAPI annotations where behavior isn't obvious
  from the method signature.

## Security

- Spring Security + JWT for stateless auth, Google OAuth2 for social login (see
  imported project description -- these are decided).
- Never log tokens, passwords, or full JWTs.
- Admin login is Google OAuth2; there is no password to store for normal operation.
  The disabled-by-default local fallback account uses **Argon2id** via
  `DelegatingPasswordEncoder` (prefix `{argon2}`) — the current OWASP recommendation,
  **not BCrypt**. No custom crypto for auth unless explicitly discussed.

## Database

- **Every identifier is `bigint` / `Long`.** Primary keys and foreign keys alike, in
  the schema, in the entity and in the OpenAPI contract (`type: integer` with
  `format: int64`). Never `int`/`Integer`. Industry default, not a capacity argument.
  Money is the exception and stays `int`: see `docs/ARCHITECTURE.md` §5.3.
- Liquibase changesets live in `db/changelog/`. One changeset per logical change, never
  edit an already-applied changeset -- add a new one.
- Prefer explicit column definitions over relying on Hibernate auto-DDL in anything
  that touches production.

## Testing

- JUnit 5 + Mockito for unit tests.
- Testcontainers (real PostgreSQL via `@ServiceConnection`) for integration tests. Do
  not substitute H2.
- WireMock for stubbing external HTTP dependencies in tests — Google OIDC, the email
  provider, the GitHub `repository_dispatch` call. There is no payment gateway: v1 has
  no checkout at all.
