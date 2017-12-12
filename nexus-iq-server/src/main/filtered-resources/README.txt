The Great Name Change of 2015
=============================

As of 1.17 CLM server is being renamed to Nexus IQ Server.

System Requirements
===================

Nexus IQ Server requires an Oracle Java 8 runtime update 45 as a minimum [2].

Internet Explorer 9+ or equivalent modern browsers from other vendors will be required for proper interaction with the web application.


Running the Nexus IQ Server
===========================

To start the Nexus IQ Server in a mode that is useful for a short evaluation run the demo script for your operating system: demo.bat or demo.sh.  This starts the server using the sample configuration file supplied in this bundle with the following defaults:
- listening on port 8070
- log to the console and ./log/clm-server.log
- data stored in ./sonatype-work/clm-server

Refer to the example config.yml in this bundle for additional options.

Authentication in the Nexus IQ Server
=====================================

Nexus IQ Server requires authentication to access the web interface.  The default username/password to access the system is admin/admin123
It is recommended that you change this password after logging in.

Documentation and Support
=========================

For more comprehensive documentation that includes full installation, configuration and usage instructions, please visit our documentation portal: http://links.sonatype.com/products/clm/doc.

If you are experiencing trouble with any part of Sonatype Nexus Lifecycle, you can always visit our support site at http://links.sonatype.com/products/clm/support. There you can view our knowledge base and contact our support team directly.

[1] Java SE 7 End of Public Updates Notice: http://www.oracle.com/technetwork/java/eol-135779.html
[2] All Java 8 update releases: http://www.oracle.com/technetwork/java/javase/8u-relnotes-2225394.html
