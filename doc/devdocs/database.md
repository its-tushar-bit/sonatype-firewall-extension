<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
# Database

## About

This devdoc is meant to describe:

* How the database is used in the code
* The various test types use the database

## Details

- Low level core database classes live in `insight-brain-db`
- Model objects and Data Access Objects (DAOs) live in `insight-brain-data`
- 'Service' or 'Application' code lives predominantly in `insight-brain-service`

### Core Database Classes

The `insight-brain-db` module contains the lowest level database classes. The main actor here is the `DataStore`
interface and its implementations. In IQ a 'DataStore' is a fairly simple class that encapsulates the `DataSource`, the
database configuration, the schema used (MTIQ uses a schema per-tenant), and the JPA `EntityManager`. There are four
data stores: operational data store (known as ODS), aggregation, data mart, and third party scans. The primary reason
for four instead of one is simply limitations in the original H2 code surrounding database locking.

`DataStore` is an interface, as well as an interface for each of the four. Then there is an `AbstractDataStore` with
common logic and an implementation for each (prefixed with `Default`). For multi-tenant IQ there is another abstract
base class `AbstractMultiTenantDataStore` as well as an implementation for each.

See the javadoc on [DataStore.java]

### Data Access Objects

The `insight-brain-data` module contains the data access objects. These 'DAO' classes (or layer) sit between the
application/service code and the database itself. The Java Persistence API (JPA) is used under the hood. e.g. see
`persistence.xml` and look for use of the `javax.persistence` package.

There are four abstract classes for each of the data stores as well as
further abstract base classes `AbstractSqlDAO` and `AbstractDAO`. These base contain all low-level database operation
type code (e.g. CRUD), and special access methods particular to that DAO, and abstract things away like JPA and locking.


### Testing

The database is a first class test fixture. It is controlled primarily by a Junit rule `DatabaseRule` which must be
accessed as a singleton. The singleton approach is used because we want to be smart about the lifecycle of the database.
Primarily this means we don't want to restart the database for every test unless the test requests it. So the database
is re-used as much as it can be via logic in the rule.

Regular usage:

```
@Rule(order = 1)
public DatabaseRule databaseRule = DatabaseRule.getInstance(YourBaseTestClass.class);
```

There are two extensions of this rule to be aware of:
1. `DatabaseContainerRule` - Present in the `insight-brain-service` module. Adds support for the `DatabaseContainer`
   that is part of the main application `InsightBrainService`.
2. `MultiTenantDatabaseContainerRule` - Multi-tenant version of `DatabaseContainerRule` in the `nexus-mtiq-serve`
   module.

Note the rule order. The database **MUST** be started before any test that uses a database. Also note that the class
type is passed in. The implementation of `getInstance` tracks this class and if the current test changes then the
database is marked as dirty and will be re-provisioned as necessary. This is to cover the case in our tests
in `insight-brain-service` where the base test class can alternate which can cause problems (e.g. IT runs with a full IQ
test server, then a regular db-only test runs and does something else, etc...).

#### Database Annotations

By default, the rule uses an in-memory H2 database. This is a fast database suitable for general tests. **IMPORTANT**
The H2 in-memory database is **NEVER** shutdown and is reused between each test.

You can customize the type and configuration of the database via annotations. There are three available:
* `@H2InMemoryTest`
* `@H2DiskTest`
* `@PostgresTest`

There are available properties for each of these annotations as they apply to that database. Examples include
suppressing migrations, forcing a clean database, and custom db parameters. See the javadoc on the annotations for what
is support on each annotation as currently not every database supports every option.

As mentioned the H2 in-memory test is the default. You do not need to annotate a test using the database rule
with `@H2InMemoryTest`, it will automatically use that. If you use any of the `H2InMemoryTest`
properties (`customSettings`, `cleanDatabase`, `suppressMigrations`) you will get a new temporary in-memory database
that is closed after the test is complete. This does not affect the default shared in-memory test database.

#### Which base test class to use?

- If you only need a database and nothing else: `AbstractDatabaseTest`. Available in the `insight-brain-db` module and
  below.
- If you need a database plus the `TemporaryEntity` helper then use: `AbstractDataTest`. Available in the
  `insight-brain-data` module and below.
- If you are testing a DAO use `AbstractDbDAOTest`. Only available in `insight-brain-data`.
- If you want the fully IQ integration test framework with a running IQ test server:
    - For regular single-tenant tests in `insight-brain-service` use `AbstractBrainServiceIntegrationTest`
    - For multi-tenant tests in `nexus-mtiq-server` use `AbstractMultiTenantBaseIntegrationTest`
- For java functional tests in `insight-brain-java-functional-test` use AbstractFunctionalTest

#### Postgres Specifics

Note the Postgres test fixture has some smarts to reduce database provisioning time.

1. A **single** Postgres container is started (using testcontainers.org) and used for **all** tests. In Postgres-speak
   this is a single cluster. Then within that cluster we create a new **database** for each test. This significantly
   saves on startup time as only one container is ever started.
2. When the Postgres test cluster is started, it automatically creates a database `template_database` and runs a full
   migration on it. This template database is then cloned for new tests instead of re-executing migrations.

To access the Postgres database while debugging a test:

The Postgres container name is `iq-test-db` with a random suffix. You can access the container during a running test and
use `psql`:

```
docker exec -it $(docker ps --quiet --filter name=iq-test-db) psql -U testuser -d testdata
```

This will give you a `psql` prompt. Use `\l` to list current databases and `\c databasename` to connect to one.

#### Cluster lock system
The cluster locking system is used to lock resources that are shared outside the database, for example report files on disk/efs. 
The database is used for the locks because it is already a shared resource in the cluster.

There are two implementations, one for H2 and one for postgres. Because H2 doesn't support clustering, the H2 implementation
does not persist locks in the database, but uses Java Semaphores for its locking mechanism. The postgres implementation uses a
table to store the locks as rows with a string ID and uses SELECT FOR UPDATE to lock the row. In practice the SELECT FOR UPDATE 
query doesn't change the database so the transaction is rolled back when the lock is released. 

There are currently no timeouts on the locks, so if a lock is not released, it will block the application indefinitely.

## History

- [CLM-26741](https://sonatype.atlassian.net/browse/CLM-26741) was an important epic completed in 2023 where the
  database layer in IQ was refactored. See ADR [0009-database-layer-refactor.md](../adr/0009-database-layer-refactor.md)
  for more. See [PR 10178](https://github.com/sonatype/insight-brain/pull/10178) for the implementation.
