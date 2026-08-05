# Error handling convention — LM-BE

Source of truth for how errors are raised, translated and reported.
Derived from the reference implementation in `cookandlaughbe`, with the problems
found in it corrected. Where this document and that project disagree, this
document wins.

Owner: Андрій. Claude writes backend code against this file; the developer reviews it.
The document is normative for both — it is not a description of what the code happens
to do, and code that diverges from it is the thing that changes.

---

## 1. The shape of the thing

```
domain / service layer          web layer                    client
─────────────────────────       ──────────────────────       ──────────
throws BaseException     ──►    one @RestControllerAdvice ──► application/problem+json
(no HTTP types at all)          translates to ProblemDetail
```

Three rules follow from that picture, and everything else in this document is a
detail of one of them.

1. **The domain describes what went wrong. It never describes the HTTP response.**
2. **Exactly one place turns a domain error into a transport response.**
3. **Nothing reaches the client that was not deliberately put there.**

The translation step is an **Adapter** at the web boundary — the same role the
controller plays for the happy path. Keeping it there is what lets the service
layer stay HTTP-free, which is already a standing project convention (it also
keeps the door open for the deferred MCP server, which would need the same
services with a completely different transport).

---

## 2. Hierarchy

Three levels. Never four.

| Level | Example | Purpose |
|---|---|---|
| **Root** | `BaseException` | one type to catch, carries the error code |
| **Category** | `NotFoundException`, `ConflictException` | what the handler dispatches on |
| **Concrete** | `ProductNotFoundException` | what the caller reads and what carries the data |

The category level exists for exactly one reason: `@ExceptionHandler(NotFoundException.class)`
catches every subtype. That is polymorphic dispatch doing real work — it is why
the abstraction earns its place, and it is also why the categories must stay few.

### Categories

| Category | Meaning | Status the handler assigns |
|---|---|---|
| `NotFoundException` | referenced thing does not exist | 404 |
| `ConflictException` | state forbids the operation (duplicate, already used, wrong state) | 409 |
| `ValidationException` | input is well-formed but semantically wrong (business rule) | 422 |
| `ExternalServiceException` | a dependency we do not control failed | 502 / 503 |

Syntactic validation (`@NotNull`, `@Size`) is **not** in this list — Bean
Validation raises `MethodArgumentNotValidException` at the controller boundary
and that is handled separately, as 400.

`AlreadyExistsException` from the reference project collapses into
`ConflictException`: "already exists" and "already used" and "wrong state" all
mean the same thing to a client, and one category with a precise `code` beats
three categories with vague ones.

---

## 3. Constructors are typed. No `Object...`.

This is the single most important correction to the reference implementation.

```java
// WRONG — the reference project
public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException(Object... formatArgs) {
        super(USER_NOT_FOUND_PATTERN, formatArgs);
    }
}
```

`new UserNotFoundException()` compiles. At runtime `"...'%s'...".formatted()`
throws `MissingFormatArgumentException` — **an exception thrown while
constructing an exception**, on the error path, where it is least likely to be
covered by a test. The compiler had the information to stop this and was told
not to use it.

The same smell shows up as `super(ACTIVATION_CODE_EXPIRED_MESSAGE, new Object())`
— a dummy argument invented to satisfy a signature that should not have needed
one.

```java
// RIGHT — reference shape
public final class ProductNotFoundException extends NotFoundException {

    private static final String MESSAGE = "Product %d not found";

    public ProductNotFoundException(long productId) {
        super(ErrorCode.PRODUCT_NOT_FOUND, MESSAGE.formatted(productId));
    }
}
```

- Constructor parameters are **typed and named**. The signature documents what
  the error is about.
- The message pattern is a **private constant of the class that owns it**.
- Concrete exceptions are `final`. Subclass a concrete exception and the
  hierarchy is four deep, which nobody can hold in their head.

### Why the global `ExceptionMessages` class goes away

It looks like consistency and behaves like a shared mutable surface: every new
exception in any feature edits one file, unrelated features conflict in it, and
nothing prevents a class from picking a constant that belongs to something else.
The pattern belongs next to its only user. That is cohesion.

If per-locale error text is ever needed, the mechanism is `MessageSource` and a
resource bundle keyed on `ErrorCode` — which the framework already resolves for
`ProblemDetail` — not a constants class.

---

## 4. `ErrorCode` — the part that makes custom exceptions worth having

```java
public enum ErrorCode {
    PRODUCT_NOT_FOUND,
    SELECTION_EXPIRED,
    SKU_ALREADY_TAKEN,
    // ...
    INTERNAL_ERROR
}
```

A stable identifier the frontend can branch on. Message text is developer-facing
and may be reworded at any time; `code` is part of the API contract and is not.

