Running the CLM Server
======================

NOTE: Java Runtime 6+ is required

Run with default configuration from the command line with:
    java -jar sonatype-clm-server-*.jar

This will start the Sonatype CLM Server in a mode that is useful for a short evaluation.  The following default settings are used:
- listening on port 8070
- log to the console
- data stored in ./sonatype-work/clm-server

Optionally provide a configuration file and run as:
    java -jar sonatype-clm-server-*.jar server config.yml

Refer to the example config.yml in this bundle for options.


Configuration for PDF report generation
=======================================

The CLM Server is used to generate PDF reports of CI CLM results.  This is done on demand and if you don't use this feature no configuration changes are necessary.  However if you plan on generating PDF reports you will likely need to increase the permgen memory available to the CLM Server.  In our testing a Sun/Oracle 1.7 64bit JVM appears to default to 85 MB of permgen and occasionally we hit that limit.

This can be set when invoking java, with a Sun/Oracle JVM:
    java -XX:MaxPermSize=128m -jar sonatype-clm-server-*.jar server config.yml


Support and further information
===============================

More information, documentation, and support can be found on our support site: http://links.sonatype.com/products/clm/ci/support