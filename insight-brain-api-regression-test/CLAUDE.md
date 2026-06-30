<!--
  Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
  Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
  "Sonatype" is a trademark of Sonatype, Inc.
-->

# API Regression Test Module — Developer Guide

REST API regression tests for **Nexus IQ Server (insight-brain)**.

These tests boot the embedded IQ Server in-process (via `AbstractResourceTest` from
`insight-brain-service`'s test-jar) and exercise REST endpoints directly — **no browser,
no Playwright**. UI scenarios live in `insight-brain-playwright-test`.

---

## Scope

Per code-review direction (PR #16455):

- **Default coverage:** public `/api/v2/` endpoints.
- **Exception:** a small set of `/rest/` endpoints that external integration clients depend on
  (e.g. the SCA Integrations team's client). Adding `/rest/` coverage requires explicit
  justification and lives under a sibling `…api.rest` package.

Today the suite covers `api/v2/policyWaivers`.

---

## Module layout

```
insight-brain-api-regression-test/
├── pom.xml                                # Failsafe + api-regression profile
├── CLAUDE.md                              # this file
└── src/test/
    ├── java/com/sonatype/clm/testing/api/
    │   ├── AbstractIqApiTest.java         # Base class: HTTP helpers, unique-data helpers
    │   ├── categories/
    │   │   └── ApiRegressionTest.java     # JUnit @Category marker
    │   └── v2/                            # /api/v2/* tests (the default surface)
    │       └── PolicyWaiversApiRegressionTest.java
    └── resources/
        └── config-test.yml                # Dropwizard config for the embedded server
                                           # (also drives logback via its `logging:` block)
```

---

## Running tests

```bash
mvn verify -pl insight-brain-api-regression-test -Papi-regression
```

Single class / method via the usual `-Dit.test=ClassName[#methodName]` Failsafe flag.

---

## Pipeline integration

**Opt-in only.** Not wired into `Jenkinsfile.build`, `Jenkinsfile.main`, or
`Jenkinsfile.feature` today. CI does not run this module on PR or main builds.
Pipeline integration is tracked separately.

---

## Authoring new API tests

1. Place the test by API surface: `/api/v2/*` → `com.sonatype.clm.testing.api.v2`.
2. Extend `AbstractIqApiTest`. Annotate the class with `@Category(ApiRegressionTest.class)`.
3. Use the HTTP helpers (`apiGet`, `apiPostJson`, `apiPutJson`, `apiDelete`, `anonApiGet`) —
   they emit per-request breadcrumbs into the per-class Failsafe report. Use the raw
   `apiRequest()` / `anonApiRequest()` builders only for multipart uploads or custom
   headers (those bypass the breadcrumb logging).
4. Use **unique-data helpers** for any seeded entity: `uniqueId("prefix")` for kebab-style
   ids (publicIds, slugs) and `uniqueName("prefix")` for display names. Hardcoded names
   collide across reused forks.
5. **Assert status before parsing the body.** `assertResponseStatus(200, response)` before
   `assertThatJson(response.getBodyText())` — otherwise a 4xx error body looks like
   malformed JSON.
6. Use `assertThatJson(...)` from `net.javacrumbs.json-unit` for response bodies, not raw
   `JsonNode` walking. Prefer `node(...)` / `inPath(...)` paths over manual field access.
7. **Bounds-check arrays.** Before `node("[0].field")` assert `.isArray().hasSize(N)` so
   off-by-one regressions surface as clear assertion failures rather than IOOBE.
8. **Avoid asserting on full server error strings.** They are an implementation detail;
   the contract is the status code. If you need a body check, match a short fragment
   (`containsIgnoringCase("already exists")`) or a production constant value, not the full
   sentence.
9. Use `tempEntity` (inherited from `AbstractResourceTest`) for database cleanup. Every
   `tempEntity.newXxx(...)` is auto-removed after the test.

---

## Module-specific rules

- **No per-test `new ObjectMapper()`** — pass the DTO directly to `apiPostJson` /
  `apiPutJson`; the helpers serialize through the underlying `HttpRequest` builder.
  Hand-rolling JSON strings or mappers in test classes is a code smell.
- **Server lifecycle:** `TestCLMServer` is static; under `reuseForks=true` it is shared
  across API test classes in a fork (booted once per fork, not per class). Per-test data
  isolation is provided by `tempEntity`.
