# ADR 9. Database Layer Cleanup/Refactor

Date: 2023-11

## Status

Proposed... but technically the work is already complete. ADRs had fallen by the wayside for a while so one was
not done before this work was completed. So effectively this ADRs is for posterity to provide context and details.

## Context

As MTIQ was being built throughout 2023 some of the engineers had growing concerns about various database related
issues. These generally fell into three buckets:

1. Migrations - from day 1 of MTIQ where it was decided that we would run a 'tenant-per-schema' approach, it was
   accepted that migrations would be a paramount concern over the long term. Essentially every Internet article
   describing the various multi-tenant database approaches note that tenant-per-schema approach makes migrations
   considerably more difficult.
2. Performance - any SaaS is concerned about performance and taking a historical on-premise monolith and running it as a
   SaaS only puts the database more under the spotlight.
3. The IQ database code itself - it was known up front that the current database layer was currently not very
   extensible because of A) extensive use of `static` variables and B) DAO classes (that is data access objects) were
   not managed by the dependency injection framework (Guice) and were usually instantiated on demand. This meant that
   any time that MTIQ needed to do database layer related work it would be cumbersome.

This ADR focuses on #3. Note that the three Confluence pages linked in [References](#references) contain much more info,
and it will only be summarized here.

The needs that the MTIQ team saw were:

- to not be constrained by H2 nor on-prem. We need to be able to make changes quickly and test/use them without thinking
  about h2/on-prem concerns.
- to have custom code for either A) MTIQ itself or B) postgres in general. The current database layer is more of a 
  'lowest common denominator' type of approach. Meaning implementations favour simplicity or the simpler database
- to move quickly with experiments and not be constrained by on-prem. So if there was a problem that could be solved in
  Postgres using an advanced feature it had, but H2 didn't support it, then the simpler approach had to be used.
- to run certain queries against the Amazon RDS DB 'reader' endpoint to distribute load. Currently ALL queries have to
  be sent to the primary database.
- to have retry logic. A long-standing topic in MTIQ disaster recovery discussions.

The specific issues in the code we noted:

- Extensive use of `static` methods and variables. The original 'data store provider' classes such
  as [OperationalDataStoreProvider](https://github.com/sonatype/insight-brain/blob/081ef95b98b8b0a9f66e482a3b3ded571a5890b8/insight-brain-db/src/main/java/com/sonatype/insight/brain/db/OperationalDataStoreProvider.java)
  were implemented entirely with `static` methods. Static by definition cannot be extended or customized in any way so
  this would be a hindrance.
- The DAO classes (those ending with `DAO` in the `insight-brain-data` module) were not managed by Guice, and
  furthermore not using any creational patterns whatsoever. Rather they were instantiated with `new` any time they were
  used. E.g. see [PolicyResource.java](https://github.com/sonatype/insight-brain/blob/081ef95b98b8b0a9f66e482a3b3ded571a5890b8/insight-brain-service/src/main/java/com/sonatype/insight/brain/policy/PolicyResource.java). This usage pattern by nature precludes any customization or extension.

These are the two primary problems driving this. There were other, smaller, issues and smells noted as part of this
effort. See the wiki links in [References](#references) to a few Confluence pages which contain more details.

## Decisions

- In Q3 it was decided to proceed with a refactor of this code to enable the needs listed above.
- The DAO classes will be managed by Guice (dependency injection)
- Problem `static` code would be refactored (primarily this was the original four `*DataStoreProvider` classes).

## Consequences

- All DAO classes will be managed by the dependency injection framework
- Opens the door for custom DAO implementations for Postgres or MTIQ
- The test code will need a significant overhaul
- Large feature branch that will be difficult to merge in at the end

___
References:

- [Initial Confluence page](https://sonatype.atlassian.net/wiki/spaces/~cpeters/pages/91390311/MTIQ+Database+Discussion)
  from back on July 18 when I first documented my concerns.
- [Confluence page](https://sonatype.atlassian.net/wiki/spaces/~cpeters/pages/126681137/Database+Layer+Cleanup#The-TemporaryEntity-class)
  where many of the points of the first document were expounded.
- [Confluence page](https://sonatype.atlassian.net/wiki/spaces/~cpeters/pages/143982847/MTIQ+DB+Layer+Problems) where we
  expounded of some the problems in the original codebase.
