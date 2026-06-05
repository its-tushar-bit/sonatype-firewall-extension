System Requirements
===================

This install includes a bundled JDK. If you are required to use a different JDK, please refer to the documentation for Sonatype IQ Server for the supported versions (https://help.sonatype.com/en/java-compatibility-matrix.html).

Running the Sonatype IQ Server
==============================

This package contains example scripts to configure Sonatype IQ Server to run as a system service with systemd or init.d. The scripts are located in the examples directory of the installation. Additionally, the examples directory contains a sample configuration file that can be used to configure the server.

To start the Sonatype IQ Server, run the wrapper script for your operating system:

# Linux
./bin/nexus-iq-server server path/to/config.yml
# Windows
.\bin\nexus-iq-server.bat server path\to\config.yml

Refer to the example config.yml in this bundle for additional options.

Content
=======

The following files and directories are in the installation directory:

README.txt         # This file
bin/               # Contains Java binaries and the nexus-iq-server script
conf/              # JDK conf
eula.html          # Sonatype eula
examples/          # Example systemd service file, script and config file
jars/              # IQ Server application jar
legal/             # JDK legal
lib/               # JDK lib
release            # JDK release

Run as a Service
================

The Sonatype IQ Server can be Run as a Service, please see our help documentation for details on how to use the service file in the examples directory.

# Linux
- http://links.sonatype.com/products/nxiq/doc/run-as-a-service-linux
# Windows
- http://links.sonatype.com/products/nxiq/doc/run-as-a-service-windows

Note on the Standalone JAR Distribution
========================================

The standalone JAR distribution has been discontinued. This bundled distribution
is the only supported format. See https://help.sonatype.com/en/sonatype-iq-server-feature-status.html
for details.

Authentication in the Sonatype IQ Server
========================================

Sonatype IQ Server requires authentication to access the web interface.  The default username/password to access the system is admin/admin123
It is highly recommended that you change this password after logging in.

Documentation and Support
=========================

For more comprehensive documentation that includes full installation, configuration and usage instructions, please visit our documentation portal: http://links.sonatype.com/products/clm/doc.

If you are experiencing trouble with any part of Sonatype Lifecycle, you can always visit our support site at http://links.sonatype.com/products/clm/support. There you can view our knowledge base and contact our support team directly.
