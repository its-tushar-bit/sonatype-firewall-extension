<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
# Nexus IQ Server #

The on-premises server that customers run to evaluate policy against applications and review the results.

See https://docs.sonatype.com/display/INSIGHT/Insight+Brain for more information.

# Building #

Standard Maven build, i.e. `mvn clean install`.

Be sure to have both Maven and npm set up to use repository.sonatype.org.  See
https://docs.sonatype.com/display/CDI/Setting+up+npm+to+use+repository.sonatype.org for npm instructions

## Functional Tests ##

Add `-D skip-functional-test` to the `mvn` invocation to skip just the expensive functional tests but still run other
unit/integration tests.

Use `-D geb.env=firefox|chrome|phantom` to select the webdriver/browser for the Geb-based functional tests.

Use `-D browser=firefox|chrome` to select the webdriver/browser for the Java-based functional tests.

Use `-D slowmo.delay=<integer>` to enable "slow motion" for the functional tests where REST requests are delayed by the
specified number of milliseconds on the server. This mode can help to expose bad tests that make invalid assumptions
about timing of (asynchronous) operations. A delay of 500 ms doesn't delay tests too much that timeouts occur and is
typically sufficient to trigger errors where tests are badly coded and fail to wait on page changes. PhantomJS is known
to not support this slow motion mode properly so other browsers should be used.

# Connecting to a different HDS #

There is development option to connect to a different HDS.  Use the config `hdsUrl` and point to the desired location.

