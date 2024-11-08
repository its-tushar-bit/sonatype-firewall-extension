<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
# Changelog

## Version 1.184.0 (November 08, 2024)
* Removed the mount path requirement for Sonatype Container Security
* Added support for files ending with the pattern .cdx.json

## Version 1.183.0 (October 10, 2024)
* Maintenance release

## Version 1.182.0 (September 04, 2024)
* Added support for customizing the container scan runtime via the `NEXUS_CONTAINER_SCANNING_RUNTIME` environmental variable
* Added support for customizing the container scan socket mapping via the `NEXUS_CONTAINER_SOCKET_MAPPING` environmental variable

## Version 1.181.0 (August 20, 2024)
* Updated internal dependencies to ensure compatibility with Lifecycle 181

## Version 1.180.0 (August 8, 2024)
* Updated internal dependencies to ensure compatibility with Lifecycle 180
* Added support for analyzing Java 21 and Java 22 bytecode
* Going forward Sonatype CLI requires Java 17 to run

## Version 1.162.0 (June 2023)
* Notable bug fix
  * Scan targets containing folder names with spaces are now handled correctly

## Version 1.158.0 (March 2023)
* Updates to Nexus Container Scanning with Nexus IQ CLI
  * Scanning remote images does not require providing environmental variables if the image is public

## Version 1.152.0 (January 2023)
* Introduced call flow analysis in Java (or any JVM language) binaries found in the scan targets to find method signatures that trigger a security vulnerability

## Version 1.150.0 (November 2022)
* Updates to Nexus Container Scanning with Nexus IQ CLI
  * Scanning local images does not require providing environmental variables
  * To scan remote images, the user will now have to provide only these variables: `NEXUS_CONTAINER_SCANNING_REGISTRY_USER` and `NEXUS_CONTAINER_SCANNING_REGISTRY_PASSWORD`
* Evaluations exit with a non-zero if there are any scanning errors

## Version 1.145.0 (October 2022)
* Notable bug fix
  * Releases 142 and above fix a bug where a manifest scan processed pom.xml files inside a META-INF directory. Files in this directory, in most cases (specifically for uber/shaded archives), do not represent the manifest file for the target application to be scanned. All pom.xml files inside a META-INF directory from release 142 and above are now ignored during a manifest scan

## Version 1.143.0 (September 2022)
* CycloneDX REST API Improvements
* Improved support for evaluating Java 18 applications and components
* Improvements to Nexus IQ CLI for auto-creating new applications

## Version 1.133.0 (March 2022)
* Dependency Information for CycloneDX SBOM scans

## Version 1.132.0 (January 2022)
* Bug fix for false positives in docker image scans

## Version 1.130.0 (December 2021)
* Update logback library version to remediate a low/moderate vulnerability (Nexus IQ Server does not use log4j)
* Cran and Cargo matching improvements
* Conda matching improvements

## Version 1.125.0 (October 2021)
* Conan Matching Improvements
  * Conan data and matching have been improved for both Lifecycle and Firewall
* Dependency Information Improvements for NPM
  * NPM Dependency Information detection has been improved to display more accurate results
* Added support for analyzing Java 17 bytecode

## Version 1.123.0 (September 2021)
* Fixed an issue with some NPM scans that were causing IQ Server 122 evaluations to fail when reading dependency information

## Version 1.122.0 (September 2021)
* Dependency Information for NPM
  * NPM project scans with manifests allow the displaying of dependency information for NPM components (Direct and Transitive)

## Version 1.120.0 (July 2021)
* Added support for container scanning via Nexus Container

## Version 1.119.0 (July 2021)
* SBOM Improvements and Bug Fixes:
  * CycloneDX SBOM scans have been improved to display better results in the report and some bugs have been fixed as well

## Version 1.118.0 (June 2021)
* Swift Application Analysis:
  * IQ Server can now be used to evaluate policies against components from the dependency file of a Swift application

## Version 1.117.0 (June 2021)
* Support for CycloneDX 1.3
  * CycloneDX Application Analysis has been extended to support the schema version CycloneDX 1.3 for XML format

## Version 1.116.0 (June 2021)
* Improvements to Python Application Analysis:
  * IQ Server now supports evaluating policies against Python components defined in poetry.lock files

## Version 1.114.0 (May 2021)
* Support for CycloneDX 1.2
  * CycloneDX Application Analysis has been extended to support the schema version CycloneDX 1.2 for XML format

## Version 1.107.0 (March 2021)
* Java Manifest Application Analysis
  * IQ Server now supports evaluating policies against Java components in pom.xml and build.gradle files

## Version 1.106.0 (March 2021)
* Improvements to manifest analysis:
  * Updated CLI scanner to exclude development dependencies when scanning package-lock.json files
  * Updated CLI scanner to parse package-lock.json files stored inside an archive
  * Fixed parsing errors when scanning yarn.lock and *.csproj files

## Version 1.105.0 (Feb 2021)
* Fixed initialization error in NuGet manifest scanning

## Version 1.104.0 (Jan 2021)
* Application analysis of components for:
  * NPM, as defined in yarn.lock, pnpm-lock.yaml, package-lock.json, and npm-shrinkwrap.json files
  * NuGet, as defined in .csproj and packages.config files

## Version 1.103.0 (Dec 2020)
* Added support for analyzing Java 14 and 15 bytecode

## Version 1.101.0 (Nov 2020)
* Nexus IQ CLI no longer supports Lifecycle XC. IQ Server now has native support for all languages that were supported in Lifecycle XC

## Version 1.98.0 (Sep 2020)
* Application analysis of components for:
  * Go components defined in a Gopkg.lock

## Version 1.97.0 (Aug 2020)
* Application analysis of components for:
  * C/C++ components defined in a conaninfo.txt file
  * Go components defined in a go.list file

## Version 1.94.0 (Jun 2020)
* Now released in sync with Nexus IQ Server releases (which may or may not include updates relevant to this docker image's release)
* Application analysis of components for:
  * C/C++ conanfile.py Files
  * Yum
  * Alpine
  * Debian
  * Drupal
  * R (CRAN)
  * Rust (Cargo)

## Version 1.88.0 (Mar 2020)
* Application analysis of components for:
  * Swift/Objective-C CocoaPods
  * Conda

## Version 1.87.0 (Mar 2020)
* Identify components based on SHA-1 value (content hash)
* Application analysis of components for:
  * C/C++ Conan
  * PHP Composer
  * RubyGems
  * CycloneDX application analysis extended to support submitting component vulnerabilities
