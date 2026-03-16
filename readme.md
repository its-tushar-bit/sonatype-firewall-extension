<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
# Nexus IQ Server #

## Contents ##

* [ About Nexus IQ Server ](#about-nexus-iq-server)
* [ Related Projects ](#related-projects)
* [ Contributing ](#contributing)
* [ Working with `insight-brain` ](#working-with-insight-brain)
    * [ Requirements ](#requirements)
    * [ Building ](#building)
    * [ Code Formatting ](#code-formatting)
    * [ Deployment ](#deployment)
    * [ Running Tests ](#running-tests)
* [ API Guidelines ](./doc/devdocs/api-guide-lines.md)

## About Nexus IQ Server ##

**Nexus IQ Server** is the on-premises server that customers run to evaluate their applications against a set of policies and review the results. It is part of the [**Nexus Lifecycle**](https://www.sonatype.com/product-nexus-lifecycle) product umbrella (historically, this product umbrella was previously known as **Component Lifecycle Management (CLM)**).

`insight-brain` contains the server, front-end, and component scanner for Nexus IQ Server. It **scans** projects (i.e. it generates hashes that represent components in an application - see also: [`insight-scanner`](https://github.com/sonatype/insight-scanner)), and evaluates known component vulnerabilities and component license information against user-configured **policies**. It then uses these data to generate an **application scan report**.

## Related Projects ##

* In order to evaluate components against policies, IQ Server must access vulnerability and license data for each component. It requests identity, vulnerability, and license information for these components (represented as hashes) from [`hosted-data-services`](https://github.com/sonatype/hosted-data-services), also known as "HDS".
* Some data objects are shared by both `insight-brain` and `hosted-data-services`. These data objects are defined in [`insight-dto-model`](https://github.com/sonatype/insight-dto-model).
* [`insight-scanner`](https://github.com/sonatype/insight-scanner) is the source of the scanner code. `insight-brain` is one consumer of the scanner library.
* [`nexus-vulnerability-scanner`](https://github.com/sonatype/nexus-vulnerability-scanner) is essentially a "free sample" of IQ Server functionality; we offer it [on our marketing website](https://www.sonatype.com/appscan) as a way to demonstrate the information contained on an application scan report.
* The front-end of IQ Server consumes [`react-shared-components`](https://github.com/sonatype/sonatype-react-shared-components), a component library built in React that is shared across various Sonatype projects.

# Contributing #

[![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-v1.4%20adopted-ff69b4.svg)](code-of-conduct.md)

Team Insight is the primary maintainer for this application but welcomes contributions from any and all Sonatypers!

Please read our [contribution rules and guidelines](contributing.md).

# Working with insight-brain #

## Requirements ##

This project requires the following local installs:
1. Java 17
2. Maven 3.9.x
3. npm and yarn (for the frontend; see [that readme](insight-brain-frontend/README.md) for more specific detail)

Be sure to configure both Maven and npm to use repo.s.c (https://sonatype.repo.sonatype.app) as the source for packages. You will need to use your own personal user credentials for repo.s.c (instructions for how to do this are included in the following Maven repo.s.c instructions):

* [**Maven** instructions](https://sonatype.atlassian.net/wiki/spaces/CDI/pages/1921712289/Setting+up+maven+to+use+sonatype.repo.sonatype.app)
* [**npm** instructions](https://sonatype.atlassian.net/wiki/spaces/CDI/pages/1947467777/Setting+up+npm+to+use+sonatype.repo.sonatype.app)

## Building ##

For a full build, including all tests (WARNING: this takes a long time!):

`mvn clean install`

If you just want to build the project in order to get up and running quickly, you can skip all tests as follows (run from the root dir):

`mvn clean install -Pquick`

### Increasing build speed ###
To improve build times you can make use of the [Maven Daemon](https://github.com/apache/maven-mvnd).

1. Install version v3.9.0+ of `mvn`
2. Install `mvnd` https://github.com/apache/maven-mvnd#how-to-install-mvnd
3. Use `mvnd` instead of `mvn`

### Building for front-end development ###

The front-end build is included in the main Maven build, and it is also compiled into the backend server in a [typical deployment](#deployment). That being said, if you are doing front-end development, you will also want to be familiar with how to build and deploy the front-end assets separately. See [`insight-brain-frontend/README.md`](insight-brain-frontend/README.md) for details.

## Deployment ##

The server is deployed from the `insight-brain-service` directory - see the [`README`](insight-brain-service/README.md) there for details.

## Code Formatting ##

Code formatting is enforced by [Spotless](https://github.com/diffplug/spotless) with an Eclipse formatter config
located at `sonatype-config/sonatype-eclipse.xml`.

**Local development**: Spotless automatically formats changed files during the build (the `sonatype` profile runs
`spotless:apply` by default). Only files changed relative to `origin/main` are formatted (`ratchetFrom`).

**CI builds**: The `ci` profile skips auto-formatting; CI runs `spotless:check` separately to fail the build on
violations.

### Check formatting

    mvn spotless:check

### Fix formatting

If you encounter a formatting error, the easiest way to address it is to invoke the `apply` goal, optionally
scoped to the failing module:

    mvn spotless:apply
    mvn spotless:apply -pl :insight-brain-data

### Formatting a specific file

To format a single file without running the full build:

    mvn spotless:apply -DspotlessFiles='.*MyClass\.java'

### Editing the formatter config

The Eclipse formatter settings live in `sonatype-config/sonatype-eclipse.xml`. If you need to adjust a rule,
edit that file and commit it alongside the reformatted code.

## Running Tests ##

**Running fast tests only**: We categorize tests as "slow" when they average over 100ms per test case. To run only
the fast tests locally you can pass `-DexcludedGroups=SlowTest`

**Prerequisite**: some tests use Docker to connect to an external service (e.g. a PostgreSQL database). To run all tests successfully, you will need to [install Docker Engine](https://docs.docker.com/install/). Alternatively, you can skip the Docker tests by setting the property `docker.optional` to `true` in your Maven `settings.xml`.

Add `-D skip-functional-test` to the `mvn` invocation to skip just the expensive functional tests but still run other
unit/integration tests.

Use `-D browser=firefox|chrome` to select the webdriver/browser for the Java-based functional tests.

Use `-D slowmo.delay=<integer>` to enable "slow motion" for the functional tests where REST requests are delayed by the
specified number of milliseconds on the server. This mode can help to expose bad tests that make invalid assumptions
about timing of (asynchronous) operations. A delay of 500 ms doesn't delay tests too much that timeouts occur and is
typically sufficient to trigger errors where tests are badly coded and fail to wait on page changes. PhantomJS is known
to not support this slow motion mode properly so other browsers should be used.

**Writing Tests**:
The tests that use the IQ database must cleanup after themselves.
This is achieved by having an instance of TemporaryEntity as junit rule in the test class:
```
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();
```
If your test class extends from one of the IQ abstract/base test classes, most probably there is a TemporaryEntity rule already in place.
If you add a new persisted entity class, then you must add cleanup code in the TemporaryEntity.after() method.
The TemporaryEntity rule detects any entities leaked by tests. The detection is enabled for all tests in our CI builds.
If you want to enable the detection locally, add `-DdetectTestEntityLeaks` to the test run command.

Note: It is not required anymore to instantiate all entities via one of the newSomeEntity helper methods in TemporaryEntity.
Those helper methods can still be used, but it is not required anymore.

## Adding Configuration

We can generally categorize IQ Server configurations as either simple configurations or complex configurations.

A simple configuration is a name-value pair where the name is a String and the value can be converted into a primitive.

A complex configuration is one where the value is an object with fields which themselves may be objects.

For a simple configuration, the recommended way to add this is to add to the
[`ConfigurationProperty.PROPERTIES`](https://github.com/sonatype/insight-brain/blob/main/insight-brain-service/src/main/java/com/sonatype/insight/brain/api/v2/service/ConfigurationProperty.java#L19)
array and add tests to check the conversions from primitive value to String and from String to primitive value. Once done, in code
the simple configuration will be accessible using the
[`ApiConfigurationService`](https://github.com/sonatype/insight-brain/blob/main/insight-brain-service/src/main/java/com/sonatype/insight/brain/api/v2/service/ApiConfigurationService.java)
or [`SystemConfigurationPropertyDAO`](https://github.com/sonatype/insight-brain/blob/main/insight-brain-data/src/main/java/com/sonatype/insight/brain/dataaccess/configuration/SystemConfigurationPropertyDAO.java),
or via REST API using the
[`DefaultApiConfigurationResource`](https://github.com/sonatype/insight-brain/blob/main/insight-brain-service/src/main/java/com/sonatype/insight/brain/api/v2/DefaultApiConfigurationResource.java).

For a complex configuration, it is expected to have its own table/DAO/service/resource.

In either case, we should ensure that any configuration changes take effect in all relevant places on each node in a
cluster. For simple configurations this means implementing
[`ConfigurationListener`](https://github.com/sonatype/insight-brain/blob/main/insight-brain-service/src/main/java/com/sonatype/insight/brain/api/v2/service/ConfigurationListener.java)
in the relevant places and accounting for changes to the properties you're interested in. For complex configurations you
would typically implement your own listener in the right places and ensure they get triggered in your service.

## Experimental Feature Flags ##

As new work is being developed, it can be hidden behind feature flags. New feature flags should be stored in the 
database.

An easy way to do this is to add it to the `SystemConfigurationPropertyFeature` enum. When adding you can set 
`enabledWhenAbsent` to `false` if you want the feature to only be enabled if it's inside the database, or `true` 
if you want the feature to only be enabled if it's not inside the database. Typically, an experimental feature would 
start with `enabledWhenAbsent` set to `false`. When it's production-ready, it would either be removed from the enum, or 
have its `enabledWhenAbsent` changed to `true` (to still be able to disable it). This would be alongside an incremental 
script to delete it from the `system_configuration_property` table.

## Working on Customer Support (zendesk) Tickets ##

Some Customer Support tickets have support.zip files that include db dumps.
To import the db dump, see insight-brain-service/src/test/java/com/sonatype/insight/brain/support/DbImportFromSupportZip.java

## IQ CLI ##

In December 2024, the IQ CLI was moved to its own repository: [sonatype/iq-cli](https://github.com/sonatype/iq-cli). 
