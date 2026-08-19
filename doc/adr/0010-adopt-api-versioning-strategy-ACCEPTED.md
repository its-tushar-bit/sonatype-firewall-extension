# ADR 10. Adopt API Versioning Strategy

Date: 2024-01-23

## Status

Proposed

## Context
A Repo Manager support issue was found due to the semantic behavior changes to our APIs. Changing the semantics of an API should be considered a breaking change which would require a versioning strategy to support backwards compatibility and avoid breaking integrations. Although the changes weren't intended, it started a conversation around having a strategy to deal with and manage intended breaking changes to our APIs. A set of guidelines were formed to create a common or standard way our products should present API with versioning.

A [repository](https://github.com/sonatype/restful-api-guidelines) exists that documents the guidelines for development of restful APIs. This ADR focuses on the adoption of [API compatibility](https://github.com/sonatype/restful-api-guidelines/blob/main/chapters/compatibility.adoc). To summarize the proposed adopted guidelines:
 * SHOULD avoid URL versioning
 * MUST use Sonatype-Version header for versioning
 * MUST reflect deprecation in API specifications
 * MUST obtain approval of clients before API shut down
 * MUST collect external partner consent on deprecation time span
 * MUST monitor usage of deprecated API scheduled for sunset
 * SHOULD add Deprecation and Sunset header to responses
 * SHOULD add monitoring for Deprecation and Sunset header
 * MUST not start using deprecated APIs

Along with the guidelines to support API compatibility, it's also important to define what breaking changes means. This is defined in [API Versioning](https://github.com/sonatype/restful-api-guidelines/blob/main/chapters/versioning.adoc)
Along with defining breaking changes, we will also need to document when breaking changes occur, and also increment to a new API version representing the release of breaking changes.

A trival example of how versioning could be implemented was created in [version-demo](https://github.com/sonatype/version-demo). This follows a pattern used by [Stripe](https://stripe.com/blog/api-versioning) which attempts to encapsulate breaking changes into a set of version migrations, so that the main endpoints can represent the latest API version, and not share the concern of versions where possible.

## Decision
 * Versioning strategy is adopted
 * Implemented will be determined based on prioritization and need. A need would occur when the engineering team determines an API change must happen in a non-backwards compatible way, requiring the need to start enforcing versioning.

## Consequences
 * Introducing a pattern like suggested provides a framework for handling breaking changes. Risk is introduced with every breaking change, and adds complexity to the system. Introducing a breaking change should still be a last resort after all other reasonable options have been considered. This is an important aspect that might not be immediately clear when faced with new system requirements
 * This will affect both SaaS and on prem flavors of our platform, and we should think about how integrations will understand what "version" means when switching between those contexts
 * Deprecated fields in DTO's will need to stay in the code as long as there is support for the version of the API that they were last supported in
 * Integrations will need to adopt the use of the Sonatype-Version header
 * Existing integrations without knowledge of the proposed versioning strategy will need to have sane and default behavior such that future version don't retroactively cause breakages
 * Standardizing around a common base path for all new APIs will establish a consistent URL path. The common `/api/v2` path must be used for all new API resources.

## Changelog
 * Adds clarification to use `/api/v2` as the default base path for all new resources