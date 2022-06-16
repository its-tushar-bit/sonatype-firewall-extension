# ADR 6. Independent database migration process

Date: 2022-06-17

## Status

Proposed

## Context

As part of [IQ server resiliency initiative](https://issues.sonatype.org/browse/CLM-15105) we are in the process of making the IQ server process to be more stateless (for more details see [arch-0003](0003-iq-server-clustering.md) ) with the objective of enabling clustered deployment.
One of the key issues we have is the IQ data migration process which is currently attached to the server start-up which can cause problems in a cluster deployment due to possible concurrency.

For resolving this we have done a discovery (see [CLM-21668](https://issues.sonatype.org/browse/CLM-21668) ) of the recommended patterns that IQ data migration could adopt and the following proposal is based on it.

## Decisions

- We will separate the database migration process from server startup.
- The database migration can still be triggered at server startup, but it would only be triggered based on a configuration property.
- The configuration property to trigger database migrations at server startup will be enabled by default which means no changes to the existing customers/deployments.
- The property is expected to be disabled in a clustered deployment setup. So the database migration to a new version needs to be triggered separately.  
- For safety, we will introduce a version mapping between the IQ binary and the database version required. The IQ server will not start unless the database is at least the expected minimum version.
- Clustered environments are expected to run a 2 step upgrade process, firstly upgrading the database, and secondly upgrading the server binaries.
- For the initial resilient IQ architecture we will not consider live (no downtime) upgrades. The clustered environments are expected to have a downtime for database upgrades (i.e. database change scripts are not expected to be backward compatible). 

## Consequences

- Safer way to upgrade cluster environments with multiple IQ nodes.

___
References:

- _[blue-green deployment architecture](https://martinfowler.com/bliki/BlueGreenDeployment.html)_
- _[database refactoring patterns](https://databaserefactoring.com/)_   
