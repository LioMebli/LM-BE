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
- **Error handling is specified below, in § Error handling.** It is normative: code
  that diverges from it is the thing that changes.
- Document endpoints with SpringDoc OpenAPI annotations where behavior isn't obvious
  from the method signature.

## Error handling

Normative for how errors are raised, translated and reported. Section numbers are
stable — Java comments, the constitution and `specs/LM-10` cite them as `§3`, `§6`
and so on. Until 2026-08-15 this lived in `LM-BE/docs/ERROR_HANDLING.md`; closed
specs still name that path, and this section is what they mean.

### §1 The shape of the thing

```
domain / service layer          web layer                    client
─────────────────────────       ──────────────────────       ──────────
throws BaseException     ──►    one @RestControllerAdvice ──► application/problem+json
(no HTTP types at all)          translates to ProblemDetail
```

Three rules follow, and everything below is a detail of one of them.

1. **The domain describes what went wrong. It never describes the HTTP response.**
2. **Exactly one place turns a domain error into a transport response.**
3. **Nothing reaches the client that was not deliberately put there.**

The translation step is an **Adapter** at the web boundary — the same role the
controller plays for the happy path. Keeping it there is what lets the service layer
stay HTTP-free, which also keeps the door open for the deferred MCP server: the same
services with a completely different transport.

### §2 Hierarchy — three levels, never four

| Level | Example | Purpose |
|---|---|---|
| **Root** | `BaseException` | one type to catch, carries the error code |
| **Category** | `NotFoundException`, `ConflictException` | what the handler dispatches on |
| **Concrete** | `ProductNotFoundException` | what the caller reads and what carries the data |

The category level exists for exactly one reason: `@ExceptionHandler(NotFoundException.class)`
catches every subtype. That is polymorphic dispatch doing real work — it is why the
abstraction earns its place, and why the categories must stay few.

| Category | Meaning | Status the handler assigns |
|---|---|---|
| `NotFoundException` | referenced thing does not exist | 404 |
| `ConflictException` | state forbids the operation (duplicate, already used, wrong state) | 409 |
| `ValidationException` | input is well-formed but semantically wrong (business rule) | 422 |
| `ExternalServiceException` | a dependency we do not control failed | 502 / 503 |

Syntactic validation (`@NotNull`, `@Size`) is **not** in this list — Bean Validation
raises `MethodArgumentNotValidException` at the controller boundary, handled
separately as 400.

There is no `AlreadyExistsException`: "already exists", "already used" and "wrong
state" all mean the same thing to a client, and one category with a precise `code`
beats three categories with vague ones.

### §3 Constructors are typed. No `Object...`.

```java
public final class ProductNotFoundException extends NotFoundException {

    private static final String MESSAGE = "Product %d not found";

    public ProductNotFoundException(long productId) {
        super(ErrorCode.PRODUCT_NOT_FOUND, MESSAGE.formatted(productId));
    }
}
```

- Constructor parameters are **typed and named**. The signature documents what the
  error is about.
- The message pattern is a **private constant of the class that owns it**.
- Concrete exceptions are `final`. Subclass one and the hierarchy is four deep, which
  nobody can hold in their head.

**Why `Object... formatArgs` is banned.** With it, `new ProductNotFoundException()`
compiles, and at runtime `formatted()` throws `MissingFormatArgumentException` — an
exception thrown while constructing an exception, on the error path, where it is least
likely to be covered by a test. The compiler had the information to stop that and was
told not to use it. The same smell is a dummy argument invented to satisfy a signature
that should not have needed one.

**No global `ExceptionMessages` class.** It looks like consistency and behaves like a
shared mutable surface: every new exception in any feature edits one file, unrelated
features conflict in it, and nothing stops a class from picking a constant belonging to
something else. The pattern belongs next to its only user. That is cohesion. If
per-locale error text is ever needed the mechanism is `MessageSource` and a resource
bundle keyed on `ErrorCode` — which the framework already resolves for `ProblemDetail`
— not a constants class.

### §4 `ErrorCode` — what makes a custom exception worth having

A stable identifier the frontend can branch on. Message text is developer-facing and
may be reworded at any time; `code` is part of the API contract and is not. This is
what a custom exception buys over `IllegalArgumentException`: not a nicer message, a
**contract**. If a new type would produce no new `code` and be handled no differently,
it should not exist — see §8.

`ErrorCode` deliberately does **not** carry an `HttpStatus`. Status is transport and
lives in the handler; putting it here would drag `org.springframework.http` back into
the domain through the front door after §5 pushed it out the back.

