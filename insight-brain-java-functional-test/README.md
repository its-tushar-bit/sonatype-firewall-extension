<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
# Functional tests

This test project runs Functional tests with Selenium on IQ. (On-Premise).

If you are looking for MTIQ functional tests those are located on `nexus-mtiq-functional-test` module.

`insight-brain-functional-test-common` module contains common resources used in both IQ and MTIQ functional tests.

## Running

The functional tests run against a Chrome browser instance, which can be managed two different ways:
* Chrome can be run within a Docker container via testcontainers. This is the default mode and matches the way the
  tests are run in the CI/CD pipeline.
* The path to a locally installed chromedriver binary can be specified, in which case that chromedriver will be used
  to drive a locally installed Chrome instance. This mode can be useful while debugging, as the Chrome browser will
  visibly appear on the screen, allowing one to watch the test interact with the UI. To use this mode, set the
  `webdriver.chrome.driver` system property to the path of the chromedriver binary. Example:
  `-Dwebdriver.chrome.driver=/user/bin/chromedriver`. You must manage the installation of the chromedriver binary
  yourself.

## Troubleshooting

### Can't run tests from intelliJ

#### 1) Local Chrome instance is not used even though `webdriver.chrome.driver` system property is set.

This can be caused by the fact that `docker-functional-tests` profile is active.

#### Fix

In intelliJ, go to `Maven` tab, and in the `Profiles` section, uncheck the `docker-functional-tests` profile.
After unchecking the profile, you need to reload the maven project.

#### 2) "package sun.security.tools.keytool does not exist"

This can be caused by a couple of misconfigurations in IntelliJ. The error
shown is similar to one below:

```
/insight-brain/insight-brain-data/src/main/java/com/sonatype/insight/brain/dataaccess/configuration/saml/SamlConfigurationInternalDAO.java:18:34
java: package sun.security.tools.keytool does not exist
```

#### Fix
Double check you have installed the correct java jdk version supported for the development of the project. (At the moment of writing this: openjdk 1.8).

In addition, make sure that **your project SDK version matches the
selected version in the Java compiler section** under the IntelliJ global
settings. More information can be found in the following link:

