<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
# Functional tests
## Troubleshooting

### Can't run tests from intelliJ

#### 1) "package sun.security.tools.keytool does not exist"

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

#### 2) "Unenhanced classes were detected" problem

Sometimes IntelliJ will show an error when trying to run the functional test from the
IDE UI (right-click on method name > run). This might be caused because of some problem with the class enhancement
on the data module. A similar error to this one would be shown:

```
1190  InsightBrainODS  WARN   [main] openjpa.Enhance - Unenhanced classes were detected even though the enhancer has ran. Ensure that the EntityManagerFactory is created prior to creating any Entities.

<openjpa-3.2.0-r6f721f6 nonfatal user error> org.apache.openjpa.persistence.ArgumentException: This configuration disallows runtime optimization, but the following listed types were not enhanced at build time or at class load time with a javaagent: "
com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping
...
	at org.apache.openjpa.enhance.ManagedClassSubclasser.prepareUnenhancedClasses(ManagedClassSubclasser.java:117)
...

Process finished with exit code 255
```

#### Fix

To fix this, run the following command from the root folder of the project (insight-brain):

```
mvn process-classes
```
