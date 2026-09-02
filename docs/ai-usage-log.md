# AI Usage Log

This project was built with AI assistance as part of the development workflow.

I used **ChatGPT** mainly for requirement analysis, design discussions, breaking the work into smaller implementation steps, troubleshooting issues, and reviewing engineering decisions.

I used **GitHub Copilot** as an in-editor coding assistant for code completion and implementation suggestions.

I treated suggestions from both tools as starting points rather than automatically accepting them. I reviewed the generated code, made changes where needed, and validated the implementation through compilation, automated tests, API calls, application logs, and database checks.

This log captures the meaningful AI-assisted parts of the project rather than every individual prompt or code completion.

## 1. Initial project setup and architecture

**What I was working on**

I needed to decide on a stack and an initial structure for the URL shortener.

**How I used AI**

I used ChatGPT to discuss the assignment requirements and compare possible implementation approaches.

Based on that discussion, I selected:

* Java 17
* Spring Boot
* Maven
* Spring Data JPA / Hibernate
* PostgreSQL
* Flyway
* Kafka for asynchronous analytics
* Docker for local infrastructure
* JUnit and Mockito for testing

**What I decided**

I decided to build the system incrementally instead of introducing every component at once.

The first goal was a small working vertical slice:

`REST API -> Service -> Repository -> PostgreSQL`

Kafka will be introduced later for analytics rather than making the core URL-shortening operation dependent on asynchronous infrastructure.

**AI tool:** ChatGPT
**Outcome:** Accepted with modifications

---

## 2. Local PostgreSQL setup

**What I was working on**

After adding persistence dependencies, the application needed a configured datasource.

**How I used AI**

I used ChatGPT to work through the PostgreSQL setup using Docker.

I also ran into a local Docker/Colima issue where the Docker daemon was unavailable. I used ChatGPT to interpret the errors and troubleshoot the local environment.

**What I decided**

I kept PostgreSQL rather than switching to an embedded database such as H2 just to simplify development.

Since PostgreSQL is the database intended for the application, I preferred to develop against it from the beginning.

**How I validated it**

I verified that the PostgreSQL container was running and successfully started the Spring Boot application against it.

**AI tool:** ChatGPT
**Outcome:** Accepted with local troubleshooting

---

## 3. Database schema management

**What I was working on**

I needed a repeatable way to create and evolve the application's database schema.

**How I used AI**

I used ChatGPT to discuss Flyway migrations versus allowing Hibernate to automatically create/update the database schema.

The recommendation was to manage schema changes explicitly with Flyway and configure Hibernate to validate the resulting schema.

**What I decided**

I used Flyway and configured:

`spring.jpa.hibernate.ddl-auto=validate`

I also disabled Open Session in View.

The first migration creates the `short_urls` table and the index used for short-code lookups.

**How I validated it**

I started the application and verified that Flyway applied the migration and Hibernate successfully validated the schema.

**AI tool:** ChatGPT
**Outcome:** Accepted

---

## 4. ShortUrl JPA entity

**What I was working on**

I needed the initial JPA entity corresponding to the `short_urls` table.

**How I used AI**

I used AI assistance to create the initial entity and JPA mappings.

During testing, I found that the generated code used:

`com.example.urlshortener.entity`

while my actual Spring Boot project uses:

`com.example.url_shortener.entity`

This caused the Spring context test to fail.

**What I changed**

I traced the test failure to the package mismatch and corrected the entity package rather than changing the project structure or bypassing the failing test.

**How I validated it**

I ran:

`./mvnw clean test`

The result was:

`Tests run: 1, Failures: 0, Errors: 0`

`BUILD SUCCESS`

**AI tool:** ChatGPT / coding assistant
**Outcome:** Edited after testing

**What I learned**

This was a useful example of why I don't treat generated code as automatically correct. Even relatively simple generated code still needs to be compiled, tested, and reviewed in the context of the actual repository.

---

## 5. Repository and short-code generation

**What I was working on**

I needed persistence operations for finding short URLs and a way to generate short codes.

**How I used AI**

AI assistance was used for the initial repository structure and short-code generator implementation.

The repository includes operations for finding a URL by its short code and checking whether a generated code already exists.

For short-code generation, I used a seven-character value containing uppercase letters, lowercase letters, and digits. The implementation uses `SecureRandom`.

**What I decided**

I kept the database unique constraint on `short_code` as the final protection against duplicates.

The current implementation also checks whether a generated code already exists before inserting it.

I recognize that the existence check alone does not completely prevent a race condition between checking and inserting. A production implementation should also handle a unique-constraint failure and retry generation.

**How I validated it**

