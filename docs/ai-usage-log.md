# AI Usage Log

This project was built with AI assistance as part of the development workflow. I used AI mainly to discuss design options, generate initial code, troubleshoot issues, and identify test cases. I reviewed the suggestions before applying them and made changes where they didn't fit the project.

## 1. Initial project setup and architecture

**What I was working on**

I needed to decide on a stack and an initial structure for the URL shortener.

**How I used AI**

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