[https://stackoverflow.com/questions/40448203/intellij-says-the-package-does-not-exist-but-i-can-access-the-package](https://stackoverflow.com/questions/40448203/intellij-says-the-package-does-not-exist-but-i-can-access-the-package)


Run the following command in the root folder of the project (insight-brain folder).

```
mvn clean install -DskipTests -Dskip-functional-test -X -Dcheckstyle.skip=true
```

#### 3) "Unenhanced classes were detected" problem

Sometimes IntelliJ will show an error when trying to run the functional test from the
IDE UI (right-click on method name > run). This might be caused because of some problem with the class enhancement
on the data module. A similar error to this one would be shown:

```
1190  InsightBrainODS  WARN   [main] openjpa.Enhance - Unenhanced classes were detected even though the enhancer has ran. Ensure that the EntityManagerFactory is created prior to creating any Entities.

<openjpa-3.2.0-r6f721f6 nonfatal user error> org.apache.openjpa.persistence.ArgumentException: This configuration disallows runtime optimization, but the following listed types were not enhanced at build time or at class load time with a javaagent: "
com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping
com.sonatype.insight.brain.model.configuration.ldap.LdapConnection
com.sonatype.insight.brain.model.configuration.ldap.LdapServer".

	at org.apache.openjpa.enhance.ManagedClassSubclasser.prepareUnenhancedClasses(ManagedClassSubclasser.java:117)
	at org.apache.openjpa.kernel.AbstractBrokerFactory.loadPersistentTypes(AbstractBrokerFactory.java:314)
	at org.apache.openjpa.kernel.AbstractBrokerFactory.initializeBroker(AbstractBrokerFactory.java:240)
	at org.apache.openjpa.kernel.AbstractBrokerFactory.newBroker(AbstractBrokerFactory.java:216)
	at org.apache.openjpa.kernel.DelegatingBrokerFactory.newBroker(DelegatingBrokerFactory.java:166)
	at org.apache.openjpa.persistence.EntityManagerFactoryImpl.doCreateEM(EntityManagerFactoryImpl.java:282)
	at org.apache.openjpa.persistence.EntityManagerFactoryImpl.createEntityManager(EntityManagerFactoryImpl.java:201)
	at org.apache.openjpa.persistence.EntityManagerFactoryImpl.createEntityManager(EntityManagerFactoryImpl.java:188)
	at org.apache.openjpa.persistence.EntityManagerFactoryImpl.createEntityManager(EntityManagerFactoryImpl.java:178)
	at org.apache.openjpa.persistence.EntityManagerFactoryImpl.createEntityManager(EntityManagerFactoryImpl.java:64)
	at com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO.createTransactionContext(AbstractOperationalSqlDAO.java:27)
	at com.sonatype.insight.dataaccess.AbstractDAO.getList(AbstractDAO.java:130)
	at com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO.getAll(MigrationTrackerDAO.java:30)
	at com.sonatype.insight.brain.dataaccess.TemporaryEntity.before(TemporaryEntity.java:512)
	at org.junit.rules.ExternalResource$1.evaluate(ExternalResource.java:50)
	at org.junit.rules.ExternalResource$1.evaluate(ExternalResource.java:54)
	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
	at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100)
	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:366)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:103)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:63)
	at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
	at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:293)
	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
	at org.junit.runners.ParentRunner.run(ParentRunner.java:413)
	at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
	at com.intellij.junit4.JUnit4IdeaTestRunner.startRunnerWithArgs(JUnit4IdeaTestRunner.java:69)
	at com.intellij.rt.junit.IdeaTestRunner$Repeater$1.execute(IdeaTestRunner.java:38)
	at com.intellij.rt.execution.junit.TestsRepeater.repeat(TestsRepeater.java:11)
	at com.intellij.rt.junit.IdeaTestRunner$Repeater.startRunnerWithArgs(IdeaTestRunner.java:35)
	at com.intellij.rt.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:235)
	at com.intellij.rt.junit.JUnitStarter.main(JUnitStarter.java:54)


Process finished with exit code 255
```

#### Fix

To fix this, run the following command from the insight-brain-data folder of the project (insight-brain/insight-brain-data):

```
mvn process-classes
```

### Docker API version incompatibility with testcontainers

#### 4) "client version 1.32 is too old. Minimum supported API version is 1.44"

When running functional tests with Docker/testcontainers, you may encounter an error similar to:

```
Caused by: java.lang.ExceptionInInitializerError: Exception java.lang.IllegalStateException: Could not find a valid Docker environment. Please see logs and check configuration [in thread "main"]
```

And when `-X` debug flag is added to the maven command, a more detailed error like this:

```
Caused by: com.github.dockerjava.api.exception.BadRequestException: Status 400:
{"message":"client version 1.32 is too old. Minimum supported API version is 1.44, please upgrade your client to a newer version"}
```

This occurs because:
- The project uses testcontainers 1.21.3, which depends on docker-java-api 3.4.2
- docker-java-api 3.4.2 uses Docker API version 1.32 by default
- Modern Docker Desktop versions (29.0+) require minimum API version 1.44

#### Fix

Add the `-Dapi.version=1.44` system property when running functional tests with the `docker-functional-tests` profile:

```bash
mvn test-compile failsafe:integration-test failsafe:verify \
  -Dit.test=YourTestClass#yourTestMethod \
  -Pdocker-functional-tests \
  -Dapi.version=1.44 \
  -pl insight-brain-java-functional-test
```

This overrides the default Docker API version used by docker-java-api to match your Docker Desktop version.

**Note**: This is only needed when running tests locally with newer Docker Desktop versions. CI/CD environments typically use compatible Docker versions and don't require this flag.