### §5 `@ResponseStatus` on exceptions: forbidden

Stating the status on the exception, on the handler method and inside the response body
is three copies of one fact; the first one edited alone wins silently. Worse,
`@ResponseStatus` lives in `org.springframework.web.bind.annotation`, so putting it on
an exception that services throw means the service layer imports Spring Web — the exact
coupling this convention exists to prevent.

**Status is assigned in the handler and nowhere else.** The same reasoning rules out
implementing Spring's `ErrorResponse` interface on `BaseException`, tidy as that looks:
it puts `HttpStatusCode` and `ProblemDetail` in the domain.

### §6 The wire format is RFC 9457, not a bespoke record

Spring Framework 7 supports Problem Details for HTTP APIs natively through
`ProblemDetail`, serialised as `application/problem+json`. A hand-rolled
`ExceptionResponse` record is one more thing to document, version and explain, in
exchange for nothing.

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Product 4711 not found",
  "instance": "/api/v1/products/4711",
  "code": "PRODUCT_NOT_FOUND",
  "traceId": "b7c1f0e2a9d4"
}
```

`code` and `traceId` are added via `ProblemDetail.setProperty(...)`.

**The handler extends `ResponseEntityExceptionHandler`.** Handle only the domain
categories and everything else — malformed JSON, wrong method, unknown path,
unsupported media type — falls through to Spring's default error page and comes back
in a different shape than every deliberate error in the API. A client cannot parse that
generically. `ResponseEntityExceptionHandler` already renders all built-in Spring MVC
exceptions as `ProblemDetail`: extend it, annotate `@RestControllerAdvice`, add methods
for the four domain categories plus a catch-all.

```java
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException ex) { ... }   // 404

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException ex) { ... }   // 409

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) { ... }         // 500, no detail
}
```

The catch-all is not optional. Without it, the one thing guaranteed to happen in
production — an exception nobody predicted — is the one case with no defined behaviour.

> `spring.mvc.problemdetails.enabled=true` autoconfigures such a handler at order 0.
> Extending the class directly is more explicit and avoids ordering questions — prefer it.

`MethodArgumentNotValidException` returns a `ProblemDetail` with status 400 and an added
`errors` property (field → message). Not a bare `Map`, and not a method whose return
type is `Object`. If a timestamp is added at all it is `Instant`: `LocalDateTime` in an
API response carries no zone and is ambiguous the moment anything crosses a machine
boundary.

### §7 Package layout

```
com.vansisto.lmbe
├── common
│   └── error
│       ├── BaseException.java
│       ├── ErrorCode.java
│       ├── NotFoundException.java
│       ├── ConflictException.java
│       ├── ValidationException.java
│       ├── ExternalServiceException.java
│       └── web
│           └── ApiExceptionHandler.java
├── catalog
│   ├── CategoryService.java
│   └── CategoryNotFoundException.java
├── product
│   ├── ProductService.java
│   └── ProductNotFoundException.java
└── selection
    └── SelectionExpiredException.java
