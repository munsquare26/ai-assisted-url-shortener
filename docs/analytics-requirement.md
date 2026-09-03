# Analytics Requirement

## Original Requirement

> Track analytics for shortened URLs.

## Ambiguities Identified

Before implementation, I identified several questions that are not answered by the requirement:

- What counts as a click?
- Should requests for missing URLs count?
- Should requests for expired URLs count?
- Does analytics need to be updated before the redirect response is returned?
- Does the click count need to be immediately consistent?
- What should happen to redirects if the analytics system is unavailable?

## Engineering Decisions

For this prototype:

1. A click is counted only when a valid short code successfully resolves to an original URL.
2. Missing URLs do not count as clicks.
3. Expired URLs do not count as clicks.
4. Analytics processing should not be part of the database transaction used for the core URL lookup.
5. Redirect latency should not depend on analytics database processing.
6. Click analytics may be eventually consistent.
7. Kafka will be used to publish successful redirect events asynchronously.
8. A Kafka consumer will update `click_count` and `last_accessed_at`.

## Intended Flow

Successful redirect:

Client -> Redirect API -> URL lookup -> Kafka event -> HTTP 302

Then asynchronously:

Kafka -> Analytics consumer -> PostgreSQL

## Tradeoff

This design favors redirect availability and latency over immediately consistent analytics.

A user may therefore successfully follow a shortened URL before its updated click count is visible in the database.

For this system, I consider that acceptable because redirect behavior is part of the core product path, while analytics is secondary.