System Requirements
===================

This install includes a bundled JDK. If you are required to use a different JDK, please refer to the documentation for Sonatype IQ Server for the supported versions (https://help.sonatype.com/en/java-compatibility-matrix.html).

Running the Sonatype IQ Server
==============================

This package contains example scripts to configure Sonatype IQ Server to run as a system service with systemd or init.d. The scripts are located in the examples directory of the installation. Additionally, the examples directory contains a sample configuration file that can be used to configure the server.

To start the Sonatype IQ Server run the demo script for your operating system:

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

Migrating from the standalone jar
=================================

The bundled distribution for IQ server contains the server and CLI jar files, along with demo.sh/bat files and a sample config file. To run the application, you would need to ensure Java is installed on your system and invoke it with the correct arguments. For example:
java -jar nexus-iq-server.jar server path/to/config.yml
With this new distribution, there is a wrapper script which allows you to invoke the application without needing to install the JDK. To run the server, use the following command:
./bin/nexus-iq-server server path/to/config.yml

Authentication in the Sonatype IQ Server
========================================

Sonatype IQ Server requires authentication to access the web interface.  The default username/password to access the system is admin/admin123
It is highly recommended that you change this password after logging in.

Documentation and Support
=========================

For more comprehensive documentation that includes full installation, configuration and usage instructions, please visit our documentation portal: http://links.sonatype.com/products/clm/doc.

If you are experiencing trouble with any part of Sonatype Lifecycle, you can always visit our support site at http://links.sonatype.com/products/clm/support. There you can view our knowledge base and contact our support team directly.