```

**`catalog` and `product` are two features, not one.** The tree above said
`catalog/ProductService.java` until 2026-08-22, which contradicted both the code — where
`ProductService` has lived in `product/` since LM-10 — and `docs/ARCHITECTURE.md` §7, which
lists them separately. Left as it was, it argued for merging two packages every time somebody
read it.

**Only `NotFoundException` of the four categories exists today.** `ConflictException`,
`ValidationException` and `ExternalServiceException` are specified in §2 and will be created
by the first feature that throws one — a category with no thrower is an abstraction with no
payer, which Principle X forbids. The specification is not the backlog.

- **Root and categories** are shared infrastructure → `common/error`.
- **Concrete exceptions live with the code that throws them.** A feature package that
  can be read without jumping elsewhere is the point of package-by-feature; a central
  `exception/` package undoes it.
- **The handler is a web component**, not an exception — hence `common/error/web`.

### §8 When to create a new exception type

> **Create a new type when a caller or the handler would treat it differently.
> Otherwise reuse the category and give it a distinct `ErrorCode`.**

That rule needs one clarification, because read alone it collides with §3 and §7.

`ProductNotFoundException` and `CategoryNotFoundException` both become a 404 and nobody
catches either — which looks like a case for one shared type. But they carry **different
`ErrorCode`s**, and §3 requires a typed constructor with the message pattern owned by the
class that raises it. Collapse them and the call site becomes
`new NotFoundException(PRODUCT_NOT_FOUND, "Product %d not found".formatted(id))` —
untyped, with the pattern now homeless; put static factories on the category instead and
catalog knowledge moves into `common/error`, which §7 forbids.

So: **one concrete class per resource kind is correct.** What the rule forbids is a class
per *message* within one resource kind, and — the real smell — a concrete class that
introduces no new `ErrorCode` and is handled identically to its sibling.
`SelectionExpiredException` is a third case: it maps to 410 and the frontend shows a
different screen, so it earns a class on the strength of **dispatch**, not merely of a code.

> Amended 2026-08-05. The original text named those two classes as an example of what
> *not* to create, which contradicted §3 and §7. Worked through in
> `specs/LM-10/research.md` R8 when the first feature hit the seam.

**Built-in exceptions.** Never throw `RuntimeException`, `IllegalStateException`,
`IllegalArgumentException` or `Exception` from service or domain code — they carry no
code, no category and no contract. `IllegalArgumentException` and `NullPointerException`
from `Objects.requireNonNull` are acceptable for **programming errors**: contract
violations by our own code, which should never reach a client. They land in the catch-all
as 500, which is correct — they *are* bugs.

**Never let a library's checked exception cross a layer boundary.** Wrap it where it is
raised — `catch (MessagingException e) { throw new EmailDeliveryException(recipient, e); }`
— and keep the cause. A JavaMail type in an HTTP signature two layers from where it was
raised is the failure this prevents. Preserve the cause always: a wrapped exception that
drops it destroys the only evidence of what actually failed.

### §9 Logging

| What | Level | Stack trace |
|---|---|---|
| 4xx from a domain exception | `WARN` | no — it is an expected outcome, and the stack is noise |
| 5xx / catch-all | `ERROR` | yes — full stack |
| Anything at all | — | never a password, token, JWT or a phone number from an enquiry |

Log in the handler, once. A service that logs and rethrows produces the same failure
three times in the log at three levels of the stack.

`traceId` goes into the response **and** into the log line. That pair is the only way a
client report ("I got an error at 14:32") becomes a searchable log query. Reuse the
`X-Correlation-Id` already in the MDC.

### §10 Security rules for error responses

These are not style preferences.

- **Authentication failure is 401, not 403.** 403 means *authenticated, not permitted*;
  401 means *not authenticated*. A client cannot implement a login flow against the wrong
  one.
- **Never distinguish "no such user" from "wrong password".** Throwing a not-found
  exception from `loadUserByUsername` renders as a 404 with the email echoed back — a
  working account enumeration oracle on an unauthenticated endpoint. One generic message,
  one status, for both cases.
- **Never echo an unvalidated input back into `detail`.** For the admin panel this matters
  more than for the public catalogue.
- **The catch-all never includes `ex.getMessage()`.** A `SQLException` message contains
  schema names; a `RestClient` message can contain a full URL with credentials. The
  response gets a fixed sentence and the `traceId`; the detail goes to the log.

### §11 Testing

- Every category → status mapping is covered by one `@WebMvcTest` slice test asserting the
  status **and** the `code` field. The `code` is a contract; an untested contract is a wish.
- One test asserts the catch-all: an unexpected exception yields 500, a `traceId`, and
  **no** internal text in the body.
- One test asserts that a malformed JSON body comes back in the same `problem+json` shape
  as a domain error. That is the regression test for the gap in §6.

### The eleven rules, in one list

1. Domain throws; the web layer decides the status. No `org.springframework.web` in
   exception classes.
2. `BaseException` → category → concrete. Three levels, never four.
3. Typed constructor parameters. Never `Object...`.
4. The message pattern is a private constant of its own class.
5. Every exception carries an `ErrorCode`. The code is the contract; the message is not.
6. One `@RestControllerAdvice`, extending `ResponseEntityExceptionHandler`.
7. RFC 9457 `ProblemDetail` for every error, including Spring's own.
8. A catch-all exists and leaks nothing.
9. One concrete type per resource kind and per distinct `ErrorCode` — never one per
   message, and never one handled identically to its sibling.
10. No library exception crosses a layer boundary; wrap it, keep the cause.
11. 4xx → `WARN` without stack, 5xx → `ERROR` with stack, `traceId` in both the response
    and the log.

References: [Error Responses — Spring Framework 7](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
· [`ProblemDetail` javadoc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/ProblemDetail.html)
· [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457.html)

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