This is what a custom exception buys over `IllegalArgumentException`: not a nicer
message, a **contract**. If a new exception type would not produce a new `code`
and would not be handled differently, it should not exist — see §8.

`ErrorCode` deliberately does **not** carry an `HttpStatus`. Status is a
transport concern and lives in the handler; putting it here would drag
`org.springframework.http` back into the domain through the front door after
§5 pushed it out the back.

---

## 5. `@ResponseStatus` on exceptions: removed

The reference project states the status in three places — `@ResponseStatus` on
the category, `@ResponseStatus` on the handler method, and `NOT_FOUND.value()`
inside the response body. Three copies of one fact drift; the first one to be
edited alone wins silently.

Beyond the duplication, `@ResponseStatus` lives in
`org.springframework.web.bind.annotation`. Putting it on an exception that
services throw means the service layer imports Spring Web — the exact coupling
this convention exists to prevent.

**Status is assigned in the handler and nowhere else.**

The same reasoning rules out implementing Spring's `ErrorResponse` interface on
`BaseException`, even though it is otherwise a tidy fit: it puts `HttpStatusCode`
and `ProblemDetail` in the domain.

---

## 6. The wire format is RFC 9457, not a bespoke record

Spring Framework 7 supports **Problem Details for HTTP APIs (RFC 9457)** natively
through `ProblemDetail`, and Jackson serialises it as `application/problem+json`.
Use it. A hand-rolled `ExceptionResponse` record is one more thing to document,
version and explain, in exchange for nothing.

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

### The handler extends `ResponseEntityExceptionHandler`

The reference handler covers five exception types. Everything else — malformed
JSON, wrong method, unknown path, unsupported media type — falls through to
Spring's default error page and comes back in a different shape than every
deliberate error in the API. A client cannot parse that generically.

`ResponseEntityExceptionHandler` already handles all built-in Spring MVC
exceptions and renders them as `ProblemDetail`. Extend it, annotate with
`@RestControllerAdvice`, and add methods for the four domain categories plus a
catch-all. One shape for every error the API can produce.

> Alternative: `spring.mvc.problemdetails.enabled=true` autoconfigures such a
> handler at order 0. Extending the class directly is more explicit and avoids
> ordering questions — prefer it.

