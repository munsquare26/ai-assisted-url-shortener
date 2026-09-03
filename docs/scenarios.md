# Engineering Scenarios

This project demonstrates three types of engineering work: greenfield development, a brownfield change, and an ambiguous requirement.

The goal was not only to implement each requirement, but also to show how AI assistance was used while keeping engineering decisions, validation, and final ownership with the developer.

---

## Scenario 1 — Greenfield Development

### Requirement

Build the initial URL shortening capability.

A client should be able to submit an original URL and receive a shortened URL that can later redirect back to the original destination.

### Initial Breakdown

I divided the requirement into a small set of components:

1. REST endpoint for creating a short URL
2. URL validation
3. short-code generation
4. persistence in PostgreSQL
5. redirect endpoint
6. error handling
7. automated tests

This kept the first implementation focused on the core URL-shortening behavior before introducing additional features.

### Implementation

The create endpoint is:

```text
POST /api/v1/urls
```

Example request:

```json
{
  "url": "https://www.google.com"
}
```

The service validates the URL, generates a seven-character short code, persists the mapping, and returns a usable public URL.

Example response:

```json
{
  "shortCode": "dMIPVD8",
  "shortUrl": "http://localhost:8080/dMIPVD8",
  "originalUrl": "https://www.google.com"
}
```

The public redirect endpoint is:

```text
GET /{shortCode}
```

For example:

```text
GET /dMIPVD8
```

returns:

```text
HTTP 302
Location: https://www.google.com
```

### Design Decisions

#### PostgreSQL as the source of truth

URL mappings are persisted rather than stored in application memory.

This makes mappings survive application restarts and provides database-level integrity guarantees.

#### Random short-code generation

Short codes are generated using `SecureRandom` and a Base62-style character set.

The application checks for an existing code before saving.

A unique database constraint on `short_code` provides the final integrity guarantee.

#### Validation before persistence

Only `http` and `https` URLs with a host are accepted.

Validation happens before repository operations.

This ordering was reinforced by a test that initially failed because the repository was being accessed before invalid input was rejected.

#### Public redirect path

The management API remains under:

```text
/api/v1/urls
```

while the generated public short URL uses:

```text
/{shortCode}
```

This keeps shortened URLs simple and separates the public redirect behavior from API-oriented operations.

### Validation

The greenfield implementation was validated through:

* service unit tests
* controller tests
* manual API requests
* direct PostgreSQL verification
* manual redirect verification

The implementation successfully demonstrated the full flow:

```text
create URL
    ->
persist mapping
    ->
return short URL
    ->
request short URL
    ->
HTTP 302 redirect
```

### AI Assistance

AI was used to help:

* decompose the requirement
* review the initial architecture
* generate candidate implementation approaches
* identify useful test cases
* debug test failures
* review configuration choices

AI-generated suggestions were not accepted without validation.

One example was the validation-order test. The generated negative test exposed that invalid input still caused a repository interaction. The implementation was changed so validation became the first service operation.

Another example was application configuration. Moving the base URL into configuration initially broke the manually constructed service tests because field injection produced a null value. The implementation was changed to constructor injection.

These failures were useful because they resulted in improvements rather than being hidden.

### Outcome

The greenfield scenario produced a working URL-shortening service with persistence, validation, redirects, error handling, and automated tests.

---

# Scenario 2 — Brownfield Change

## Requirement

Add optional expiration to an existing shortened URL.

The important constraint was that the system already contained URL records and working behavior.

The change therefore needed to preserve existing URLs while allowing newly created URLs to optionally expire.

## Existing System

Before this change, the database already stored:

```text
short_code
original_url
created_at
click_count
last_accessed_at
```

Existing URLs had no expiration concept.

Changing the schema or application behavior could not make those records invalid.

## Design Decision

A nullable column was introduced:

```text
expires_at
```

The semantics are:

```text
expires_at = NULL
```

means:

```text
URL never expires
```

This allows existing records to retain their original behavior without requiring artificial expiration values or a destructive data migration.

## Schema Evolution

Instead of modifying the original Flyway migration, a new migration was created:

```text
V2__add_url_expiration.sql
```

The migration adds:

```sql
ALTER TABLE short_urls
ADD COLUMN expires_at TIMESTAMP WITH TIME ZONE;
```

Using a new migration preserves migration history and models how the change would be applied to an existing environment.

## API Change

The create request can optionally contain:

```json
{
  "url": "https://example.com",
  "expiresAt": "2026-09-10T12:00:00Z"
}
```

If `expiresAt` is omitted, the shortened URL continues to behave as before.

## Redirect Behavior

When resolving a short URL, the service checks the expiration timestamp.

A URL with no expiration continues normally.

A URL whose expiration time has passed returns:

```text
HTTP 410 Gone
```

rather than:

```text
HTTP 404 Not Found
```

The URL did exist, so `410 Gone` communicates the state more accurately than pretending the resource was never present.

## Backward Compatibility

The main compatibility decision was keeping `expires_at` nullable.

This means records created before the feature was introduced require no modification.

Their behavior remains:

```text
short code
    ->
lookup
    ->
no expiration
    ->
redirect
```

while expiring URLs follow:

```text
short code
    ->
lookup
    ->
expiration check
    ->
redirect OR 410
```

## Validation

The change was tested at both the service and HTTP levels.

Tests cover:

* creating a URL with an expiration value
* resolving a non-expired URL
* rejecting an expired URL
* returning HTTP `410 Gone`

The database migration and expired URL behavior were also tested manually against PostgreSQL and the running application.

