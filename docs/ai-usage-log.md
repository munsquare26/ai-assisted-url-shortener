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

## How I am using AI in this project

I am following a few rules throughout the implementation:

1. AI-generated code is treated as a suggestion, not automatically accepted code.
2. I review changes before keeping them.
3. Generated code must compile and pass the same tests as manually written code.
4. Important flows are also tested through the running API.
5. I record meaningful failures or changes instead of only documenting successful AI suggestions.
6. I do not provide production credentials, secrets, tokens, or sensitive data to AI tools.
7. Architecture and tradeoff decisions remain my responsibility.

The goal is not to have AI build the application autonomously. I am using AI to speed up parts of the engineering workflow while keeping review, validation, and final decisions under engineer control.

I discussed the assignment requirements along with the role requirements and used AI to help narrow down the stack and development approach.

The initial recommendation was Spring Boot with PostgreSQL/JPA for persistence and Kafka for analytics.

**My decision**

I went with:

- Java 17
- Spring Boot
- Maven
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway
- Kafka for analytics
- Docker for local infrastructure

I decided not to introduce Kafka immediately. I want to get the basic create/redirect flow working first and then add asynchronous analytics. This keeps the first implementation small and gives me a working vertical slice before adding another dependency.

---

## 2. Local PostgreSQL setup

**What I was working on**

The application failed to start after adding Spring Data JPA and the PostgreSQL driver because there was no configured datasource.

**How I used AI**

I used AI to troubleshoot the startup error and set up PostgreSQL locally using Docker.

There was also an issue starting the local Docker daemon through Colima. I used AI to interpret the error and fix the local environment before continuing with application development.

**My decision**

I kept PostgreSQL rather than switching to an embedded database such as H2 just to make the application start.

Since PostgreSQL is the database I intend to use for the application, I preferred to develop against it from the beginning.

**Validation**

I verified that the PostgreSQL container was running and then started the Spring Boot application successfully.

---

## 3. Database schema management

**What I was working on**

I needed a repeatable way to create and evolve the database schema.

**How I used AI**

AI suggested using Flyway migrations rather than allowing Hibernate to automatically create or update the database schema.

**My decision**

I used Flyway and configured Hibernate with schema validation.

The first migration creates the `short_urls` table and the index used for short-code lookups.

I also disabled Open Session in View because I don't want database access happening implicitly outside the intended service/repository flow.

**Validation**

Spring Boot successfully started with:

- Flyway migration applied
- Hibernate schema validation enabled
- JPA EntityManagerFactory initialized

---

## 4. ShortUrl JPA entity

**What I was working on**

I created the first JPA entity corresponding to the `short_urls` table.

**How I used AI**

AI provided an initial version of the entity and its JPA mappings.

The generated code initially used the package:

`com.example.urlshortener.entity`

while my Spring Boot project uses:

`com.example.url_shortener.entity`

This caused the Spring context test to fail because Hibernate could not instantiate the entity.

**My decision**

I did not work around the failing test or remove it. I traced the nested exception, found the package mismatch, corrected the entity package, cleaned the Maven build, and ran the tests again.

**Validation**

Command:

`./mvnw clean test`

Result:

`Tests run: 1, Failures: 0, Errors: 0`

`BUILD SUCCESS`

This was also a useful reminder that AI-generated code still needs to be treated like any other code change: compile it, test it, understand failures, and correct it before moving forward.