Handler skeleton (shape only — the bodies are the implementation's business):

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
production — an exception nobody predicted — is the one case with no defined
behaviour.

### Validation errors use the same envelope

`MethodArgumentNotValidException` returns a `ProblemDetail` with status 400 and
an added `errors` property (field → message). Not a bare `Map`, and not a method
whose return type is `Object`.

### Timestamps

If a timestamp is added at all, it is `Instant`. `LocalDateTime` in an API
response carries no zone and is ambiguous the moment anything crosses a machine
boundary.

---

## 7. Package layout

Backend conventions say **package by feature**. Errors follow the same rule.

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
│   ├── ProductService.java
│   └── ProductNotFoundException.java
└── selection
    └── SelectionExpiredException.java
```

- **Root and categories** are shared infrastructure → `common/error`.
- **Concrete exceptions live with the code that throws them.** A feature package
  that can be read without jumping elsewhere is the point of package-by-feature;
  a central `exception/` package undoes it.
- **The handler is a web component**, not an exception. It goes under `web`,
  which is why `common/error/web` and not `exception/handler`.

Two smaller notes on the reference layout:

- `not_found`, `already_exists` — Java package names are lowercase with **no
  underscores** (JLS §6.1 convention, and every static analyser flags it).
  If such packages were kept they would be `notfound`, `alreadyexists`.
- `misc` is a junk drawer. Once a package is named `misc`, whatever does not
  obviously belong somewhere ends up in it, and the name stops carrying
  information.

---

## 8. When to create a new exception type

The instruction "prefer custom exceptions" is right and has a failure mode:
eighty exception classes, one per message, each used once.

> **Create a new type when a caller or the handler would treat it differently.
> Otherwise reuse the category and give it a distinct `ErrorCode`.**

That rule needs one clarification, because read alone it collides with §3 and §7.

`ProductNotFoundException` and `CategoryNotFoundException` both become a 404 and
nobody catches either — which looks like a case for one shared type. But they carry
**different `ErrorCode`s**, and §3 requires a typed constructor with the message
pattern owned by the class that raises it. Collapse them and the call site becomes
`new NotFoundException(PRODUCT_NOT_FOUND, "Product %d not found".formatted(id))` —
untyped, with the pattern now homeless; put static factories on the category instead
and catalog knowledge moves into `common/error`, which §7 forbids.

So: **one concrete class per resource kind is correct.** What the rule forbids is a
class per *message* within one resource kind, and — the real smell — a concrete class
that introduces no new `ErrorCode` and is handled identically to its sibling. That one
should not exist.

`SelectionExpiredException` is a third case: it maps to 410 and the frontend shows a
different screen, so it earns a class on the strength of **dispatch**, not merely of a
code.

> Amended 2026-08-05. The original text named these two classes as an example of what
> *not* to create, which contradicted §3 and §7. Worked through in
> `specs/LM-10/research.md` R8 when the first feature hit the seam.

### Built-in exceptions

- **Never throw** `RuntimeException`, `IllegalStateException`,
  `IllegalArgumentException` or `Exception` from service or domain code. They
  carry no code, no category and no contract.
- `IllegalArgumentException` and `NullPointerException` from
  `Objects.requireNonNull` are acceptable for **programming errors** —
  contract violations by another part of our own code, which should never reach a
  client. They land in the catch-all as 500. That is correct: they *are* bugs.
- **Never let a library's checked exception cross a layer boundary.** The
  reference project has `throws MessagingException` on the service *and* on the
  controller method — a JavaMail type in the HTTP signature, two layers from
  where it was raised. Wrap it where it is raised:
  `catch (MessagingException e) { throw new EmailDeliveryException(recipient, e); }`
  and keep the cause. This is the real content of "minimise built-in exceptions".

Preserve the cause. Always. `super(message, cause)` — a wrapped exception that
drops its cause destroys the only evidence of what actually failed.

---

## 9. Logging

| What | Level | Stack trace |
|---|---|---|
| 4xx from a domain exception | `WARN` | no — it is an expected outcome, and the stack is noise |
| 5xx / catch-all | `ERROR` | yes — full stack |
| Anything at all | — | never a password, token, JWT or a phone number from an enquiry |

Log in the handler, once. A service that logs and rethrows produces the same
failure three times in the log at three levels of the stack.

`traceId` goes into the response **and** into the log line. That pair is the only
way a client report ("I got an error at 14:32") becomes a searchable log query.
Reuse the `X-Correlation-Id` already in the MDC.

---

## 10. Security rules for error responses

These are not style preferences.

- **Authentication failure is 401, not 403.** The reference project returns 403
  for `BadCredentialsException`. 403 means *authenticated, not permitted*; 401
  means *not authenticated*. A client cannot implement a login flow against the
  wrong one.
- **Never distinguish "no such user" from "wrong password".** The reference
  project throws `UserNotFoundException` from `loadUserByUsername`, which the
  global handler renders as 404 with the email echoed back — a working account
  enumeration oracle on an unauthenticated endpoint. One generic message, one
  status, for both cases.
- **Never echo an unvalidated input back into `detail`.** For the admin panel
  this matters more than for the public catalogue.
- **The catch-all never includes `ex.getMessage()`.** A `SQLException` message
  contains schema names; a Feign or `RestClient` message can contain a full URL
  with credentials. The response gets a fixed sentence and the `traceId`; the
  detail goes to the log.

---

## 11. Testing

- Every category → status mapping is covered by one `@WebMvcTest` slice test that
  asserts the status **and** the `code` field. The `code` is a contract; an
  untested contract is a wish.
- One test asserts the catch-all: an unexpected exception yields 500, a
  `traceId`, and **no** internal text in the body.
- One test asserts that a malformed JSON body comes back in the same
  `problem+json` shape as a domain error. That is the regression test for the
  gap in §6.

---

## 12. What the reference project got right

Worth stating, because most of the structure survives:

- Centralised handling in `@RestControllerAdvice`, no `try/catch` in controllers.
- A single root exception type.
- An intermediate category level used for polymorphic handler dispatch — this is
  the good idea the rest of the document is built on.
- Unchecked exceptions throughout, so business signatures stay clean.
- A response object rather than an ad-hoc string body.

The corrections are about type safety (§3), coupling (§5), completeness (§6) and
information leakage (§10).

---

## Summary — the eleven rules

1. Domain throws; the web layer decides the status. No `org.springframework.web`
   in exception classes.
2. `BaseException` → category → concrete. Three levels, never four.
3. Typed constructor parameters. Never `Object...`.
4. The message pattern is a private constant of its own class.
5. Every exception carries an `ErrorCode`. The code is the contract; the message
   is not.
6. One `@RestControllerAdvice`, extending `ResponseEntityExceptionHandler`.
7. RFC 9457 `ProblemDetail` for every error, including Spring's own.
8. A catch-all exists and leaks nothing.
9. One concrete type per resource kind and per distinct `ErrorCode` — never one per
   message, and never one that is handled identically to its sibling.
10. No library exception crosses a layer boundary; wrap it, keep the cause.
11. 4xx → `WARN` without stack, 5xx → `ERROR` with stack, `traceId` in both the
    response and the log.

---

## References

- [Error Responses — Spring Framework 7 reference](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
- [`ProblemDetail` javadoc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/ProblemDetail.html)
- [RFC 9457 — Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457.html)
