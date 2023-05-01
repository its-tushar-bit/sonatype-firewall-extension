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
    * [ Deployment ](#deployment)
    * [ Running Tests ](#running-tests)

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
1. Java 8 (if you use a version of Maven that has a newer JDK, you'll want to point JAVA_HOME to your Java 8 install)
2. Maven 3.6.x (note: it is possible newer versions may not work though 3.8.1 seems to work)
3. npm and yarn (for the frontend; see [that readme](insight-brain-frontend/README.md) for more specific detail)

Be sure to configure both Maven and npm to use repo.s.c (https://repo.sonatype.com) as the source for packages. You will need to use your own personal user credentials for repo.s.c (instructions for how to do this are included in the following Maven repo.s.c instructions):

* [**Maven** instructions](https://docs.sonatype.com/x/AYlCCg)
* [**npm** instructions](https://docs.sonatype.com/display/CDI/Setting+up+npm+to+use+repo.sonatype.com)

## Building ##

For a full build, including all tests (WARNING: this takes a long time!):

`mvn clean install`

If you just want to build the project in order to get up and running quickly, you can skip all tests as follows (run from the root dir):

`mvn clean install -DskipTests`

### Building for front-end development ###

The front-end build is included in the main Maven build, and it is also compiled into the backend server in a [typical deployment](#deployment). That being said, if you are doing front-end development, you will also want to be familiar with how to build and deploy the front-end assets separately. See [`insight-brain-frontend/README.md`](insight-brain-frontend/README.md) for details.

## Deployment ##

The server is deployed from the `insight-brain-service` directory - see the [`README`](insight-brain-service/README.md) there for details.

## Running Tests ##

**Prerequisite**: some tests use Docker to connect to an external service (e.g. a PostgreSQL database). To run all tests successfully, you will need to [install Docker Engine](https://docs.docker.com/install/). Alternatively, you can skip the Docker tests by setting the property `docker.optional` to `true` in your Maven `settings.xml`.

Add `-D skip-functional-test` to the `mvn` invocation to skip just the expensive functional tests but still run other
unit/integration tests.

Use `-D geb.env=firefox|chrome|phantom` to select the webdriver/browser for the Geb-based functional tests.

Use `-D browser=firefox|chrome` to select the webdriver/browser for the Java-based functional tests.

Use `-D slowmo.delay=<integer>` to enable "slow motion" for the functional tests where REST requests are delayed by the
specified number of milliseconds on the server. This mode can help to expose bad tests that make invalid assumptions
about timing of (asynchronous) operations. A delay of 500 ms doesn't delay tests too much that timeouts occur and is
typically sufficient to trigger errors where tests are badly coded and fail to wait on page changes. PhantomJS is known
to not support this slow motion mode properly so other browsers should be used.

**Flaky Tests**:
One cause for flaky tests is persisted entities left in the db after a test is run. These entities may impact other unrelated tests.
We have a mechanism to detect this problem.
If you suspect that a flaky test is caused by some other test that leaves instances of Foo entities in the db,
- add this method to the FooDAO class:
```
  @Override
  protected boolean detectTestEntityLeaks() {
    return true;
  }
```
- push the change to a branch and run a CI build for it

If there are Foo entities leaked by some tests, those tests will fail with a "Detected test entity leaks" error message,
and the test output will contain the stack trace(s) for where the leaked entities were created.
WARNING: This mechanism should be used only on branches, never on the main branch - it will cause memory leaks in the product.

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
