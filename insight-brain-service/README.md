<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
# Insight Brain Service

`insight-brain-service` is the module containing the back-end of [Nexus IQ Server](https://github.com/sonatype/insight-brain).

## Contents

* [ Deploying IQ Server Locally ](#deploying-iq-server-locally)
    * [ Configuration ](#configuration)
    * [ Ports ](#ports)
    * [ Using the application ](#using-the-application)
* [ Mail Assets ](#mail-assets)

## Deploying IQ Server Locally

Before deploying, ensure your project has been [built](https://github.com/sonatype/insight-brain#building) successfully.

From the `insight-brain-service` directory, you can start the server as follows:

`mvn exec:java -Dexec.mainClass=com.sonatype.insight.brain.spring.InsightBrainSpringApplication -Dexec.args='server src/test/resources/config-dev.yml'`

Alternatively, you can also launch the server using the compiled jar (NOTE: replace `*` with the appropriate version):

`java -jar target/insight-brain-service-*-SNAPSHOT-server.jar server src/test/resources/config-dev.yml`

### Configuration

Note that the above deploy command includes a reference to a configuration file: [`insight-brain-service/src/test/resources/config-dev.yml`](./src/test/resources/config-dev.yml).

This file is checked into the project and does not need to be modified for use in a typical development environment. Sometimes, depending on the work you’re doing, you may need to change this config. For example, the default config has the `hdsUrl` config set to the common HDS staging environment, but you might want to use a local deploy of HDS instead.

When you need a custom config, we suggest creating a local copy of this file, allowing you to easily maintain a separate config (or set of configs) that suits your own needs. When deploying, simply update the path in the above deploy command to point to your new locally-maintained config file.

### Ports

The server runs on port `8070` by default. You can override the default port by specifying the system property `dw.server.applicationConnectors[0].port` using Maven's `-D` command line parameter.

For example, to deploy to port 8072:

`mvn exec:java -Dexec.mainClass=com.sonatype.insight.brain.spring.InsightBrainSpringApplication -Dexec.args='server src/test/resources/config-dev.yml' -Ddw.server.applicationConnectors[0].port=8072`

### Using the application

You can log on to the server with these default credentials:
* username: **admin**
* password: **admin123**

The **first time** you launch the application, you will need to add a license file. Download and use `[year]-sonatype-internal-lcc-lfc-1000apps-1000rm_users-1000lc_users-1000fw_users.lic` from [the product licensing page](https://sonatype.atlassian.net/wiki/spaces/ProdMgmt/pages/43516041/Product+Licensing).

## Mail Assets

The mail assets employed by the policy alert mails (cf. `policythreats.ftl`) are maintained in https://github.com/sonatype/cdn.sonatype.com/tree/master/dist/clm/policy/1.3