## AI Assistance

AI was used to explore the implications of adding expiration to an already working schema.

The main questions reviewed were:

* whether the column should be nullable
* how existing records should behave
* whether the original migration should be modified
* what HTTP status should represent an expired URL
* which regression tests were needed

The final decision was to use a new Flyway migration, preserve existing records with a nullable expiration field, and return `410 Gone` for expired URLs.

## Outcome

The feature was added without breaking the original URL creation and redirect behavior.

This scenario demonstrates schema evolution and backward-compatible modification of an existing implementation rather than treating every requirement as greenfield development.

---

# Scenario 3 — Ambiguous Requirement

## Original Requirement

> Track analytics for shortened URLs.

This requirement was intentionally treated as ambiguous rather than immediately translated into code.

## Questions Identified

Before implementation, several questions needed answers:

* What counts as a click?
* Should requests for unknown short codes count?
* Should expired URL requests count?
* Should analytics be updated before returning the redirect?
* Does the click count need to be immediately consistent?
* What happens if analytics processing is unavailable?
* Should analytics processing increase redirect latency?

These questions were documented before implementing the feature.

## Decisions

For this prototype, the following semantics were selected.

### Successful redirects count as clicks

A click is recorded only after the application successfully resolves an existing, non-expired short URL.

### Unknown URLs do not count

Requests for nonexistent short codes return `404` and do not publish analytics events.

### Expired URLs do not count

Requests for expired URLs return `410` and do not publish analytics events.

### Analytics can be eventually consistent

The redirect response does not require the analytics database update to complete first.

A small delay between a redirect and the updated statistics is acceptable.

### Kafka separates redirect and analytics processing

A successful resolution produces a click event.

The flow is:

```text
Client
  |
  v
Redirect API
  |
  v
URL lookup
  |
  +----> Kafka click event
  |
  v
HTTP 302
```

The analytics update happens through:

```text
Kafka
  |
  v
Analytics Consumer
  |
  v
PostgreSQL
```

## Event Model

The click event contains:

```text
shortCode
clickedAt
```

This provides enough information for the analytics consumer to identify the shortened URL and record when the click occurred.

## Stored Analytics

The prototype maintains:

```text
click_count
last_accessed_at
```

These values can be retrieved using:

```text
GET /api/v1/urls/{shortCode}/stats
```

Example:

```json
{
  "shortCode": "dMIPVD8",
  "originalUrl": "https://www.google.com",
  "clickCount": 3,
  "createdAt": "2026-09-02T22:34:06.177955Z",
  "lastAccessedAt": "2026-09-03T05:07:35.338241Z",
  "expiresAt": null
}
```

## Validation

The analytics implementation was validated in several ways.

Service tests verify that a successful redirect publishes a click event.

Expired URLs are rejected before analytics publishing.

The complete integration was also tested manually:

```text
HTTP redirect
    ->
Kafka producer
    ->
Kafka broker
    ->
Kafka consumer
    ->
PostgreSQL update
```

After requesting a short URL, PostgreSQL showed an incremented `click_count` and an updated `last_accessed_at`.

The statistics endpoint then returned the updated analytics through the API.

## Integration Failure Found

The first Kafka configuration did not successfully start the application.

The consumer failed during application startup because the configured JSON serializer/deserializer did not match the Jackson version used by the Spring Boot 4 / Spring Kafka 4 dependency stack.

The runtime error was investigated and the serializer configuration was corrected.

The application was then restarted and the complete Kafka flow was verified again.

This was an important validation step because the issue was not exposed by the existing unit tests.

## Tradeoff

Asynchronous analytics improves separation between redirect processing and analytics updates, but introduces eventual consistency and additional operational complexity.

The prototype does not attempt to provide exactly-once analytics.

Possible production improvements include:

* Kafka retry policies
* dead-letter topics
* idempotent event processing
* atomic database counter updates
* explicit topic configuration
* improved behavior when Kafka is unavailable
* an outbox pattern for stronger event publication guarantees

These were intentionally left as documented improvements rather than adding complexity that was not required for the prototype.

## AI Assistance

AI was used primarily to help turn the vague analytics requirement into explicit engineering questions and compare implementation approaches.

The final semantics were chosen by the developer before implementation.

AI was also used during Kafka integration debugging when runtime validation exposed an incompatible serializer configuration.

The implementation was accepted only after the complete producer-to-consumer-to-database flow was manually verified.

## Outcome

The ambiguous requirement was converted into a concrete, testable definition:

> A click is a successful redirect of an existing, non-expired short URL. Click events are processed asynchronously through Kafka, and analytics are eventually consistent.

This definition gave the implementation a clear behavioral contract while making the tradeoffs visible to reviewers.

---

# Summary

The three scenarios exercise different types of engineering work:

| Scenario   | Engineering Focus                         | Result                                  |
| ---------- | ----------------------------------------- | --------------------------------------- |
| Greenfield | Build a new capability from a requirement | URL creation, persistence and redirect  |
| Brownfield | Modify an existing system safely          | Backward-compatible optional expiration |
| Ambiguous  | Clarify behavior before implementation    | Kafka-based asynchronous analytics      |

Across all three scenarios, AI was treated as an engineering assistant rather than an autonomous decision-maker.

AI helped with decomposition, implementation options, test generation, debugging, and review.

The developer remained responsible for:

* selecting the design
* rejecting or changing unsuitable suggestions
* running tests
* investigating failures
* validating real infrastructure behavior
* documenting tradeoffs
* approving the final implementation