I ran the Maven tests after the changes and confirmed the build passed.

**AI tools:** ChatGPT and GitHub Copilot
**Outcome:** Reviewed and accepted

---

## 6. Create-short-URL API

**What I was working on**

I wanted the first complete working flow where a client could submit a URL and receive a shortened URL.

**How I used AI**

I used ChatGPT for the API structure and implementation approach, and GitHub Copilot as an in-editor coding assistant while implementing the DTO, service, and controller code.

The resulting flow is:

`POST /api/v1/urls`

`Controller -> Service -> Repository -> PostgreSQL`

**What I decided**

I kept the first implementation intentionally small.

There are some things I identified for follow-up rather than hiding them inside the initial implementation. For example:

* the generated base URL currently uses localhost
* URL validation needs to be strengthened
* duplicate-code handling can be made more robust

These will be addressed as explicit engineering changes.

**How I validated it**

I started the application and sent a request containing:

`https://www.google.com`

The API returned:

`HTTP 201 Created`

with a generated short code.

During my development run, one generated value was:

`dMIPVD8`

I then queried PostgreSQL directly and confirmed that the record had actually been persisted with:

* the generated short code
* the expected original URL
* creation timestamp
* initial click count of zero

**AI tools:** ChatGPT and GitHub Copilot
**Outcome:** Accepted with follow-up improvements identified

---

## 7. Redirect flow

**What I was working on**

After creating shortened URLs, I needed to resolve a short code back to its original URL.

**How I used AI**

I used ChatGPT to discuss the HTTP behavior and implementation structure.

The service performs the short-code lookup, while the controller is responsible for returning the HTTP redirect.

GitHub Copilot was used as an in-editor coding assistant during implementation.

**What I decided**

For the initial implementation, a successful lookup returns:

`HTTP 302 Found`

with the original URL in the `Location` header.

**How I validated it**

I manually requested:

`GET /api/v1/urls/dMIPVD8`

and received:

`HTTP/1.1 302`

with:

`Location: https://www.google.com`

This verified the complete flow:

`create -> persist -> lookup -> redirect`

**AI tools:** ChatGPT and GitHub Copilot
**Outcome:** Reviewed and accepted

---

## 8. Missing short-code behavior

**What I was working on**

I reviewed what should happen when a client requests a short code that doesn't exist.

**How I used AI**

The initial implementation used a generic `IllegalArgumentException`.

During review with ChatGPT, we identified that a missing short URL is an expected application condition and should have an explicit domain exception and HTTP response.

The proposed change is to introduce a `ShortUrlNotFoundException` and map it to:

`HTTP 404 Not Found`

**What I decided**

I accepted the design change because it makes the API contract clearer and avoids representing an expected lookup failure as an internal application error.

**AI tool:** ChatGPT
**Outcome:** Initial approach revised

**Status:** In progress

---

## 9. Service-level testing and validation ordering

**What I was working on**

After manually validating the create and redirect flows, I wanted automated tests around the service layer so that the core behavior could be verified without running the full application manually.

**How I used AI**

I used ChatGPT to identify useful service-level test cases and generate an initial Mockito-based test structure.

The initial tests covered:

* creating a shortened URL
* resolving an existing short code
* rejecting an invalid URL
* handling a missing short code

The existing Spring context test was also still part of the Maven test suite.

**What happened**

The first two service tests passed, but the negative test for an invalid URL failed.

The test expected an invalid URL to be rejected without interacting with the repository. Mockito reported that the repository had already been called through the short-code generation flow.

This showed that URL validation was happening too late in `createShortUrl()`.

The service was attempting to generate/check a short code before validating the input URL.

**What I changed**

I moved:

`validateUrl(originalUrl)`

to the beginning of `createShortUrl()`.

This means invalid input is now rejected before short-code generation or any repository/database interaction occurs.

I kept the `verifyNoInteractions(shortUrlRepository)` assertion because it documents the intended behavior: invalid requests should fail early and should not cause unnecessary persistence operations.

**How I validated it**

I ran:

`./mvnw test`

Before the change:

`Tests run: 5, Failures: 1`

After moving validation to the beginning of the service method, all tests passed:

`Tests run: 5, Failures: 0, Errors: 0`

**AI tool:** ChatGPT
**Outcome:** AI-generated test exposed an implementation issue; implementation was edited and revalidated

**What I learned**

This was a useful example of using AI for more than generating production code. The suggested negative test exposed behavior that had passed the earlier happy-path tests and manual API checks.

The important part was not simply making the test green. I reviewed why the repository was being called and changed the service ordering so invalid input fails before unnecessary work is performed.
